package no.fintlabs.model.person;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.Person;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon;
import no.fint.model.felles.kompleksedatatyper.Personnavn;
import no.fint.model.resource.Link;
import no.fint.model.resource.felles.PersonResource;
import no.fint.model.utdanning.ot.OtUngdom;
import no.fintlabs.restutil.RestUtil;
import no.fintlabs.restutil.model.PersonData;
import no.fintlabs.restutil.model.RequestData;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PersonService {

    private final SimpleDateFormat dateFormat;
    private final RestUtil restUtil;

    public PersonService(RestUtil restUtil) {
        this.restUtil = restUtil;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    }

    public List<PersonResource> getPersonResources() {
        RequestData requestData = restUtil.getRequestData().block();

        if (requestData != null) {
            if (StringUtils.hasText(requestData.getErrorMessage())) {
                log.error(requestData.getErrorMessage());
            }

            return requestData.getOtUngdommer().stream()
                    .map(otUngdomData -> createPersonResource(otUngdomData.getPerson()))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @SneakyThrows
    private PersonResource createPersonResource(PersonData personData) {
        PersonResource personResource = new PersonResource();
        String fodselsNummer = personData.getFodselsnummer();

        if (!personData.getFodselsdato().isEmpty())
            personResource.setFodselsdato(dateFormat.parse(personData.getFodselsdato()));

        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(fodselsNummer);
        personResource.setFodselsnummer(identifikator);

        Personnavn personnavn = new Personnavn();
        personnavn.setFornavn(personData.getFornavn());
        personnavn.setEtternavn(personData.getEtternavn());
        personResource.setNavn(personnavn);

        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
        if (!personData.getEpost().isEmpty())
            kontaktinformasjon.setEpostadresse(personData.getEpost());
        if (!personData.getMobilnummer().isEmpty())
            kontaktinformasjon.setMobiltelefonnummer(personData.getMobilnummer());
        personResource.setKontaktinformasjon(kontaktinformasjon);

        personResource.addOtungdom(Link.with(OtUngdom.class, "systemid", fodselsNummer));
        personResource.addSelf(Link.with(Person.class, "fodselsnummer", fodselsNummer));

        return personResource;
    }

}
