package no.fintlabs.restutil;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.restutil.model.RequestData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
public class RestUtil {

    private final WebClient webClient;
    private final String orgNumber;
    private final String apiKey;


    public RestUtil(@Value("${fint.vigo.ot.url}") String vigoOtUrl,
                    @Value("${fint.fylkesnr}") String orgNumber,
                    @Value("${fint.api-key}") String apiKey,
                    WebClient.Builder webClientBuilder) {
        this.orgNumber = orgNumber;
        this.apiKey = apiKey;
        Objects.requireNonNull(vigoOtUrl, "vigoOtUrl must not be null");
        Objects.requireNonNull(orgNumber, "orgNumber must not be null");
        Objects.requireNonNull(apiKey, "apiKey must not be null");

        this.webClient = webClientBuilder
                .baseUrl(vigoOtUrl)
                .codecs(this::configureCodecs)
                .build();
    }

    private void configureCodecs(ClientCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10 MB buffer size
    }

    public Mono<RequestData> getRequestData() {
        return webClient.get()
                .header("api-key", apiKey)
                .header("fylkesnr", orgNumber)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(RequestData.class);
    }

}
