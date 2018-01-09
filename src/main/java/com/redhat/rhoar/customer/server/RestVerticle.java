package com.redhat.rhoar.customer.server;

import java.util.List;

import com.google.inject.Inject;
import com.redhat.rhoar.customer.model.Customer;
import com.redhat.rhoar.customer.service.CustomerService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class RestVerticle extends AbstractVerticle {


    private CustomerService customerService;

	@Inject
    public RestVerticle(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        Router router = Router.router(vertx);
        //----
        // Add routes to the Router
        // * A route for HTTP GET requests that matches the "/customers" path. 
        //   The handler for this route is implemented by the getCustomers() method.
        // * A route for HTTP GET requests that matches the /customer/:customerId path.
        //   The handler for this route is implemented by the getCustomer() method.
        // * A route for the path "/customer" to which a BodyHandler is attached.
        // * A route for HTTP POST requests that matches the "/customer" path. 
        //   The handler for this route is implemented by the addCustomer() method.
        //----
        router.get("/customers").handler(this::getCustomers);
        router.get("/customer/:customerId").handler(this::getCustomer);
        router.route("/customer").handler(BodyHandler.create());
        router.post("/customer").handler(this::addCustomer);

        //Health Checks
        router.get("/health/readiness").handler(rc -> rc.response().end("OK"));
        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx)
                .register("health", f -> health(f));
        router.get("/health/liveness").handler(healthCheckHandler);

        //----
        // Create a HTTP server.
        // * Use the Router as request handler
        // * Use the verticle configuration to obtain the port to listen to. 
        //   Get the configuration from the config() method of AbstractVerticle.
        //   Look for the key "customer.http.port", which returns an Integer. 
        //   The default value (if the key is not set in the configuration) is 8080.
        // * If the HTTP server is correctly instantiated, complete the Future. If there is a failure, fail the Future. 
        //----
        vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("customer.http.port", 8080), result -> {
            if (result.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(result.cause());
            }
        });
    }

    private void getCustomers(RoutingContext rc) {
        //----
        // In the implementation:
        // * Call the getCustomers() method of the CustomerService.
        // * In the handler, transform the List<Customer> response to a JsonArray object.
        // * Put a "Content-type: application/json" header on the HttpServerResponse object. 
        // * Write the JsonArray to the HttpServerResponse, and end the response.
        // * If the getCustomers() method returns a failure, fail the RoutingContext.
        //----
        customerService.getCustomers(ar -> {
            if (ar.succeeded()) {
                List<Customer> customers = ar.result();
                JsonArray json = new JsonArray();
                customers.stream()
                    .map(p -> p.toJson())
                    .forEach(p -> json.add(p));
                rc.response()
                    .putHeader("Content-type", "application/json")
                    .end(json.encodePrettily());
            } else {
                rc.fail(ar.cause());
            }
        });
    }

    private void getCustomer(RoutingContext rc) {
        //----
        // In the implementation:
        // * Call the getCustomer() method of the CustomerService.
        // * In the handler, transform the Customer response to a JsonObject object.
        // * Put a "Content-type: application/json" header on the HttpServerResponse object. 
        // * Write the JsonObject to the HttpServerResponse, and end the response.
        // * If the getCustomer() method of the CustomerService returns null,  fail the RoutingContext with a 404 HTTP status code 
        // * If the getCustomer() method returns a failure, fail the RoutingContext.
        //----
        String customerId = rc.request().getParam("customerId");
        customerService.getCustomer(customerId, ar -> {
            if (ar.succeeded()) {
                Customer customer = ar.result();
                JsonObject json;
                if (customer != null) {
                    json = customer.toJson();
                    rc.response()
                        .putHeader("Content-type", "application/json")
                        .end(json.encodePrettily());
                } else {
                    rc.fail(404);
                }
            } else {
                rc.fail(ar.cause());
            }
        });
    }

    private void addCustomer(RoutingContext rc) {
        //----
        // In the implementation:
        // * Obtain the body contents from the RoutingContext. Expect the body to be JSON.
        // * Transform the JSON payload to a Customer object.
        // * Call the addCustomer() method of the CustomerService. 
        // * If the call succeeds, set a HTTP status code 201 on the HttpServerResponse, and end the response. 
        // * If the call fails, fail the RoutingContext.
        //----
        JsonObject json = rc.getBodyAsJson();
        customerService.addCustomer(new Customer(json), ar -> {
            if (ar.succeeded()) {
                rc.response().setStatusCode(201).end();
            } else {
                rc.fail(ar.cause());
            }
        });

    }
    
    private void health(Future<Status> future) {
        customerService.ping(ar -> {
            if (ar.succeeded()) {
                // HealthCheckHandler has a timeout of 1000s. If timeout is exceeded, the future will be failed
                if (!future.isComplete()) {
                    future.complete(Status.OK());
                }
            } else {
                if (!future.isComplete()) {
                    future.complete(Status.KO());
                }
            }
        });
    }
}
