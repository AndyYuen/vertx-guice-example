package com.redhat.rhoar.customer.startup;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class GuiceDeploymentHelper {
	
	private List<Future> futureList = new ArrayList<Future>();
	private Vertx vertx;
	private DeploymentOptions options;
	
	public GuiceDeploymentHelper(Vertx vertx, JsonObject config, Class binder) {
		this.vertx = vertx;
        config.put("guice_binder", binder.getName());
        options = new DeploymentOptions();
        options.setConfig(config);
	}

	public void deployVerticles(Class verticle) {
	        Future<String> future = Future.future();
	        futureList.add(future);
	        String deploymentName = "java-guice:" + verticle.getName();

	        vertx.deployVerticle(deploymentName, options, future.completer());
	    }
	
	public void coordinateFutures(Future<Void> startFuture) {
		if (futureList.size() == 0) {
			System.out.println("No Verticle deployment to do...");
			return;
		}
		CompositeFuture.all(futureList).setHandler(ar -> {

            if (ar.succeeded()) {
                System.out.println("Verticles deployed successfully.");
                if (startFuture != null) {
                	startFuture.complete();
                }
            } else {
                System.out.println("WARNINIG: Verticles NOT deployed successfully: " + ar.cause());
                if (startFuture != null) {
                	startFuture.fail(ar.cause());
                }
            }
        });
	}
	
	public void coordinateFutures() {
		coordinateFutures(null);
	}

}
