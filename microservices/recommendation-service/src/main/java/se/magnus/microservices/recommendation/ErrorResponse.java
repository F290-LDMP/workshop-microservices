package se.magnus.microservices.recommendation;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        String path,
        int status,
        String error,
        String message) {
}
