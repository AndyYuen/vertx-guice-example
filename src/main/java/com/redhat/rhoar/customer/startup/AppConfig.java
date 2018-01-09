package com.redhat.rhoar.customer.startup;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class AppConfig {
	
	private static AppConfig appConfigInstance = null;
	
	public static AppConfig getInstance(Vertx vertx) {
		if (appConfigInstance == null) {
			appConfigInstance = new AppConfig();
		}
		return appConfigInstance;
	}
	
	private JsonObject config;
	
	protected AppConfig() {

		config = null;
	}
	
	public JsonObject getConfig() {
		return config;
	}
	
	public void setConfig(JsonObject config) {
		this.config = config;
	}


}
