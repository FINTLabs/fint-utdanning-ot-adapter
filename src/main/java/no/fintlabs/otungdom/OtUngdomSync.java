package no.fintlabs.otungdom;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.FintIdentifikator;
import no.fint.model.felles.Person;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon;
import no.fint.model.felles.kompleksedatatyper.Personnavn;
import no.fint.model.resource.FintResource;
import no.fint.model.resource.Link;
import no.fint.model.resource.felles.PersonResource;
import no.fint.model.resource.utdanning.ot.OtUngdomResource;
import no.fint.model.utdanning.ot.OtUngdom;
import no.fintlabs.adapter.config.AdapterProperties;
import no.fintlabs.adapter.datasync.SyncData;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.sync.*;
import no.fintlabs.adapter.validator.ValidatorService;
import no.fintlabs.otungdom.vigo.OTUngdomData;
import no.fintlabs.otungdom.vigo.OtUngdomResponse;
import no.fintlabs.otungdom.vigo.PersonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This is a FINT adapter that synchronized OT ungdom data from Vigo to FINT.
 * The sync is done using regular FULL syncs that pull all OT ungdoms from a
 * Vigo OT endpoint, transforms it to FINT person resources and FINT OT Ungdom
 * resources, which are then pushed to a FINT Provider through one FULL-sync
 * per resource type.
 */
@Slf4j
@Service
public class OtUngdomSync {
    private final AdapterProperties adapterProperties;
    private final SimpleDateFormat dateFormat;

    private final WebClient vigoOtWebClient;
    private final int pageSize;
    private final ValidatorService validatorService;
    private final WebClient fintWebClient;

    public OtUngdomSync(AdapterProperties adapterProperties,
                        ValidatorService validatorService,
                        @Autowired @Qualifier("webClient") WebClient webClient,
                        @Autowired @Qualifier("vigoOtWebClient") WebClient vigoOtWebClient,
                        @Value("${fint.adapter.page-size:100}") int pageSize) {
        this.adapterProperties = adapterProperties;
        this.validatorService = validatorService;
        this.fintWebClient = webClient;
        this.vigoOtWebClient = vigoOtWebClient;
        this.pageSize = pageSize;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    }

    /**
     * Fetches Vigo OT ungdom, transform to FINT resources and push as full-sync to FINT.
     */
    @Scheduled(cron = "${fint.adapter.sync.cron}")
    public void syncOtUngdomFromVigoToFint() {
        log.info("Full sync of OT ungdom from Vigo to FINT started");

        fetchOtUngdomData()
                .flatMap(this::processOtUngdomData)
                .doOnSuccess(unused -> log.info("OT ungdom processing pipeline completed"))
                .doOnError(error -> log.error("OT ungdom processing pipeline failed", error))
                .subscribe(
                        unused -> log.info("OT ungdom full-sync completed successfully"),
                        error -> log.error("OT ungdom full-sync failed", error)
                );
    }

