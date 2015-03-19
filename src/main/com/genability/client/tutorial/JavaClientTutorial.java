/*
 * This program is a tutorial application for the Java client library for Genability's API. It
 * goes through a simple example of calculating how much a potential customer could save by
 * installing a solar power system.
 * 
 * To learn more, visit our websites:
 * 
 * Genability: http://genability.com/
 * Genability Developer Network: http://developer.genability.com/
 * Java Client Library GitHub page: https://github.com/Genability/genability-java
 */

package com.genability.client.tutorial;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.joda.time.DateTime;

import com.genability.client.api.GenabilityClient;
import com.genability.client.api.request.AccountAnalysisRequest;
import com.genability.client.api.request.DeleteAccountRequest;
import com.genability.client.api.request.GetAccountTariffsRequest;
import com.genability.client.api.request.GetLsesRequest;
import com.genability.client.api.service.AccountAnalysisService;
import com.genability.client.api.service.AccountService;
import com.genability.client.api.service.GenabilityException;
import com.genability.client.api.service.LseService;
import com.genability.client.types.Account;
import com.genability.client.types.AccountAnalysis;
import com.genability.client.types.Address;
import com.genability.client.types.Lse;
import com.genability.client.types.PropertyData;
import com.genability.client.types.Response;
import com.genability.client.types.Tariff;

public class JavaClientTutorial {

	private String appId;
	private String appKey;
	private GenabilityClient client;
	private Account account;
	private Scanner inputScanner;
	
