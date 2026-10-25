package br.com.fatecararas.util.messaging;

import br.com.fatecararas.api.event.Event;
import br.com.fatecararas.api.exceptions.MessagingUnavailableException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(classes=RabbitIntegrationTest.Application.class, properties={
    "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true"
})
class RabbitIntegrationTest {
    @SpringBootConfiguration @EnableAutoConfiguration @Import(RabbitConfiguration.class)
    static class Application {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Container static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1.0-management");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }
    @Autowired EventPublisher publisher;
    @Autowired RabbitTemplate template;
    @Autowired ConnectionFactory connectionFactory;
    @Autowired SimpleRabbitListenerContainerFactory factory;
    RabbitAdmin admin;
    SimpleMessageListenerContainer listener;
    @BeforeEach void reset() {
        admin = new RabbitAdmin(connectionFactory);
        admin.initialize();
        for (String service : new String[]{"product","review","recommendation"}) {
            admin.purgeQueue(service+".commands"); admin.purgeQueue(service+".dlq");
        }
    }
    @AfterEach void stop() { if (listener != null) listener.stop(); }
    void start(MessageListener callback) {
        listener = factory.createListenerContainer();
        listener.setQueueNames("product.commands");
        listener.setMessageListener(callback);
        listener.afterPropertiesSet(); listener.start();
    }
    int count(String queue) { return admin.getQueueInfo(queue).getMessageCount(); }
    @Test void storesCommandsWithoutConsumerAndRoutesByService() {
        publisher.publish("review", Event.delete(UUID.randomUUID(), 1));
        assertEquals(1, count("review.commands"));
        assertEquals(0, count("product.commands"));
        assertEquals(0, count("recommendation.commands"));
    }
    @Test void rejectsUnroutablePublication() {
        assertThrows(MessagingUnavailableException.class,
            () -> publisher.publish("unknown", Event.delete(UUID.randomUUID(),1)));
    }
    @Test void retriesTransientFailureThenAcknowledges() {
        var attempts = new AtomicInteger();
        start(message -> { if (attempts.incrementAndGet() < 3) throw new IllegalStateException("database offline"); });
        publisher.publish("product", Event.delete(UUID.randomUUID(),1));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(3, attempts.get()));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertEquals(0, admin.getQueueInfo("product.commands").getMessageCount()));
        assertEquals(0, count("product.dlq"));
    }
    @Test void exhaustedRetriesGoToDeadLetterQueue() {
        var attempts = new AtomicInteger();
        start(message -> { attempts.incrementAndGet(); throw new IllegalStateException("database offline"); });
        publisher.publish("product", Event.delete(UUID.randomUUID(),1));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(1, count("product.dlq")));
        assertEquals(3, attempts.get());
    }
    @Test void permanentFailureGoesToDeadLetterQueueWithoutRetry() {
        var attempts = new AtomicInteger();
        start(message -> { attempts.incrementAndGet(); throw new AmqpRejectAndDontRequeueException("invalid command"); });
        publisher.publish("product", Event.delete(UUID.randomUUID(),1));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertEquals(1, count("product.dlq")));
        assertEquals(1, attempts.get());
    }
}
