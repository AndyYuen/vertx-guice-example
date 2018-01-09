package com.redhat.rhoar.customer.startup;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class StartUp {

	public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        GuiceDeploymentHelper deployer = new GuiceDeploymentHelper(vertx, new JsonObject(), Binder.class);
        deployer.deployVerticles(MainVerticle.class);
        deployer.coordinateFutures();
	}

}
