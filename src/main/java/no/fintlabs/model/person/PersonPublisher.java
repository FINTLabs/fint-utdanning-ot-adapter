package no.fintlabs.model.person;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourcePublisher;
import no.fintlabs.adapter.datasync.ResourceRepository;
import no.fintlabs.adapter.datasync.SyncData;
import no.fintlabs.adapter.models.AdapterCapability;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PersonPublisher extends ResourcePublisher<PersonResource, ResourceRepository<PersonResource>> {

    public PersonPublisher(PersonRepository repository, AdapterProperties adapterProperties) {
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

    // Not in use
    @Override
    public void doDeltaSync() {
        submit(SyncData.ofPatchData(repository.getUpdatedResources()));
    }

    @Override
    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilityByResource("person");
    }
}
