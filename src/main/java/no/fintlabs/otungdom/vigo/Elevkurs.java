package no.fintlabs.otungdom.vigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Elevkurs {

    @JsonProperty("promrkode")
    private String promrkode;

    @JsonProperty("promrnavn")
    private String promrnavn;

    @JsonProperty("start")
    private String start;

    @JsonProperty("slutt")
    private String slutt;

}
