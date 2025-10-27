package no.fintlabs.otungdom.vigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response body from a Vigo OT endpoint.
 */
@Data
public class OtUngdomResponse {

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("antall")
    private int antall;

    @JsonProperty("otungdommer")
    private List<OTUngdomData> otUngdommer;

}
