package no.fintlabs.model.otungdom;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.ot.OtUngdomResource;
import no.fintlabs.restutil.RestUtil;
import no.fintlabs.restutil.model.OTUngdomData;
import no.fintlabs.restutil.model.RequestData;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class OtUngdomService {

    private final RestUtil restUtil;

    public OtUngdomService(RestUtil restUtil) {
        this.restUtil = restUtil;
    }

    public List<OtUngdomResource> getOtUngdomResources() {
        RequestData requestData = restUtil.getRequestData().block();

        if (requestData != null) {
            if (StringUtils.hasText(requestData.getErrorMessage())) {
                log.error(requestData.getErrorMessage());
            }
            return requestData.getOtUngdommer().stream().map(this::createOtUngdomResource).toList();
        } else {
            return Collections.emptyList();
        }
    }

    @SneakyThrows
    private OtUngdomResource createOtUngdomResource(OTUngdomData otUngdomData) {
        OtUngdomResource otUngdomResource = new OtUngdomResource();
        String fodselsNummer = otUngdomData.getPerson().getFodselsnummer();

        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(fodselsNummer);
        otUngdomResource.setSystemId(identifikator);

        otUngdomResource.addPerson(new Link("fodselsnummer/" + fodselsNummer));
        otUngdomResource.addEnhet(new Link("systemid/" + otUngdomData.getOtData().getTilknytningnr()));
        otUngdomResource.addStatus(new Link("systemid/" + otUngdomData.getOtData().getAktivitetskode()));
        otUngdomResource.addSelf(new Link("systemid/" + fodselsNummer));

        return otUngdomResource;
    }

}
