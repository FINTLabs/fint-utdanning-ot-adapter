package no.fintlabs.otungdom.vigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OTData {

    @JsonProperty("tilknytningnr")
    private String tilknytningnr;

    @JsonProperty("tilknytningnavn")
    private String tilknytningnavn;

    @JsonProperty("aktivitetskode")
    private String aktivitetskode;

}
