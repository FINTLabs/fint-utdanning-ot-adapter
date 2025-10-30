package no.fintlabs.model.otungdom;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.fintlabs.otungdom.OtUngdomSync;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.extension.MediaType.APPLICATION_JSON;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@EnableWireMock({
        @ConfigureWireMock(name = "fint-provider", portProperties = "provider.port", baseUrlProperties = "provider.base-url"),
        @ConfigureWireMock(name = "fint-idp", portProperties = "idp.port", baseUrlProperties = "idp.base-url"),
        @ConfigureWireMock(name = "vigo", portProperties = "vigo.port", baseUrlProperties = "vigo.base-url")
})
@TestPropertySource(properties = {
        "fint.adapter.base-url=http://localhost:${provider.port}",
        "fint.vigo.ot.url=http://localhost:${vigo.port}",
        "fint.fylkesnr=42",
        "fint.api-key=test-api-key",
        "spring.security.oauth2.client.provider.fint-idp.token-uri=http://localhost:${idp.port}/oauth/token",
        "fint.sync.enabled=false"
})
@ActiveProfiles("test")
class OtUngdomSyncTest {
    @Value("${fint.adapter.id}")
    private String adapterId;

    @Value("${fint.fylkesnr}")
    private String vigoFylkesNr;

    @Value("${fint.api-key}")
    private String vigoApiKey;

    @InjectWireMock("vigo")
    WireMockServer vigo;

    @InjectWireMock("fint-provider")
    WireMockServer fintProvider;

    @Autowired
    private OtUngdomSync otUngdomSync;

    @Test
    void syncOtUngdomFromVigoToFint() {
        String expectedOtUngdomResourcesJsonPost = expectedOtUngdomResourcesJsonPost();
        String expectedPersonResourcesJsonPost = expectedPersonResourcesJsonPost();

        fintProvider.stubFor(post("/provider/utdanning/ot/otungdom")
                .willReturn(aResponse().withStatus(200)));
        fintProvider.stubFor(post("/provider/utdanning/ot/person")
                .willReturn(aResponse().withStatus(200)));

        otUngdomSync.syncOtUngdomFromVigoToFint().block();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    vigo.verify(getRequestedFor(urlEqualTo("/"))
                            .withHeader("fylkesnr", equalTo(vigoFylkesNr))
                            .withHeader("api-key", equalTo(vigoApiKey))
                            .withHeader("Accept", equalTo(APPLICATION_JSON.toString())));
                });

        // Wait for the reactive flow to complete
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    fintProvider.verify(postRequestedFor(urlEqualTo("/provider/utdanning/ot/otungdom"))
                            .withRequestBody(matchingJsonPath("$.metadata.adapterId", equalTo(adapterId)))
                            .withRequestBody(matchingJsonPath("$.metadata.orgId", equalTo("fintlabs.no")))
                            .withRequestBody(matchingJsonPath("$.metadata.totalSize", equalTo("1")))
                            .withRequestBody(matchingJsonPath("$.metadata.page", equalTo("1")))
                            .withRequestBody(matchingJsonPath("$.metadata.pageSize", equalTo("1")))
                            .withRequestBody(matchingJsonPath("$.metadata.totalPages", equalTo("1")))
                            .withRequestBody(matchingJsonPath("$.metadata.uriRef", equalTo("/utdanning/ot/otungdom")))
                            .withRequestBody(matchingJsonPath("$.resources", equalToJson(expectedOtUngdomResourcesJsonPost)))
                            .withRequestBody(matchingJsonPath("$.syncType", equalTo("FULL"))));
                });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            fintProvider.verify(postRequestedFor(urlEqualTo("/provider/utdanning/ot/person"))
                    .withRequestBody(matchingJsonPath("$.metadata.adapterId", equalTo(adapterId)))
                    .withRequestBody(matchingJsonPath("$.metadata.orgId", equalTo("fintlabs.no")))
                    .withRequestBody(matchingJsonPath("$.metadata.totalSize", equalTo("1")))
                    .withRequestBody(matchingJsonPath("$.metadata.page", equalTo("1")))
                    .withRequestBody(matchingJsonPath("$.metadata.pageSize", equalTo("1")))
                    .withRequestBody(matchingJsonPath("$.metadata.totalPages", equalTo("1")))
                    .withRequestBody(matchingJsonPath("$.metadata.uriRef", equalTo("/utdanning/ot/person")))
                    .withRequestBody(matchingJsonPath("$.resources", equalToJson(expectedPersonResourcesJsonPost)))
                    .withRequestBody(matchingJsonPath("$.syncType", equalTo("FULL")))
            );
        });
    }

    private String expectedOtUngdomResourcesJsonPost() {
        return """
                [
                  {
                    "identifier": "0102200312345",
                    "resource": {
                      "systemId": {
                        "identifikatorverdi": "0102200312345"
                      },
                      "_links": {
                        "person": [
                          {
                            "href": "fodselsnummer/0102200312345"
                          }
                        ],
                        "enhet": [
                          {
                            "href": "systemid/11"
                          }
                        ],
                        "status": [
                          {
                            "href": "systemid/f1"
                          }
                        ],
                        "self": [
                          {
                            "href": "systemid/0102200312345"
                          }
                        ]
                      }
                    }
                  }
                ]
                """;
    }

    private String expectedPersonResourcesJsonPost() {
        return """
                [
                  {
                    "identifier": "0102200312345",
                    "resource": {
                      "kontaktinformasjon": {
                        "epostadresse": "foo.bar.1@test.no",
                        "mobiltelefonnummer": "11111111"
                      },
                      "fodselsdato": "2003-01-31T23:00:00.000+00:00",
                      "fodselsnummer": {
                        "identifikatorverdi": "0102200312345"
                      },
                      "navn": {
                        "fornavn": "Foo 1",
                        "etternavn": "Bar 1"
                      },
                      "_links": {
                        "otungdom": [
                          { "href": "systemid/0102200312345" }
                        ],
                        "self": [
                          { "href": "fodselsnummer/0102200312345" }
                        ]
                      }
                    }
                  }
                ]
                """;
    }

}