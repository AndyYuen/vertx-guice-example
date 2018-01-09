package com.redhat.rhoar.customer.verticle.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.allOf;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.redhat.rhoar.customer.model.Customer;
import com.redhat.rhoar.customer.service.CustomerService;
import com.redhat.rhoar.customer.service.CustomerServiceMongoImpl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class CustomerServiceTest extends MongoTestBase {

    private Vertx vertx;
    static final String COLLECTION = "customers";

    @Before
    public void setup(TestContext context) throws Exception {
        vertx = Vertx.vertx();
        vertx.exceptionHandler(context.exceptionHandler());
        JsonObject config = getConfig();
        mongoClient = MongoClient.createNonShared(vertx, config);
        Async async = context.async();
        dropCollection(mongoClient, COLLECTION, async, context);
        async.await(10000);
    }

    @After
    public void tearDown() throws Exception {
        mongoClient.close();
        vertx.close();
    }

    @Test
    public void testAddCustomer(TestContext context) throws Exception {
        String customerId = "A10";
        String vipStatus = "Gold";
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setVipStatus(vipStatus);
        customer.setBalance(100);

        CustomerService service = new CustomerServiceMongoImpl(mongoClient);

        Async async = context.async();

        service.addCustomer(customer, ar -> {
            if (ar.failed()) {
                context.fail(ar.cause().getMessage());
            } else {
                JsonObject query = new JsonObject().put("_id", customerId);
                mongoClient.findOne(COLLECTION, query, null, ar1 -> {
                    if (ar1.failed()) {
                        context.fail(ar1.cause().getMessage());
                    } else {
                        assertThat(ar1.result().getString("vipStatus"), equalTo(vipStatus));
                        async.complete();
                    }
                });
            }
        });
    }

    @Test
    public void testGetCustomers(TestContext context) throws Exception {
        // ----
        // 
        // Insert two or more customers in MongoDB Using the MongoClient.save method.
        // Retrieve the customers from Mongo using the testGetCusomters method.
        // Verify that no failures happened, 
        //   that the number of customers retrieved corresponds to the number inserted, 
        //   and that the customer values match what was inserted.
        // 
        // ----
        Async saveAsync = context.async(2);
        String customerId1 = "A11";
        JsonObject json1 = new JsonObject()
                .put("customerId", customerId1)
                .put("vipStatus", "Diamond")
                .put("balance", new Integer(1000));

        mongoClient.save(COLLECTION, json1, ar -> {
            if (ar.failed()) {
                context.fail();
            }
            saveAsync.countDown();
        });

        String customerId2 = "A12";
        JsonObject json2 = new JsonObject()
                .put("customerId", customerId2)
                .put("vipStatus", "Silver")
                .put("balance", new Integer(1000));

        mongoClient.save(COLLECTION, json2, ar -> {
            if (ar.failed()) {
                context.fail();
            }
            saveAsync.countDown();
        });

        saveAsync.await();

        CustomerService service = new CustomerServiceMongoImpl(mongoClient);

        Async async = context.async();

        service.getCustomers(ar -> {
            if (ar.failed()) {
                context.fail(ar.cause().getMessage());
            } else {
                assertThat(ar.result(), notNullValue());
                assertThat(ar.result().size(), equalTo(2));
                Set<String> itemIds = ar.result().stream().map(p -> p.getCustomerId()).collect(Collectors.toSet());
                assertThat(itemIds.size(), equalTo(2));
                assertThat(itemIds, allOf(hasItem(customerId1),hasItem(customerId2)));
                async.complete();
            }
        });
    }

    @Test
    public void testGetCustomer(TestContext context) throws Exception {
        // ----
        // test retrieving a customer by customerId
        // 
        // ----
        Async saveAsync = context.async(2);
        String customerId1 = "A11";
        JsonObject json1 = new JsonObject()
                .put("customerId", customerId1)
                .put("vipStatus", "Diamond")
                .put("balance", new Integer(1000));

        mongoClient.save(COLLECTION, json1, ar -> {
            if (ar.failed()) {
                context.fail();
            }
            saveAsync.countDown();
        });

        String customerId2 = "A12";
        JsonObject json2 = new JsonObject()
                .put("customerId", customerId2)
                .put("vipStatus", "Bronze")
                .put("balance", new Integer(1000));

        mongoClient.save(COLLECTION, json2, ar -> {
            if (ar.failed()) {
                context.fail();
            }
            saveAsync.countDown();
        });

        saveAsync.await();

        CustomerService service = new CustomerServiceMongoImpl(mongoClient);

        Async async = context.async();

        service.getCustomer("A11", ar -> {
            if (ar.failed()) {
                context.fail(ar.cause().getMessage());
            } else {
                assertThat(ar.result(), notNullValue());
                assertThat(ar.result().getCustomerId(), equalTo("A11"));
                assertThat(ar.result().getVipStatus(), equalTo("Diamond"));
                async.complete();
            }
        });
    }

    @Test
    public void testGetNonExistingCustomer(TestContext context) throws Exception {
        // ----
        // test non-existent customer
        // 
        // ----
        Async saveAsync = context.async(1);
        String customerId1 = "A11";
        JsonObject json1 = new JsonObject()
                .put("itemId", customerId1)
                .put("vipStatus", "Diamond")
                .put("balance", new Integer(1000));

        mongoClient.save(COLLECTION, json1, ar -> {
            if (ar.failed()) {
                context.fail();
            }
            saveAsync.countDown();
        });

        saveAsync.await();

        CustomerService service = new CustomerServiceMongoImpl(mongoClient);

        Async async = context.async();

        service.getCustomer("A12", ar -> {
            if (ar.failed()) {
                context.fail(ar.cause().getMessage());
            } else {
                assertThat(ar.result(), nullValue());
                async.complete();
            }
        });
    }

    //@Test
    public void testPing(TestContext context) throws Exception {
        CustomerService service = new CustomerServiceMongoImpl(mongoClient);
        
        Async async = context.async();
        service.ping(ar -> {
            assertThat(ar.succeeded(), equalTo(true));
            async.complete();
        });
    }

}
