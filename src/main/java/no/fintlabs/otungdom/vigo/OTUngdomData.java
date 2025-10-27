package no.fintlabs.otungdom.vigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OTUngdomData {

    @JsonProperty("person")
    private PersonData person;

    @JsonProperty("otData")
    private OTData otData;

    @JsonProperty("elevkurs")
    private Elevkurs elevkurs;

}
