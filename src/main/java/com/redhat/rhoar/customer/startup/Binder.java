package com.redhat.rhoar.customer.startup;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.redhat.rhoar.customer.service.CustomerService;
import com.redhat.rhoar.customer.service.CustomerServiceMongoImpl;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class Binder extends AbstractModule {


	@Provides @Singleton
    public MongoClient provideMongoClient(Vertx vertx, JsonObject config){
		System.out.println("Calling provideMongoClient...");
        return MongoClient.createShared(vertx, AppConfig.getInstance(vertx).getConfig());
    }
	
	@Provides @Singleton
    public CustomerService provideCustomerService(MongoClient client){
		System.out.println("Calling provideCustomerService...");
        return new CustomerServiceMongoImpl(client);
    }
	
	@Override
	protected void configure() {
		// TODO Auto-generated method stub

	}

}
