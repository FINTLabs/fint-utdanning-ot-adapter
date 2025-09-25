package no.fintlabs.model.otungdom;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.utdanning.ot.OtUngdomResource;
import no.fintlabs.adapter.events.WriteableResourceRepository;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class OtUngdomRepository implements WriteableResourceRepository<OtUngdomResource> {

    private final OtUngdomService otUngdomService;

    public OtUngdomRepository(OtUngdomService otUngdomService) {
        this.otUngdomService = otUngdomService;
    }

    @Override
    public OtUngdomResource saveResources(OtUngdomResource tjenesteResource, RequestFintEvent requestFintEvent) {
        return null;
    }

    @Override
    public List<OtUngdomResource> getResources() {
        return otUngdomService.getOtUngdomResources();
    }

    @Override
    public List<OtUngdomResource> getUpdatedResources() {
        return new ArrayList<>();
    }
}