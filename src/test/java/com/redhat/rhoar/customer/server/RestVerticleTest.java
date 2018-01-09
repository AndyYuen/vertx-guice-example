package com.redhat.rhoar.customer.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.rhoar.customer.model.Customer;
import com.redhat.rhoar.customer.server.RestVerticle;
import com.redhat.rhoar.customer.service.CustomerService;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class RestVerticleTest {

    private Vertx vertx;
    private Integer port;
    private CustomerService customerService;

    /**
     * Before executing our test, let's deploy our verticle.
     * <p/>
     * This method instantiates a new Vertx and deploy the verticle. Then, it waits for the verticle to successfully
     * complete its start sequence (context.asyncAssertSuccess).
     *
     * @param context the test context.
     */
    @Before
    public void setUp(TestContext context) throws IOException {
      vertx = Vertx.vertx();

      // Register the context exception handler
      vertx.exceptionHandler(context.exceptionHandler());

      // Let's configure the verticle to listen on the 'test' port (randomly picked).
      // We create deployment options and set the _configuration_ json object:
      ServerSocket socket = new ServerSocket(0);
      port = socket.getLocalPort();
      socket.close();

      DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("customer.http.port", port));

      //Mock the CustomerService
      customerService = mock(CustomerService.class);

      // We pass the options as the second parameter of the deployVerticle method.
      vertx.deployVerticle(new RestVerticle(customerService), options, context.asyncAssertSuccess());
    }

    /**
     * This method, called after our test, just cleanup everything by closing
     * the vert.x instance
     *
     * @param context
     *            the test context
     */
    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testGetCustomers(TestContext context) throws Exception {
        //----
        //
        // * Stub the getCustomers() method of CustomerService mock to return a List<Customer>
        // * Use the Vert.x Web client to execute a GET request to the "/customers" endpoint.
        //   Use the getNow() method of the HTTP client.
        // * Verify that the return code of the request equal to 200,
        //   and that the response has a header "Content-type: application/json".
        // * Use the BodyHandler method of the HttpClientResponse object to obtain and verify the response body.
        //
        //----
        String customerId1 = "A11";
        JsonObject json1 = new JsonObject()
                .put("customerId", customerId1)
                .put("vipStatus", "Diamond")
                .put("balance", new Integer(1000));
        String customerId2 = "A22";
        JsonObject json2 = new JsonObject()
                .put("customerId", customerId2)
                .put("vipStatus", "Bronze")
                .put("balance", new Integer(1000));
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(json1));
        customers.add(new Customer(json2));
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation){
                Handler<AsyncResult<List<Customer>>> handler = invocation.getArgument(0);
                handler.handle(Future.succeededFuture(customers));
                return null;
             }
         }).when(customerService).getCustomers(any());

        Async async = context.async();
        vertx.createHttpClient().get(port, "localhost", "/customers", response -> {
                assertThat(response.statusCode(), equalTo(200));
                assertThat(response.headers().get("Content-type"), equalTo("application/json"));
                response.bodyHandler(body -> {
                    JsonArray json = body.toJsonArray();
                    Set<String> itemIds =  json.stream()
                            .map(j -> new Customer((JsonObject)j))
                            .map(p -> p.getCustomerId())
                            .collect(Collectors.toSet());
                    assertThat(itemIds.size(), equalTo(2));
                    assertThat(itemIds, allOf(hasItem(customerId1),hasItem(customerId2)));
                    verify(customerService).getCustomers(any());
                    async.complete();
                })
                .exceptionHandler(context.exceptionHandler());
            })
            .exceptionHandler(context.exceptionHandler())
            .end();
    }


    @Test
    public void testGetCustomer(TestContext context) throws Exception {
        //----
        // Get a customer by customerId
        //
        //----
        String customerId = "A11";
        JsonObject json = new JsonObject()
                .put("customerId", customerId)
                .put("vipStatus", "Diamond")
                .put("balance", new Integer(1000));
        Customer customer = new Customer(json);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation){
                Handler<AsyncResult<Customer>> handler = invocation.getArgument(1);
                handler.handle(Future.succeededFuture(customer));
                return null;
             }
         }).when(customerService).getCustomer(eq("A11"),any());

        Async async = context.async();
        vertx.createHttpClient().get(port, "localhost", "/customer/A11", response -> {
                assertThat(response.statusCode(), equalTo(200));
                assertThat(response.headers().get("Content-type"), equalTo("application/json"));
                response.bodyHandler(body -> {
                    JsonObject result = body.toJsonObject();
                    assertThat(result, notNullValue());
                    assertThat(result.containsKey("customerId"), is(true));
                    assertThat(result.getString("customerId"), equalTo("A11"));
                    verify(customerService).getCustomer(eq("A11"),any());
                    async.complete();
                })
                .exceptionHandler(context.exceptionHandler());
            })
            .exceptionHandler(context.exceptionHandler())
            .end();
    }

    @Test
    public void testGetNonExistingCustomer(TestContext context) throws Exception {
        //----
        // test a non-existent customer
        //
        //----
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation){
                Handler<AsyncResult<Customer>> handler = invocation.getArgument(1);
                handler.handle(Future.succeededFuture(null));
                return null;
             }
         }).when(customerService).getCustomer(eq("A99"),any());

        Async async = context.async();
        vertx.createHttpClient().get(port, "localhost", "/customer/A99", response -> {
                assertThat(response.statusCode(), equalTo(404));
                async.complete();
            })
            .exceptionHandler(context.exceptionHandler())
            .end();
    }

    @Test
    public void testAddCustomer(TestContext context) throws Exception {
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation){
                Handler<AsyncResult<String>> handler = invocation.getArgument(1);
                handler.handle(Future.succeededFuture(null));
                return null;
             }
         }).when(customerService).addCustomer(any(),any());

        Async async = context.async();
        String customerId = "A11";
        JsonObject json = new JsonObject()
                .put("customerId", customerId)
                .put("vipStatus", "Diamond")
                .put("balance", new Integer(1000));
        String body = json.encodePrettily();
        String length = Integer.toString(body.length());
        vertx.createHttpClient().post(port, "localhost", "/customer")
            .exceptionHandler(context.exceptionHandler())
            .putHeader("Content-type", "application/json")
            .putHeader("Content-length", length)
            .handler(response -> {
                assertThat(response.statusCode(), equalTo(201));
                ArgumentCaptor<Customer> argument = ArgumentCaptor.forClass(Customer.class);
                verify(customerService).addCustomer(argument.capture(), any());
                assertThat(argument.getValue().getCustomerId(), equalTo(customerId));
                async.complete();
            })
            .write(body)
            .end();
    }

}
