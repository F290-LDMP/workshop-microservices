package se.magnus.util.http;

import java.time.Instant;

public record HttpErrorInfo(
        Instant timestamp,
        String path,
        int status,
        String error,
        String message) {
}