	public JavaClientTutorial() {
		/*
		 * Make sure to create a Genability Application before running this program. You'll also
		 * need to set the APP_ID and APP_KEY environment variables. The GenabilityClient class is
		 * the basis for all of the services provided by this library.
		 */
		appId = System.getenv("APP_ID");
		appKey = System.getenv("APP_KEY");
		
		// Without proper credentials, we can't do anything. Make sure that they're set before continuing.
		if (appId == null || appKey == null || appId.isEmpty() || appKey.isEmpty()) {
			System.out.println("No credentials available. Make sure to set your APP_ID and APP_KEY environment"
					+ " variables before running this program. See http://developer.genability.com/documentation/"
					+ "quickstart/ for more information.");
			
			System.exit(1);
		}
		
		client = new GenabilityClient(appId, appKey);
		inputScanner = new Scanner(System.in);
		
		// Handle Ctrl-C. We want to make sure that we always clean up the accounts that are
		// created by this program.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Cleaning up before exiting.");
				cleanup();
			}
		});
	}
	
	public static void main(String[] args) {
		JavaClientTutorial tutorial = new JavaClientTutorial();
		
		/*
		 * The flow of this tutorial application is as follows:
		 * 1. Get the user's ZIP code.
		 * 2. Based on that ZIP code, get the local utilities. If there's only one, just use that one. Otherwise,
		 * 	  let the user pick from a list.
		 * 3. Based on that utility, let the user pick their tariff.
		 * 4. Have the user enter their annual energy consumption.
		 * 5. Finally, do the savings analysis, show the results, and exit.
		 */
		tutorial.printWelcomeMessage();
		try {
			tutorial.displayZipCodePrompt();
		} catch(GenabilityException e) {
			if (e.getMessage().contains("401")) {
				System.out.println("Invalid credentials. Exiting.");
			}
			else {
				System.out.println("An error occurred. Exiting.");
			}
		} finally {
			tutorial.cleanup();
		}
		
	}
	
	/*
	 * When the user enters their ZIP code, we want to then associate them with a particular LSE. Most of the time
	 * there will only be one, but sometimes there will be many to choose from. As with all endpoints,
	 * making a request to the LSE endpoint is a three step process -- create the request, add your parameters, and 
	 * then send it to the service. In this case, we'll be using a GetLsesRequest and send it to the LseService. Note that
	 * we always want to get the service instance from the client that we created earlier. That way the
	 * APP_ID and APP_KEY parameters will be passed along without having to do any extra work.
	 */
	private List<Lse> getLsesByZipCode(String zipCode) {
		LseService service = client.getLseService();
		GetLsesRequest request = new GetLsesRequest();

		String[] serviceTypes = {"ELECTRICITY"};
		request.setZipCode(zipCode);
		request.setResidentialServiceTypes(serviceTypes);
		
		Response<Lse> response = service.getLses(request);
		
		if (response.getStatus().equals(Response.STATUS_SUCCESS)) {
			return response.getResults();
		} else {
			return null;
		}
	}
	
	private void displayTariffSelectionMenu() {
		AccountService service = client.getAccountService();
		GetAccountTariffsRequest request = new GetAccountTariffsRequest();
		request.setAccountId(account.getAccountId());
		Response<Tariff> response = service.getAccountTariffs(request);
		
		// Most of the time, there will be many tariffs to choose from. By default, the Genability API will guess
		// the most likely tariff based off of the number of customers. Sometimes, though, that won't be correct,
		// so it's a good idea to let people confirm the tariff that they're actually on.
		//
		// One more detail: many utilities have "baseline regions" that affect how a customer is billed for their
		// energy usage. When you assign an address to an account, the Genability API will handle that for you
		// automatically.
		if (response.getStatus().equals(Response.STATUS_SUCCESS) && response.getResults().size() > 0) {
			System.out.println("Confirm your tariff:");
			Menu tariffMenu = new Menu(inputScanner);
			List<Tariff> tariffList = response.getResults();
		
			int i = 1; 
			for (final Tariff t : tariffList) {
				MenuAction action = new MenuAction() {
					public void run() {
						setAccountProperty(account, "masterTariffId", t.getMasterTariffId().toString());
					}
				};
				
				String tariffString = t.getTariffCode() + " - " + t.getTariffName();
				tariffMenu.addMenuItem(tariffString, new Integer(i).toString(), action);
				
				i++;
			}
			
			tariffMenu.run();
			displayEnergyConsumptionPrompt();
		} else {		
			System.out.println("There was an error gathering tariffs for this account. Exiting...");
			cleanup();
		}
	}

	/*
	 * To create an Account, we use the "Accounts" endpoint. In the client library, this is a three-step
	 * process: create a request, add your parameters to that request, and then send that request to
	 * the correct service class. For the Accounts, endpoint, you'll use the AccountService class.
	 */
	private Account createAccountWithZipCode(String zipCode) {
		account = new Account();

		// We have to create an account in order to do a savings analysis. We're going to delete it when
		// we're done, so there's no reason to give it an interesting name.
		account.setAccountName("Genability Java Client Tutorial_" + UUID.randomUUID());
		
		// Some account object properties are set on the class itself. Most of them are set in its list of
		// properties. See the documentation on accounts for more information.
		//
		// Accounts on the GDN: http://developer.genability.com/documentation/api-reference/account-api/account/
		PropertyData customerClass = new PropertyData();
		customerClass.setKeyName("customerClass");
		customerClass.setDataValue(1);
		account.setProperty("customerClass", customerClass);

		Address addr = new Address();
		addr.setAddressString(zipCode);
		account.setAddress(addr);
		
		AccountService service = client.getAccountService();
		Response<Account> response = service.addAccount(account);
		if (response.getStatus().equals(Response.STATUS_SUCCESS)) {
			// A response always has response metadata and a list of results, even if there's only one
			// result (as there is in this case).
			return response.getResults().get(0);
		}
		
		return null;
	}
	
	/*
	 * This is the meat of the tutorial -- the actual savings calculation. This endpoint is extremely
	 * configurable, so you can do pretty much any kind of solar savings analysis that you want. To see
	 * all of the details and options, go to the Savings Analysis page on the GDN.
	 * 
	 * Savings Analysis: http://developer.genability.com/documentation/api-reference/switch-api/savings-analysis/
	 * 
	 * In this tutorial, we're doing a couple of things...
	 */
	private void doSavingsAnalysis(Account theAccount, int targetEnergyConsumption) {
		// First, as always, we have to create a request. In this case, the savings calculation request
		// is called an AccountAnalysisRequest.
		AccountAnalysisRequest request = new AccountAnalysisRequest();
		
		// The most important parameters of a savings analysis are the accountId and the fromDateTime. They
		// are required. Setting the account ID tells the service which utility the analysis is in, and setting the
		// fromDateTime lets it know which versions of the account's tariff to use. For example, if the
		// fromDateTime was set to 2014-01-01, then the analysis would use the rates that were active at
		// that time. You can set a toDateTime as well, but the default analysis period is one year.
		request.setAccountId(theAccount.getAccountId());
		request.setFromDateTime(DateTime.now());
		
		// Now we move onto the various settings available in the analysis. They are sent in as a list of
		// PropertyData objects.
		List<PropertyData> analysisSettings = new ArrayList<PropertyData>();
		
		// Since we only asked the user for their total usage for the year, we don't have any information about
		// what they used each month. That's pretty important for calculating savings, so we have a "baseline"
		// service can split annual usage into monthly (and further into hourly) usage based on data that
		// we've collected from across the country. For more information on baselines, see the Typical Baseline
		// page on the GDN.
		//
		// Typical Baseline: http://developer.genability.com/documentation/api-reference/tariff-api/typical-baseline/
		PropertyData baselineSetting = new PropertyData();
		
		// For each setting, you have to tell the calculator which scenarios you want that setting to apply to. There
		// are three scenarios: "before", "solar", and "after". The "before" scenario is what happens if no solar system
		// is installed. Calculations are based on the user's energy consumption only. The "solar" scenario is for the
		// energy generated by the solar power system. This scenario is used for things like calculating the cost of a PPA
		// over the life of the system. Finally, the "after" scenario is what the customer pays to the utility
		// after a solar power system is installed. The calculation is done by subtracting the energy generated by the
		// system from the energy consumed by the customer and then feeding that "net consumption" into the calculator.
		// Since we don't have detailed usage (or production) data for any scenario, we're going to use a typical baseline
		// for all three of them
		baselineSetting.setScenarios("before,after,solar");
		baselineSetting.setKeyName("baselineType");
		baselineSetting.setDataValue("TYPICAL");
		analysisSettings.add(baselineSetting);
		
		// This is where we tell the calculator to scale our modeled baseline usage to a particular target amount
		// (entered by the user). Without this setting, the calculator would assume the customer's usage to be 
		// whatever the average is for the area. Since we want to use the same energy consumption for both with and
		// without solar, we use this setting for the "before" and "after" scenarios.
		PropertyData targetEnergyConsumptionSetting = new PropertyData();
		targetEnergyConsumptionSetting.setScenarios("before,after");
		targetEnergyConsumptionSetting.setKeyName("loadSize");
		targetEnergyConsumptionSetting.setDataValue(targetEnergyConsumption);
		targetEnergyConsumptionSetting.setUnit("kWh");
		analysisSettings.add(targetEnergyConsumptionSetting);
		
		// Finally, we want the calculator to generate a typical solar profile for us, rather than have to generate
		// and upload one ourselves. This setting, solarPvLoadOffset, tells the calculator to use PVWATTS
		// to generate a profile for this location and then size it to 80% of the customer's "before" load.
		PropertyData targetSolarOffsetSetting = new PropertyData();
		targetSolarOffsetSetting.setScenarios("after,solar");
		targetSolarOffsetSetting.setKeyName("solarPvLoadOffset");
		targetSolarOffsetSetting.setDataValue("80");
		analysisSettings.add(targetSolarOffsetSetting);
		
		String errorMessage = "There was an error calculating your savings. Exiting...";
		try {
			request.setPropertyInputs(analysisSettings);
			AccountAnalysisService service = client.getAccountAnalysisService();
			Response<AccountAnalysis> result = service.calculateSavingsAnalysis(request);
			
			if (result.getStatus().equals(Response.STATUS_SUCCESS)) {
				AccountAnalysis analysisResults = result.getResults().get(0);
				Map<String, BigDecimal> summary = analysisResults.getSummary();				
				String currencyString = NumberFormat.getCurrencyInstance().format(summary.get("netAvoidedCost"));
				
				// The endpoint returns a lot of details about the results of the analysis. In this case,
				// we're only interested in the customer's first year savings. You could, however, show
				// the customer's month-by-month savings in the first year, their annual savings over
				// 20 years, or any number of other results.
				System.out.println(String.format("You could save up to %s in the first year by going solar!",
						currencyString));
			} else {
				System.out.println(errorMessage);
			}			
		} catch (Exception e) {
			System.out.println(errorMessage);
		} finally {
			cleanup();
		}		
	}
	
	/*
	 * Though we're only using a few of them here, there are many, many different properties that you can
	 * set on an account. Some of them are straightforward and broadly applicable, like "lseId", which represents 
	 * the Genability ID of the user's utility. Others, like "100HPMotorNEMAPremiumEfficiency", aren't. Most
	 * account properties relate to a particular account's eligibility (or non-eligibility) for one or
	 * more tariffs from their LSE.
	 */
	private Account setAccountProperty(Account theAccount, String keyName, String dataValue) {
		PropertyData data = new PropertyData();
		data.setKeyName(keyName);
		data.setDataValue(dataValue);
		theAccount.setProperty(data);

		AccountService service = client.getAccountService();
		Response<Account> response = service.updateAccount(theAccount);
		if (response.getStatus().equals(Response.STATUS_SUCCESS)) {
			return response.getResults().get(0);
		}
		
		return null;
	}
	
	public void cleanup() {
		deleteAccount(account);
		account = null;
	}

	// Since we don't want to save any of the data from this tutorial app, we're going to delete
	// the account at the end (and if there's an error).
	private void deleteAccount(Account theAccount) {
		if (theAccount != null && theAccount.getAccountId() != null) {
			DeleteAccountRequest request = new DeleteAccountRequest();

			// A hard delete removes the object from the database completely. You don't normally want to do this.
			// The default behavior is a "soft" delete that sets the "deleted" flag on the account to "true".
			request.setHardDelete(true);
			request.setAccountId(theAccount.getAccountId());

			AccountService service = client.getAccountService();
			service.deleteAccount(request);
		}
	}
	
	public void printWelcomeMessage() {
		System.out.println("Welcome to the tutorial app for the Genability API Java Client Library.");
	}
	
	private void displayZipCodePrompt() {
		System.out.print("Enter your ZIP code: ");
		String result = inputScanner.nextLine();

		// In order to do our savings analysis, we need to create an account -- you cannot do a savings analysis
		// without one.
		account = createAccountWithZipCode(result);
		List<Lse> lseList = getLsesByZipCode(result);

		displayLseMenu(lseList);
	}
	
	private void displayLseMenu(List<Lse> lseList) {
		if (lseList == null || lseList.size() == 0) {
			// nothing else we can do here -- probably not in coverage area. start over
			System.out.println("There was an error looking up your utility. Exiting...");
			cleanup();
			
			return;
		}
		if (lseList.size() == 1) {
			Lse theLse = lseList.get(0);
			System.out.println(String.format("Looks like you're in %s territory", theLse.getName()));
			setAccountProperty(account, "lseId", theLse.getLseId().toString());
		} else {
			System.out.println("Select your utility:");
			Menu lseMenu = new Menu(inputScanner);

			int i = 1;
			for (final Lse l : lseList) {
				MenuAction action = new MenuAction() {
					public void run() {
						account = setAccountProperty(account, "lseId", l.getLseId().toString());
					}
				};
				lseMenu.addMenuItem(l.getName(), new Integer(i).toString(), action);
				
				i++;
			}
			
			lseMenu.run();
		}
		
		displayTariffSelectionMenu();
	}
	
	private void displayEnergyConsumptionPrompt() {
		Integer annualEnergyConsumption = null;
		
		while (annualEnergyConsumption == null) {
			try {
				System.out.print("Enter your annual energy usage (kWh): ");
				annualEnergyConsumption = inputScanner.nextInt();
			} catch(InputMismatchException e) {
				// do nothing, since we just want an integer input
			}
		}
		
		doSavingsAnalysis(account, annualEnergyConsumption);
	}
	
}
