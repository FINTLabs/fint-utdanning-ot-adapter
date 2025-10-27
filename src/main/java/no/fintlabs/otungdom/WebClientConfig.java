package no.fintlabs.otungdom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

@Configuration
public class WebClientConfig {

    @Value("${fint.vigo.ot.url}")
    String vigoOtUrl;

    @Value("${fint.fylkesnr}")
    String orgNumber;

    @Value("${fint.api-key}")
    String apiKey;

    /**
     * Bean factory for Vigo OT {@link WebClient}.
     */
    @Bean
    public WebClient vigoOtWebClient() {
        Objects.requireNonNull(vigoOtUrl, "vigoOtUrl must not be null");
        Objects.requireNonNull(orgNumber, "orgNumber must not be null");
        Objects.requireNonNull(apiKey, "apiKey must not be null");

        return WebClient.builder()
                .baseUrl(vigoOtUrl)
                .codecs(this::configureCodecs)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("fylkesnr", orgNumber)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private void configureCodecs(ClientCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10 MB buffer size
    }
}
