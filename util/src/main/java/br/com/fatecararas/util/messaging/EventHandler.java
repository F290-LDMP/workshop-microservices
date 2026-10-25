package br.com.fatecararas.util.messaging;

import br.com.fatecararas.api.event.Event;
import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.api.core.DomainValidation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

public class EventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EventHandler.class);
    private final ObjectMapper mapper;
    public EventHandler(ObjectMapper mapper) { this.mapper = mapper; }
    public <T> void handle(Message message, Class<T> type, ToIntFunction<T> productId,
            Consumer<T> validate, Function<T,T> find, Consumer<T> create, IntConsumer delete) {
        Event<T> event;
        try {
            event = mapper.readValue(
                    message.getBody(),
                    mapper.getTypeFactory().constructParametricType(Event.class, type)
            );

            if (event.eventId() == null || event.correlationId() == null || event.eventCreatedAt() == null || event.eventType() == null)
                throw new InvalidInputException("Missing event metadata");

            DomainValidation.id(event.productId());

            if (event.eventType() == Event.Type.CREATE) {
                if (event.data() == null || productId.applyAsInt(event.data()) != event.productId())
                    throw new InvalidInputException("Event productId and payload must match");
                validate.accept(event.data());
            } else if (event.data() != null) throw new InvalidInputException("DELETE data must be null");
        } catch (Exception ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid command", ex);
        }

        LOG.info("Consuming eventId={} correlationId={} productId={}", event.eventId(), event.correlationId(), event.productId());

        try {
            if (event.eventType() == Event.Type.DELETE) delete.accept(event.productId());
            else {
                T existing = find.apply(event.data());
                if (existing != null) ensureSame(existing, event.data());
                else {
                    try { create.accept(event.data()); }
                    catch (InvalidInputException duplicate) {
                        // A REST request may have inserted the same key between lookup and insert.
                        existing = find.apply(event.data());
                        if (existing == null) throw duplicate;
                        ensureSame(existing, event.data());
                    }
                }
            }
        } catch (InvalidInputException ex) {
            throw new AmqpRejectAndDontRequeueException("Business conflict", ex);
        }

        LOG.info("Processed eventId={} correlationId={} productId={}", event.eventId(), event.correlationId(), event.productId());
    }
    private void ensureSame(Object existing, Object incoming) {
        ObjectNode a = mapper.valueToTree(existing), b = mapper.valueToTree(incoming);
        a.remove("serviceAddress"); b.remove("serviceAddress");
        if (!a.equals(b)) throw new InvalidInputException("Conflicting data for existing business key");
    }
}
