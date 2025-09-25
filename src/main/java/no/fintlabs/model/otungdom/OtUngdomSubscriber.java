package no.fintlabs.model.otungdom;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.utdanning.ot.OtUngdomResource;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.ResourceSubscriber;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.sync.SyncPageEntry;
import no.fintlabs.adapter.validator.ValidatorService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class OtUngdomSubscriber extends ResourceSubscriber<OtUngdomResource, OtUngdomPublisher> {

    protected OtUngdomSubscriber(WebClient webClient, AdapterProperties props, OtUngdomPublisher publisher, ValidatorService validatorService) {
        super(webClient, props, publisher, validatorService);
    }

    @Override
    protected AdapterCapability getCapability() {
        return adapterProperties.getCapabilities().get("otungdom");
    }

    @Override
    protected SyncPageEntry createSyncPageEntry(OtUngdomResource resource) {
        return SyncPageEntry.of(resource.getSystemId().getIdentifikatorverdi(), resource);
    }
}