    /**
     * Fetch OT ungdom data from Vigo
     */
    private Mono<List<OTUngdomData>> fetchOtUngdomData() {
        return vigoOtWebClient.get()
                .retrieve()
                .bodyToMono(OtUngdomResponse.class)
                .doOnNext(this::checkForErrors)
                .map(OtUngdomResponse::getOtUngdommer)
                .filter(otUngdomData -> !otUngdomData.isEmpty())  // Skip if no OT ungdom
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No OT ungdom data found in response");
                    return Mono.empty();
                }))
                .doOnNext(otUngdomData -> log.info("Fetched {} OT ungdom", otUngdomData.size()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof Exception)
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying fetch OT ungdom data - attempt {}", retrySignal.totalRetries())))
                .onErrorResume(error -> {
                    log.error("Failed to fetch OT ungdom data after 3 retries", error);
                    return Mono.error(new RuntimeException("Unable to fetch OT ungdom data", error));
                });
    }

    /**
     * Log error message if present in response message.
     */
    private void checkForErrors(OtUngdomResponse ungdomResponse) {
        if (ungdomResponse.getErrorMessage() != null && !ungdomResponse.getErrorMessage().trim().isEmpty()) {
            log.error("Vigo OT error response: {}", ungdomResponse.getErrorMessage());
        }
        int ungdomCount = ungdomResponse.getOtUngdommer().size();
        if (ungdomResponse.getAntall() != ungdomCount) {
            log.warn("Vigo OT response contained {} while the 'antall' field says {}", ungdomCount, ungdomResponse.getAntall());
        }
    }

    /**
     * Process batch with fallback to individual processing on failure
     */
    private Mono<Void> processOtUngdomData(List<OTUngdomData> otUngdomData) {
        List<OtUngdomResource> fintOtUngdomer = otUngdomData.stream()
                .map(this::transformToFintOtUngdom)
                .toList();

        List<PersonResource> fintPersoner = otUngdomData.stream()
                .map(this::transformToFintPerson)
                .toList();

        String otUngdomUri = getCapability("otungdom").getEntityUri();
        String personUri = getCapability("person").getEntityUri();

        int perSyncConcurrency = calculatePerSyncConcurrency(otUngdomData, fintPersoner);

        Mono<Void> ungdomPush = fintOtUngdomer.isEmpty()
                ? Mono.empty()
                : pushFintResources(fintOtUngdomer, otUngdomUri, perSyncConcurrency);

        Mono<Void> personerPush = fintPersoner.isEmpty()
                ? Mono.empty()
                : pushFintResources(fintPersoner, personUri, perSyncConcurrency);

        return Mono.when(ungdomPush, personerPush);
    }

    /**
     * Calculates per-sync concurrency by dividing available processors
     * evenly across active syncs (OT-ungdom and Person).
     * Ensures at least one concurrent worker.
     */
    private int calculatePerSyncConcurrency(List<?> ungdommer, List<?> personer) {
        int parallelSyncs = (ungdommer.isEmpty() ? 0 : 1) + (personer.isEmpty() ? 0 : 1);
        return Math.max(1, Runtime.getRuntime().availableProcessors() / Math.max(1, parallelSyncs));
    }


    private OtUngdomResource transformToFintOtUngdom(OTUngdomData vigoUngdom) {
        OtUngdomResource otUngdomResource = new OtUngdomResource();
        String fodselsNummer = vigoUngdom.getPerson().getFodselsnummer();

        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(fodselsNummer);
        otUngdomResource.setSystemId(identifikator);

        otUngdomResource.addPerson(new Link("fodselsnummer/" + fodselsNummer));
        otUngdomResource.addEnhet(new Link("systemid/" + vigoUngdom.getOtData().getTilknytningnr()));
        otUngdomResource.addStatus(new Link("systemid/" + vigoUngdom.getOtData().getAktivitetskode()));
        otUngdomResource.addSelf(new Link("systemid/" + fodselsNummer));

        return otUngdomResource;
    }

    private PersonResource transformToFintPerson(OTUngdomData vigoUngdom) {
        PersonResource personResource = new PersonResource();
        PersonData personData = vigoUngdom.getPerson();
        String fodselsNummer = personData.getFodselsnummer();

        if (!personData.getFodselsdato().isEmpty())
            personResource.setFodselsdato(parseDate(personData.getFodselsdato()));

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

    private Date parseDate(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    private <T extends FintResource> Mono<Void> pushFintResources(List<T> fintResources, String capabilityUri, int concurrency) {
        log.debug("Pushing batch of {} FINT resources", fintResources.size());

        SyncData<T> syncData = SyncData.ofPostData(fintResources);

        Instant start = Instant.now();
        List<SyncPage> pages = this.createSyncPages(syncData.getResources(), syncData.getSyncType(), capabilityUri);
        return Flux.fromIterable(pages)
                .flatMap(this::sendPage, concurrency)
                .doOnComplete(() -> this.logDuration(syncData.getResources().size(), start))
                .then();
    }

    public <T extends FintResource> List<SyncPage> createSyncPages(List<T> resources, SyncType syncType, String capabilityUri) {
        String corrId = UUID.randomUUID().toString();
        int resourceSize = resources.size();
        int amountOfPages = (resourceSize + this.pageSize - 1) / this.pageSize;
        List<SyncPage> pages = new ArrayList<>();

        for (int resourceIndex = 0; resourceIndex < resourceSize; resourceIndex += this.pageSize) {
            SyncPage syncPage = this.createSyncPage(corrId, resources, syncType, resourceSize, amountOfPages, resourceIndex, capabilityUri);
            pages.add(syncPage);
        }

        if (this.adapterProperties.isDebug()) {
            this.validatorService.validate(pages, resourceSize);
        }

        return pages;
    }

    private <T extends FintResource> SyncPage createSyncPage(String corrId, List<T> resources, SyncType syncType, int resourceSize, int totalPages, int resourceIndex, String capabilityUri) {
        List<SyncPageEntry> syncPageEntries = this.createSyncPageEntries(resources, resourceIndex);
        SyncPageMetadata syncPageMetadata = this.getSyncPageMetadata(corrId, resourceSize, totalPages, resourceIndex, syncPageEntries, capabilityUri);
        return FullSyncPage.builder().metadata(syncPageMetadata).resources(syncPageEntries).syncType(syncType).build();
    }

    private <T extends FintResource> List<SyncPageEntry> createSyncPageEntries(List<T> resources, int resourceIndex) {
        int stoppingIndex = Math.min(resourceIndex + this.pageSize, resources.size());
        return resources.subList(resourceIndex, stoppingIndex).stream()
                .map(this::createSyncPageEntry).toList();
    }

    private SyncPageEntry createSyncPageEntry(FintResource resource) {
        FintIdentifikator fintIdentifikator = resource.getIdentifikators().values().iterator().next();

        return SyncPageEntry.of(fintIdentifikator.getIdentifikatorverdi(), resource);
    }

    private SyncPageMetadata getSyncPageMetadata(String corrId, int resourceAmount, int totalPages, int i, List<SyncPageEntry> entries, String capabilityUri) {
        String adapterId = this.adapterProperties.getId();
        String orgId = this.adapterProperties.getOrgId();
        int pageNo = i / this.pageSize + 1;

        return SyncPageMetadata.builder()
                .orgId(orgId)
                .adapterId(adapterId)
                .corrId(corrId)
                .totalPages(totalPages)
                .totalSize(resourceAmount)
                .pageSize(entries.size())
                .page(pageNo)
                .uriRef(capabilityUri)
                .time(System.currentTimeMillis())
                .build();
    }

    private Mono<?> sendPage(SyncPage page) {
        return this.fintWebClient
                .method(page.getSyncType().getHttpMethod())
                .uri("/provider" + page.getMetadata().getUriRef())
                .body(Mono.just(page), SyncPage.class)
                .retrieve()
                .toBodilessEntity()
                .flatMap(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.debug("Posted {} sync page {}/{} with {} resources to {}",
                                page.getSyncType(),
                                page.getMetadata().getPage(),
                                page.getMetadata().getTotalPages(),
                                page.getResources().size(),
                                page.getMetadata().getUriRef());
                        return Mono.empty();
                    } else {
                        return Mono.error(new WebClientResponseException(
                                "Non-2xx response: " + response.getStatusCode(),
                                response.getStatusCode().value(),
                                "Failed to " + page.getSyncType().getHttpMethod() + " sync page",
                                null, null, null));
                    }
                })
                .retryWhen(Retry.backoff(4, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientResponseException)
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying {} sync page {} of {} - attempt {}",
                                        page.getSyncType(), page.getMetadata().getPage(), page.getMetadata().getTotalPages(), retrySignal.totalRetries()))
                        .maxBackoff(Duration.ofSeconds(8)))
                .doOnError(error -> log.error("Failed to {} {} sync page {} of {} after 4 retries",
                        page.getSyncType().getHttpMethod(), page.getSyncType(), page.getMetadata().getPage(), page.getMetadata().getTotalPages(), error));
    }

    private void logDuration(int resourceSize, Instant start) {
        Duration timeElapsed = Duration.between(start, Instant.now());
        int amountOfPages = (resourceSize + this.pageSize - 1) / this.pageSize;
        log.debug("Syncing {} resources in {} pages took {}:{}:{} to complete", resourceSize, amountOfPages, "%02d".formatted(timeElapsed.toHoursPart()), "%02d".formatted(timeElapsed.toMinutesPart()), "%02d".formatted(timeElapsed.toSecondsPart()));
    }

    private AdapterCapability getCapability(String resource) {
        return adapterProperties.getCapabilityByResource(resource);
    }
}
