package com.genability.client.tutorial;

import com.genability.client.api.GenabilityClient;
import com.genability.client.api.request.GetAccountRequest;
import com.genability.client.api.service.AccountService;
import com.genability.client.types.Account;
import com.genability.client.types.Response;

public class GenabilitySwitchApiTutorial {

	private static String appId = System.getenv("APP_ID");
	private static String appKey = System.getenv("APP_KEY");
	
	public static void main(String[] args) {
		System.out.println("Hello, world!");
		System.out.printf("Got appId: %s\n", appId);
		System.out.printf("Got appKey: %s\n", appKey);
	}
}
