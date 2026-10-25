package br.com.fatecararas.util.messaging;

import br.com.fatecararas.api.core.product.Product;
import br.com.fatecararas.api.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventHandlerTest {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final EventHandler handler = new EventHandler(mapper);
    @SuppressWarnings("unchecked") final Consumer<Product> create = mock(Consumer.class);
    final AtomicInteger deleted = new AtomicInteger();
    final Product product = new Product(1, "Produto", 10, null);
    Message message(Object event) throws Exception { return new Message(mapper.writeValueAsBytes(event), new MessageProperties()); }
    void consume(Message message, Function<Product,Product> find) {
        handler.handle(message, Product.class, Product::getProductId, body -> {}, find, create, deleted::set);
    }
    @Test void createsAndRoundTripsMetadata() throws Exception {
        var event = Event.create(UUID.randomUUID(), 1, product);
        consume(message(event), body -> null);
        verify(create).accept(argThat(body -> body.getProductId() == 1 && body.getName().equals("Produto")));
        var parsed = mapper.readTree(message(event).getBody());
        assertEquals(event.eventId().toString(), parsed.get("eventId").asText());
        assertEquals(event.correlationId().toString(), parsed.get("correlationId").asText());
    }
    @Test void redeliveryIgnoresServiceAddress() throws Exception {
        consume(message(Event.create(UUID.randomUUID(), 1, product)), body -> new Product(1,"Produto",10,"http://instance"));
        verifyNoInteractions(create);
    }
    @Test void conflictingDataIsNotRetried() throws Exception {
        var msg = message(Event.create(UUID.randomUUID(), 1, product));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consume(msg, body -> new Product(1,"Outro",10,null)));
        verifyNoInteractions(create);
    }
    @Test void rejectsMalformedOrMismatchedCommands() throws Exception {
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consume(new Message("bad json".getBytes(), new MessageProperties()), body -> null));
        var mismatch = message(Event.create(UUID.randomUUID(), 2, product));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consume(mismatch, body -> null));
        var missingMetadata = new Message("{\"eventType\":\"UNKNOWN\",\"productId\":1}".getBytes(), new MessageProperties());
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consume(missingMetadata, body -> null));
        verifyNoInteractions(create);
    }
    @Test void deletesByProductId() throws Exception {
        consume(message(Event.delete(UUID.randomUUID(), 7)), body -> null);
        assertEquals(7, deleted.get());
        verifyNoInteractions(create);
    }
    @Test void transientDatabaseFailureRemainsRetryable() throws Exception {
        var msg = message(Event.create(UUID.randomUUID(), 1, product));
        assertThrows(IllegalStateException.class, () -> consume(msg, body -> { throw new IllegalStateException("database offline"); }));
    }
}
