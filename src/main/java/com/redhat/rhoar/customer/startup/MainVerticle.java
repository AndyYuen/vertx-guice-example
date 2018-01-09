package com.redhat.rhoar.customer.startup;



import com.redhat.rhoar.customer.server.RestVerticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;


public class MainVerticle extends AbstractVerticle {

	
    @Override
    public void start(Future<Void> startFuture) throws Exception {
    	
    	//----
        //
        // * Create a ConfigStoreOptions instance.
        // * Set the type to "configmap" and the format to "yaml".
        // * Configure the ConfigStoreOptions instance with the name and the key of the configmap
        // * Create a ConfigRetrieverOptions instance
        // * Add the ConfigStoreOptions instance as store to the ConfigRetrieverOptions instance
        // * Create a ConfigRetriever instance with the ConfigRetrieverOptions instance
        // * Use the ConfigRetriever instance to retrieve the configuration
        // * If the retrieval was successful, call the deployVerticles method, otherwise fail the startFuture object.
        //
        //----

        ConfigStoreOptions configStore = new ConfigStoreOptions()
            .setType("configmap")
            .setFormat("yaml")
            .setConfig(new JsonObject()
                .put("name", "app-config")
                .put("key", "app-config.yaml"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        if (System.getenv("KUBERNETES_NAMESPACE") != null) {
            //we're running in Kubernetes
            options.addStore(configStore);

	        ConfigRetriever.create(vertx, options)
	            .getConfig(ar -> {
	                if (ar.succeeded()) {
	                	System.out.println("Successfully retrieved the configuration.");
	                	AppConfig.getInstance(vertx).setConfig(ar.result());
	                	deploy(ar.result(), startFuture);
	                } else {
	                    System.out.println("Failed to retrieve the configuration: " + ar.cause());
	                }
	            });
        }
        else {
        	System.out.println("Failed: Not running on Openshift.");
        	startFuture.fail("Not running on Openshift.");
        }
    }
    
    private void deploy(JsonObject config, Future<Void> startFuture) {

        GuiceDeploymentHelper deployer = new GuiceDeploymentHelper(vertx, config, Binder.class);
        deployer.deployVerticles(RestVerticle.class);
        deployer.coordinateFutures(startFuture);
    }


    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }

}
