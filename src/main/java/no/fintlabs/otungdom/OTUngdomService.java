package no.fintlabs.otungdom;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.Link;
import no.fint.model.resource.felles.PersonResource;
import no.fint.model.resource.utdanning.kodeverk.OTEnhetResource;
import no.fint.model.resource.utdanning.kodeverk.OTStatusResource;
import no.fint.model.resource.utdanning.ot.OTUngdomResource;
import no.fintlabs.restutil.RestUtil;
import no.fintlabs.restutil.model.OTUngdomData;
import no.fintlabs.restutil.model.RequestData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OTUngdomService {

    private final RestUtil restUtil;

    public OTUngdomService(RestUtil restUtil) {
        this.restUtil = restUtil;
    }

    public List<OTUngdomResource> getOTUngdomResources() {
        List<OTUngdomResource> otungdomResources = new ArrayList<>();
        RequestData requestData = restUtil.getRequestData().block();

        if (requestData != null) {
            requestData.getOtungdommer().forEach(otUngdomData -> {
                OTUngdomResource otungdomResource = createOTUngdomResource(otUngdomData);
                otungdomResources.add(otungdomResource);
            });
        }

        return otungdomResources;
    }

    @SneakyThrows
    private OTUngdomResource createOTUngdomResource(OTUngdomData otUngdomData) {
        OTUngdomResource otungdomResource = new OTUngdomResource();
        // What is the unique identifier for OTUngdomResource?
        String identifierId = "";

        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(identifierId);
        otungdomResource.setSystemId(identifikator);

        otungdomResource.addLink("person", Link.with(PersonResource.class, "fodselsnummer", otUngdomData.getPerson().getFodselsnummer()));
        otungdomResource.addLink("otstatus", Link.with(OTStatusResource.class, "systemid", otUngdomData.getOtData().getAktivitetskode()));
        otungdomResource.addLink("otenhet", Link.with(OTEnhetResource.class, "systemid", otUngdomData.getOtData().getTilknytningnr()));
        otungdomResource.addSelf(Link.with(OTUngdomResource.class, "systemid", identifierId));

        return otungdomResource;
    }

}
