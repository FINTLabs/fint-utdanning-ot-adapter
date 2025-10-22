package no.fintlabs.model.otungdom;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.utdanning.ot.OtUngdomResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourcePublisher;
import no.fintlabs.adapter.datasync.ResourceRepository;
import no.fintlabs.adapter.datasync.SyncData;
import no.fintlabs.adapter.models.AdapterCapability;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OtUngdomPublisher extends ResourcePublisher<OtUngdomResource, ResourceRepository<OtUngdomResource>> {

    public OtUngdomPublisher(OtUngdomRepository repository, AdapterProperties adapterProperties) {
        super(repository, adapterProperties);
    }

    @Override
    @Scheduled(initialDelayString = "1000", fixedRateString = "86400000")
    public void doFullSync() {
        log.info("Start full sync for resource {}", getCapability().getEntityUri());
        try {
            submit(SyncData.ofPostData(repository.getResources()));
        } catch (Exception e) {
            log.error("Full sync failed for resource {}", getCapability().getEntityUri(), e);
        }
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
