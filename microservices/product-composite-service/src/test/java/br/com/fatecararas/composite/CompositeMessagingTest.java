package br.com.fatecararas.composite;

import br.com.fatecararas.api.composite.product.*;
import br.com.fatecararas.api.event.Event;
import br.com.fatecararas.api.exceptions.*;
import br.com.fatecararas.composite.client.*;
import br.com.fatecararas.composite.config.ServiceIds;
import br.com.fatecararas.composite.service.ProductCompositeIntegrationService;
import br.com.fatecararas.util.messaging.EventPublisher;
import br.com.fatecararas.util.http.ServiceUtil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompositeMessagingTest {
    final EventPublisher publisher = mock(EventPublisher.class);
    final ProductClient product = mock(ProductClient.class);
    final RecommendationClient recommendation = mock(RecommendationClient.class);
    final ReviewClient review = mock(ReviewClient.class);
    final ProductCompositeIntegrationService integration = new ProductCompositeIntegrationService(
        publisher, product, recommendation, review, mock(ServiceUtil.class), mock(DiscoveryClient.class), new ServiceIds("product","recommendation","review"), Runnable::run);
    ProductAggregate aggregate(List<RecommendationSummary> recommendations) {
        return new ProductAggregate(1,"Produto",10,recommendations,
            List.of(new ReviewSummary(1,"Ana","Teste","OK")),null);
    }
    @Test void publishesCorrelatedCommandsWithoutHttpWrites() {
        integration.create(aggregate(List.of(new RecommendationSummary(1,"Ana",5,"OK"))));
        var commands = ArgumentCaptor.forClass(Event.class);
        verify(publisher, times(3)).publish(anyString(), commands.capture());
        assertEquals(1, commands.getAllValues().stream().map(Event::correlationId).distinct().count());
        assertEquals(3, commands.getAllValues().stream().map(Event::eventId).distinct().count());
        verifyNoInteractions(product,recommendation,review);
    }
    @Test void validatesEntireAggregateBeforePublishing() {
        assertThrows(InvalidInputException.class,
            () -> integration.create(aggregate(List.of(new RecommendationSummary(1,"Ana",6,"OK")))));
        verifyNoInteractions(publisher);
    }
    @Test void rejectsDuplicateChildrenBeforePublishing() {
        var item = new RecommendationSummary(1,"Ana",5,"OK");
        assertThrows(InvalidInputException.class, () -> integration.create(aggregate(List.of(item,item))));
        verifyNoInteractions(publisher);
    }
    @Test void stopsOnPublicationFailure() {
        doThrow(new MessagingUnavailableException("offline",null)).when(publisher).publish(eq("product"),any());
        assertThrows(MessagingUnavailableException.class, () -> integration.create(aggregate(List.of())));
        verify(publisher, times(1)).publish(anyString(),any());
    }
    @Test void deletePublishesToAllServices() {
        integration.delete(1);
        verify(publisher).publish(eq("product"),argThat(event -> event.eventType()==Event.Type.DELETE && event.data()==null));
        verify(publisher).publish(eq("review"),any());
        verify(publisher).publish(eq("recommendation"),any());
        verifyNoInteractions(product,recommendation,review);
    }
    @Test void httpAcceptsOnlyConfirmedPublicationAndReports503() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new br.com.fatecararas.composite.web.ProductCompositeController(integration))
            .setControllerAdvice(new br.com.fatecararas.util.http.GlobalControllerExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/product-composite")
            .contentType("application/json").content("{\"productId\":1,\"name\":\"Produto\",\"weight\":10}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted());
        doThrow(new MessagingUnavailableException("broker offline",null)).when(publisher).publish(anyString(),any());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/product-composite/1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isServiceUnavailable())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(503));
    }
}
