package no.fintlabs.restutil.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RequestData {

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("antall")
    private int antall;

    @JsonProperty("otungdommer")
    private List<OTUngdomData> otUngdommer;

}
