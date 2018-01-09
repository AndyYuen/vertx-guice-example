package com.redhat.rhoar.customer.model;

import java.io.Serializable;

import io.vertx.core.json.JsonObject;

public class Customer implements Serializable {

    private static final long serialVersionUID = -6994655395272795260L;
    

	private String customerId;
    private String vipStatus;
    private Integer balance;

    // This is required if you want to use Customer as a Data Object on the EventBus
    public Customer() {
        
    }
    
    //-----
    // Add a constructor which takes a JSON object as parameter. 
    // The JSON representation of the Customer class is:
    // 
    //  {
    //    "customerId"	: "A123",
    //    "vipStatus"	: "GOld",
    //    "balance"		: 1200
    //  }
    //
    //-----
    public Customer(JsonObject jsonObject) {
    	customerId = jsonObject.getString("customerId");
    	vipStatus = jsonObject.getString("vipStatus");
    	balance = jsonObject.getInteger("balance");
    }
    

    
    //-----
    // Implement the toJson method which returns a JsonObject representing this instance. 
    // The JSON representation of the Customer class is:
    // 
    //  {
    //    "customerId"	: "A123",
    //    "vipStatus"	: "GOld",
    //    "balance"		: 1200
    //  }
    //
    //-----
    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        json.put("customerId", customerId);
        json.put("vipStatus", vipStatus);
        json.put("balance", balance);
        return json;
    }
    
    public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getVipStatus() {
		return vipStatus;
	}

	public void setVipStatus(String vipStatus) {
		this.vipStatus = vipStatus;
	}

	public Integer getBalance() {
		return balance;
	}

	public void setBalance(Integer balance) {
		this.balance = balance;
	}



}
