package no.fintlabs.otungdom.vigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PersonData {

    @JsonProperty("fornavn")
    private String fornavn;

    @JsonProperty("etternavn")
    private String etternavn;

    @JsonProperty("fdato")
    private String fodselsdato;

    @JsonProperty("fnr")
    private String fodselsnummer;

    @JsonProperty("epost")
    private String epost;

    @JsonProperty("mobilnr")
    private String mobilnummer;

}
