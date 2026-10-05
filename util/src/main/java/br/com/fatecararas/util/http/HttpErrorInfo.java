package br.com.fatecararas.util.http;

import java.time.ZonedDateTime;
import org.springframework.http.HttpStatus;

public class HttpErrorInfo {
    private ZonedDateTime timestamp;
    private String path;
    private int status;
    private String error;
    private String message;

    public HttpErrorInfo() { }

    public HttpErrorInfo(HttpStatus status, String path, String message) {
        this.timestamp = ZonedDateTime.now();
        this.path = path;
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
    }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
