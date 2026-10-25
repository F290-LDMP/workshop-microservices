package br.com.fatecararas.composite.service;

import br.com.fatecararas.api.composite.product.ProductAggregate;
import br.com.fatecararas.api.composite.product.RecommendationSummary;
import br.com.fatecararas.api.composite.product.ReviewSummary;
import br.com.fatecararas.api.composite.product.ServiceAddresses;
import br.com.fatecararas.api.core.product.Product;
import br.com.fatecararas.api.core.recommendation.Recommendation;
import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.api.core.DomainValidation;
import br.com.fatecararas.api.event.Event;
import br.com.fatecararas.util.messaging.EventPublisher;
import br.com.fatecararas.composite.client.ProductClient;
import br.com.fatecararas.composite.client.RecommendationClient;
import br.com.fatecararas.composite.client.ReviewClient;
import br.com.fatecararas.composite.config.ServiceIds;
import br.com.fatecararas.util.http.ServiceUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

@Service
public class ProductCompositeIntegrationService {
    private final EventPublisher publisher;
    private final ProductClient productClient;
    private final RecommendationClient recommendationClient;
    private final ReviewClient reviewClient;
    private final ServiceUtil serviceUtil;
    private final Executor executor;
    private final DiscoveryClient discoveryClient;
    private final ServiceIds serviceIds;

    public ProductCompositeIntegrationService(
            EventPublisher publisher,
            ProductClient productClient,
            RecommendationClient recommendationClient,
            ReviewClient reviewClient,
            ServiceUtil serviceUtil,
            DiscoveryClient discoveryClient,
            ServiceIds serviceIds,
            @Qualifier("applicationTaskExecutor") Executor executor) {
        this.publisher = publisher;
        this.productClient = productClient;
        this.recommendationClient = recommendationClient;
        this.reviewClient = reviewClient;
        this.serviceUtil = serviceUtil;
        this.discoveryClient = discoveryClient;
        this.serviceIds = serviceIds;
        this.executor = executor;
    }

    public void create(ProductAggregate aggregate) {
        validateId(aggregate.getProductId());
        var recommendations = aggregate.getRecommendations() == null
                ? java.util.List.<RecommendationSummary>of() : aggregate.getRecommendations();
        var reviews = aggregate.getReviews() == null
                ? java.util.List.<ReviewSummary>of() : aggregate.getReviews();
        var recommendationIds = new java.util.HashSet<Integer>();
        var reviewIds = new java.util.HashSet<Integer>();
        for (var item : recommendations) {
            if (item == null) throw new InvalidInputException("Null recommendation");
            validateId(item.getRecommendationId());
            DomainValidation.rate(item.getRate());
            if (!recommendationIds.add(item.getRecommendationId())) throw new InvalidInputException("Duplicate recommendationId");
        }
        for (var item : reviews) {
            if (item == null) throw new InvalidInputException("Null review");
            validateId(item.getReviewId());
            if (!reviewIds.add(item.getReviewId())) throw new InvalidInputException("Duplicate reviewId");
        }
        var correlationId = java.util.UUID.randomUUID();
        publisher.publish("product", Event.create(correlationId, aggregate.getProductId(),
                new Product(aggregate.getProductId(), aggregate.getName(), aggregate.getWeight(), null)));
        for (var item : recommendations) publisher.publish("recommendation",
                Event.create(correlationId, aggregate.getProductId(),
                new Recommendation(aggregate.getProductId(), item.getRecommendationId(), item.getAuthor(), item.getRate(), item.getContent(), null)));
        for (var item : reviews) publisher.publish("review",
                Event.create(correlationId, aggregate.getProductId(),
                new Review(aggregate.getProductId(), item.getReviewId(), item.getAuthor(), item.getSubject(), item.getContent(), null)));
    }

    public ProductAggregate get(int productId) {
        validateId(productId);

        CompletableFuture<Product> product = CompletableFuture.supplyAsync(
                () -> productClient.getProduct(productId), executor);

        CompletableFuture<List<Recommendation>> recommendations = CompletableFuture.supplyAsync(
                () -> recommendationClient.getRecommendations(productId), executor);

        CompletableFuture<List<Review>> reviews = CompletableFuture.supplyAsync(
                () -> reviewClient.getReviews(productId), executor);

        try {
            CompletableFuture.allOf(product, recommendations, reviews).join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw ex;
        }

        Product p = product.join();

        return new ProductAggregate(p.getProductId(), p.getName(), p.getWeight(),
                recommendations.join().stream().map(r -> new RecommendationSummary(
                        r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent())).toList(),
                reviews.join().stream().map(r -> new ReviewSummary(
                        r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent())).toList(),
                new ServiceAddresses(serviceUtil.getServerAddress(), p.getServiceAddress(),
                        reviews.join().stream().map(Review::getServiceAddress).filter(Objects::nonNull).findFirst()
                                .orElseGet(() -> registeredAddress(serviceIds.review())),
                        recommendations.join().stream().map(Recommendation::getServiceAddress).filter(Objects::nonNull)
                                .findFirst().orElseGet(() -> registeredAddress(serviceIds.recommendation()))));
    }

    public void delete(int productId) {
        validateId(productId);
        var correlationId = java.util.UUID.randomUUID();
        for (String destination : new String[]{"product", "recommendation", "review"})
            publisher.publish(destination, Event.delete(correlationId, productId));
    }

    private void validateId(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
    }

    private String registeredAddress(String serviceId) {
        return discoveryClient.getInstances(serviceId).stream().findFirst()
                .map(instance -> instance.getUri().toString()).orElse(null);
    }
}
