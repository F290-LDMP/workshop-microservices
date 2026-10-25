package br.com.fatecararas.api.event;

import java.time.Instant;
import java.util.UUID;

public record Event<T>(UUID eventId, UUID correlationId, Type eventType,
                       int productId, Instant eventCreatedAt, T data) {
    public enum Type { CREATE, DELETE }
    public static <T> Event<T> create(UUID correlationId, int productId, T data) {
        return new Event<>(UUID.randomUUID(), correlationId, Type.CREATE, productId, Instant.now(), data);
    }
    public static Event<Void> delete(UUID correlationId, int productId) {
        return new Event<>(UUID.randomUUID(), correlationId, Type.DELETE, productId, Instant.now(), null);
    }
}
