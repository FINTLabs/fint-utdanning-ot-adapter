package no.fintlabs.model.person;

import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourceSubscriber;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.sync.SyncPageEntry;
import no.fintlabs.adapter.validator.ValidatorService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PersonSubscriber extends ResourceSubscriber<PersonResource, PersonPublisher> {

    protected PersonSubscriber(WebClient webClient, AdapterProperties props, PersonPublisher publisher, ValidatorService validatorService) {
        super(webClient, props, publisher, validatorService);
    }

    @Override
    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilities().get("person");
    }

    @Override
    protected SyncPageEntry createSyncPageEntry(PersonResource resource) {
        return SyncPageEntry.of(resource.getFodselsnummer().getIdentifikatorverdi(), resource);
    }
}
