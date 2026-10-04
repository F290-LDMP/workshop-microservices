package br.com.fatecararas.composite.client;

import br.com.fatecararas.api.exceptions.InvalidInputException;
import br.com.fatecararas.api.exceptions.NotFoundException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RemoteServiceExceptionDecoder {
    @Bean
    ErrorDecoder remoteErrorDecoder() {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            String message = "Falha remota em " + methodKey + " (HTTP " + response.status() + ")";
            if (response.status() == 404) return new NotFoundException(message);
            if (response.status() == 422 || response.status() == 400) return new InvalidInputException(message);
            return fallback.decode(methodKey, response);
        };
    }
}
