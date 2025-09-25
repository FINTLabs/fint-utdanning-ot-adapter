package no.fintlabs.model.person;

import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.adapter.events.WriteableResourceRepository;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonRepository implements WriteableResourceRepository<PersonResource> {

    private final PersonService personService;

    public PersonRepository(PersonService personService) {
        this.personService = personService;
    }

    @Override
    public PersonResource saveResources(PersonResource resource, RequestFintEvent requestFintEvent) {
        return null;
    }

    @Override
    public List<PersonResource> getResources() {
        return personService.getPersonResources();
    }

    @Override
    public List<PersonResource> getUpdatedResources() {
        return new ArrayList<>();
    }
}