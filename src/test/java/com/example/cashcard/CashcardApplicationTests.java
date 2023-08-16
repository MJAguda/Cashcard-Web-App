package com.example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // This will start out Spring Boot applicatino and make it available for your test to perform requests to it.
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CashCardApplicationTests {

    // We've asked Spring to inject a test helper that'll allow us to make HTTP requests to the locally running application.
    @Autowired // @Autowired is a form of Spring dependency injection it's best use only in tests.
    TestRestTemplate restTemplate;

    @Test
    @DirtiesContext
    void shouldReturnACashCardWhenDataIsSaved() {
        // We use restTemplate to make an HTTP GET requests to our application endpoints /cashcards/99
        // restTemplate will return a ResponseEntity, which we'eve captured in a variable we've named response. 
        //ResponseEntity is another helpful Spring object that provides valuable information about what happed with out requests.
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123") // Basic Auth in HTTP tests
            .getForEntity("/cashcards/99", String.class); 
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK); // Inspect many aspects of the response, including the HTTP Response Status Code, which we expect to be 200 OK

        DocumentContext documentContext = JsonPath.parse(response.getBody()); // Converts the response String into a JSON-aware object with lots of helper methods.
        Number id = documentContext.read("$.id");
        assertThat(id).isEqualTo(99);

        Double amount = documentContext.read("$.amount");
        assertThat(amount).isEqualTo(123.45);
    }

    @Test
    void shouldNotReturnACashCardWithAnUnknownId() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards/1000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
    }

    @Test
    @DirtiesContext // @DirtiesContext Causing Spring to start with a clean slate, as if those other tests hadn't been run
    void shouldCreateANewCashCard(){
        // The database will create and manage all unique CashCard.id values for us with the contant amount.
        CashCard newCashCard = new CashCard(null, 250.00, null);
        // Provide newCashCard data for the new CashCard
        ResponseEntity<Void> createResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .postForEntity("/cashcards", newCashCard, Void.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // We now expect the HTTP response status code to be 201 CREATED
        URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
        // We'll use the Location header's information to fetch the newly create CashCard
        ResponseEntity<String> getResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity(locationOfNewCashCard, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify that the new CashCard.id is not null, and the newly created CashCard.amount is 250.00.
        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        Double amount = documentContext.read("$.amount");

        assertThat(id).isNotNull();
        assertThat(amount).isEqualTo(250.00);
    }

    @Test // Expects a GET endpoint which returns multiple CashCard objects.
    void shouldReturnAllCashCardsWhenListIsRequested(){
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        int cashCardCount = documentContext.read("$.length()"); // Calculates the length of the array
        assertThat(cashCardCount).isEqualTo(3);

        JSONArray ids = documentContext.read("$..id"); // Retrieves the list of all id values returned
        assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

        JSONArray amounts = documentContext.read("$..amount"); // Collects all amounts returned
        assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.00, 150.00); // containsExactlyInAnyOrder(...) asserts that while the list must contain everything we assert, the order does not matter.
    }

    @Test
    void shouldReturnAPageOfCashCards(){ // Paging
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void ShouldReturnASortedPageOfCashCards(){ // Sorting, page=0 Get the firstpage, size=1 Each page has size 1, sort=amount,desc
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(1);

        double amount = documentContext.read("$[0].amount");
        assertThat(amount).isEqualTo(150.00);
    }

    @Test // Test which doesn't send any pagination or sorting paramters
    void shouldReturnASortedPageOfCashCardsWithNoParametersAndUserDefaultValues(){ 
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123") 
            .getForEntity("/cashcards", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(3);

        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
    }

    @Test // Verify Basic Auth, test if Username and Password is Incorrect
    void shouldNotReturnACashWhenUsingBadCredentials(){
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("BAD-USER", "abc123")
            .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        response = restTemplate
            .withBasicAuth("sarah1", "BAD-PASSWORD")
            .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test // Role Verification
    void shouldRejectUsersWhoAreNotCardOwners(){
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("hank-owns-no-cards", "qrs456")
            .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test // Test that usrs cannot access each other's data
    void shouldNotAllowAccessToCashCardsTheyDoNotOwn(){
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards/102", String.class); // kumar2's data
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test // Test for updating a cashcard
    @DirtiesContext
    void shouldUpdateAnExistingCashCard(){
        CashCard cashCardUpdate = new CashCard (null, 19.99, null);

        HttpEntity<CashCard> request = new HttpEntity<CashCard>(cashCardUpdate);

        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/99", HttpMethod.PUT, request, Void.class); // The same with getForEntity and postForEntity
    
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);       

        // Verify is successfully Updated
        ResponseEntity<String> getResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        Double amount = documentContext.read("$.amount");
        assertThat(id).isEqualTo(99);
        assertThat(amount).isEqualTo(19.99);
    }

    @Test // Test that will check if cashacard to be updated does exists
    void shouldNotUpdateACashCardThatDoesNotExist(){
        CashCard unknownCashCard = new CashCard (null, 19.99, null);

        HttpEntity<CashCard> request = new HttpEntity<CashCard>(unknownCashCard);

        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class); // The same with getForEntity and postForEntity
    
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);   
    }

    @Test // Thest that will delete an existing cashcard
    @DirtiesContext
    void shouldDeleteAnExistingCashCard(){
        // Check if user exists and Delete the user sarah1
        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Check if the deleted user still exists or Check if deletion is successful
        ResponseEntity<String> getResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .getForEntity("/cashcards/99", String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteACashCardThatDoesNotExists(){
        ResponseEntity<Void> deleteResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/99999", HttpMethod.DELETE, null,Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn(){
        ResponseEntity<Void> deleteResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        ResponseEntity<String> getResponse = restTemplate
            .withBasicAuth("kumar2", "xyz789")
            .getForEntity("/cashcards/102", String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}