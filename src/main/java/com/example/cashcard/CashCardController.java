package com.example.cashcard;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController // This tells Spring that this class is a Component of type RestController and capable of handling HTTP requests
@RequestMapping("/cashcards") // This is a companion to @RestController that indicates which address request must have to acces this Controller
public class CashCardController {

    // Inject the CashCardRepository into CashCardController
    private CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository){
        this.cashCardRepository = cashCardRepository;
    }


    @GetMapping("/{requestedId}")
    public ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal){ // @PathVariable makes Spring Web 
        //aware of the requestedId supplied in the HTTP request

        // Call CrudRepository.findByID which returns an Optional
        //Optional<CashCard> cashCardOptional = Optional.ofNullable(cashCardRepository.findByIdAndOwner(requestedId, principal.getName()));

        CashCard cashCard = findCashCard(requestedId, principal);

        // /If cashCardOptional.isPresent() is true then the repository successfully found the CashCard and we can retrieve it with 
        //cashCardOptional.get().
        if(cashCard != null){
            return ResponseEntity.ok(cashCard);
        }
        else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb, Principal principal){
        
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());

        // Saves a new CashCard and returns the saved object with a unique id provided by the database
        CashCard savedCashCard = cashCardRepository.save(cashCardWithOwner);
        // This is constructing a URI to a newly created CashCard. This is the URI that the caller cal then use to GET the newly-created CashCard.
        URI locationOfNewCashCard = ucb
            .path("cashcards/{id}")
            .buildAndExpand(savedCashCard.id()) //savedCashCard.id is used as the identifier, which matches the GET endpoint's specification of cashcards/<CashCard.id>
            .toUri();
        // Return 201 Create with the corrent Location Header
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @GetMapping
    public ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(), // CrusRepository.findAll() will automatically return all CashCard records from the database
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount")) // getSortOf() method provides default values for the page, size, and sort parameters.
        ));
        return ResponseEntity.ok(page.getContent());
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate, Principal principal){
        CashCard cashCard = findCashCard(requestedId, principal); // retrieval of the CashCard to the submitted requestedId to ensure only the authenticated owner may update this CashCard.
        
        if(cashCard != null){
            CashCard updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
            cashCardRepository.save(updatedCashCard);
    
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }    

    private CashCard findCashCard(Long requestedId, Principal principal){
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal){
        // Check if record does exists
        if(cashCardRepository.existsByIdAndOwner(id, principal.getName())){
            // Delete the record
            cashCardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}