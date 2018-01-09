package com.redhat.rhoar.customer.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.redhat.rhoar.customer.model.Customer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class CustomerServiceMongoImpl implements CustomerService {


    private MongoClient client;
	
    private final String COLLECTION = "customers";

	@Inject
    public CustomerServiceMongoImpl(MongoClient client) {
        this.client = client;
    }

    @Override
    public void getCustomers(Handler<AsyncResult<List<Customer>>> resulthandler) {
        // ----
        // 
        // Use the `MongoClient.find()` method. 
        // Use an empty JSONObject for the query
        // The collection to search is COLLECTION
        // In the handler implementation, transform the `List<JSONObject>` to `List<Person>` - use Java8 Streams!
        // Use a Future to set the result on the handle() method of the result handler
        // Don't forget to handle failures!
        // ----
        JsonObject query = new JsonObject();
        client.find(COLLECTION, query, ar -> {
            if (ar.succeeded()) {
                List<Customer> customers = ar.result().stream()
                                           .map(json -> new Customer(json))
                                           .collect(Collectors.toList());
                resulthandler.handle(Future.succeededFuture(customers));
            } else {
                resulthandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void getCustomer(String customerId, Handler<AsyncResult<Customer>> resulthandler) {
        // ----
        // 
        // Use the `MongoClient.find()` method. 
        // Use a JSONObject for the query with the field 'customerId' set to the customer customerId
        // The collection to search is COLLECTION
        // In the handler implementation, transform the `List<JSONObject>` to `Person` - use Java8 Streams!
        // If the customer is not found, the result should be set to null
        // Use a Future to set the result on the handle() method of the result handler
        // Don't forget to handle failures!
        // ----
        JsonObject query = new JsonObject().put("customerId", customerId);
        client.find(COLLECTION, query, ar -> {
            if (ar.succeeded()) {
                Optional<JsonObject> result = ar.result().stream().findFirst();
                if (result.isPresent()) {
                    resulthandler.handle(Future.succeededFuture(new Customer(result.get())));
                } else {
                    resulthandler.handle(Future.succeededFuture(null));
                }
            } else {
                resulthandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void addCustomer(Customer customer, Handler<AsyncResult<String>> resulthandler) {
        client.save(COLLECTION, toDocument(customer), resulthandler);
    }

    @Override
    public void ping(Handler<AsyncResult<String>> resultHandler) {
        resultHandler.handle(Future.succeededFuture("OK"));
    }

    private JsonObject toDocument(Customer customer) {
        JsonObject document = customer.toJson();
        document.put("_id", customer.getCustomerId());
        return document;
    }
}
