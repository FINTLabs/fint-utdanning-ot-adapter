package no.fintlabs.otungdom;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.utdanning.ot.OTUngdomResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourcePublisher;
import no.fintlabs.adapter.datasync.ResourceRepository;
import no.fintlabs.adapter.datasync.SyncData;
import no.fintlabs.adapter.models.AdapterCapability;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class OTUngdomPublisher extends ResourcePublisher<OTUngdomResource, ResourceRepository<OTUngdomResource>> {

    public OTUngdomPublisher(OTUngdomRepository repository, AdapterProperties adapterProperties) {
        super(repository, adapterProperties);
    }

    @Override
    @Scheduled(initialDelayString = "1000", fixedRateString = "500000")
    public void doFullSync() {
        log.info("Start full sync for resource {}", getCapability().getEntityUri());
        submit(SyncData.ofPostData(repository.getResources()));
    }

    @Override
    public void doDeltaSync() {
        log.info("Start delta sync for resource {}", getCapability().getEntityUri());
        submit(SyncData.ofPatchData(repository.getUpdatedResources()));
    }

    @Override
    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilityByResource("otungdom");
    }
}
