package br.com.fatecararas.composite;

import br.com.fatecararas.composite.config.SecurityConfiguration;
import br.com.fatecararas.composite.service.ProductCompositeIntegrationService;
import br.com.fatecararas.composite.web.ProductCompositeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductCompositeController.class)
@org.springframework.test.context.ContextConfiguration(classes = {ProductCompositeController.class, SecurityConfiguration.class})
class CompositeSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean JwtDecoder decoder;
    @MockBean ProductCompositeIntegrationService integration;
    @Test void missingAndMalformedTokensAreUnauthorized() throws Exception {
        mvc.perform(get("/product-composite/1")).andExpect(status().isUnauthorized());
        when(decoder.decode("invalid")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("invalid"));
        mvc.perform(get("/product-composite/1").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(integration);
    }
    @Test void readerCanReadButCannotPublishOrDelete() throws Exception {
        var reader = jwt().authorities(new SimpleGrantedAuthority("SCOPE_product:read"));
        mvc.perform(get("/product-composite/1").with(reader)).andExpect(status().isOk());
        mvc.perform(post("/product-composite").with(reader).contentType("application/json").content("{\"productId\":1}")).andExpect(status().isForbidden());
        mvc.perform(delete("/product-composite/1").with(reader)).andExpect(status().isForbidden());
        verify(integration).get(1);
        verifyNoMoreInteractions(integration);
    }
    @Test void writerCanPublishWithoutCsrfAndOtherPathsAreDenied() throws Exception {
        var writer = jwt().authorities(new SimpleGrantedAuthority("SCOPE_product:write"));
        mvc.perform(post("/product-composite").with(writer).contentType("application/json").content("{\"productId\":1}")).andExpect(status().isAccepted());
        mvc.perform(delete("/product-composite/1").with(writer)).andExpect(status().isAccepted());
        mvc.perform(get("/actuator/env").with(writer)).andExpect(status().isForbidden());
        verify(integration).create(any());
        verify(integration).delete(1);
    }
}
