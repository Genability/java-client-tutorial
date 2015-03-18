package com.genability.client.tutorial;

import java.math.BigDecimal;
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
		appId = System.getenv("APP_ID");
		appKey = System.getenv("APP_KEY");
		client = new GenabilityClient(appId, appKey);
		inputScanner = new Scanner(System.in);
	}
	
	public static void main(String[] args) {
		JavaClientTutorial tutorial = new JavaClientTutorial();
		
		tutorial.printWelcomeMessage();
		tutorial.displayMainMenu();
	}
	
	public void printWelcomeMessage() {
		System.out.println("Welcome to tutorial app for the Genability API Java Client Library. "
				+ "Please select from the following options:");
	}
	
	public void displayMainMenu() {
		Menu mainMenu = new Menu(inputScanner);
		mainMenu.addMenuItem("Enter your ZIP code", "1", new MenuAction() {
			public void run() {
				System.out.print("Enter your ZIP code: ");
				String result = inputScanner.nextLine();
				account = createAccountWithZipCode(result);
				List<Lse> lseList = getLsesByZipCode(result);
				
				displayLseMenu(lseList);
			}
		});

		mainMenu.addMenuItem("Quit", "2", new MenuAction() {
			public void run() {
				System.out.println("Goodbye!");
				inputScanner.close();
				System.exit(0);
			}
		});
		
		mainMenu.run();
	}
	
	public Account createAccountWithZipCode(String zipCode) {
		account = new Account();
		account.setAccountName("DELETE ME_" + UUID.randomUUID());
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
			return response.getResults().get(0);
		}
		
		return null;
	}
	
	public List<Lse> getLsesByZipCode(String zipCode) {
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
	
	public void displayTariffSelectionMenu() {
		AccountService service = client.getAccountService();
		GetAccountTariffsRequest request = new GetAccountTariffsRequest();
		request.setAccountId(account.getAccountId());
		Response<Tariff> response = service.getAccountTariffs(request);
		
		if (response.getStatus().equals(Response.STATUS_SUCCESS) && response.getResults().size() > 0) {
			System.out.println("Confirm your tariff:");
			Menu tariffMenu = new Menu(inputScanner);
			List<Tariff> tariffList = response.getResults();
		
			int i = 1; 
			for (Tariff t : tariffList) {
				MenuAction action = new MenuAction() {
					public void run() {
						setAccountProperty(account, "masterTariffId", t.getMasterTariffId().toString());
					}
				};
				tariffMenu.addMenuItem(t.getTariffCode(), new Integer(i).toString(), action);
				
				i++;
			}
			
			tariffMenu.run();
			displayEnergyConsumptionPrompt();
		} else {		
			System.out.println("There was an error gathering tariffs for this account. Exiting...");
			deleteAccount(account);
		}
	}
	
	public void displayLseMenu(List<Lse> lseList) {
		if (lseList == null || lseList.size() == 0) {
			// nothing else we can do here -- probably not in coverage area. start over
			System.out.println("There was an error looking up your utility. Exiting...");
			deleteAccount(account);
			
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
			for(Lse l : lseList) {
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
	
	public void displayEnergyConsumptionPrompt() {
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
	
	public void doSavingsAnalysis(Account theAccount, int targetEnergyConsumption) {
		AccountAnalysisRequest request = new AccountAnalysisRequest();
		
		request.setAccountId(theAccount.getAccountId());
		request.setFromDateTime(DateTime.now());
		
		List<PropertyData> analysisSettings = new ArrayList<PropertyData>();
		
		// specify that we want to use the baseline usage
		PropertyData baselineSetting = new PropertyData();
		baselineSetting.setScenarios("before,after,solar");
		baselineSetting.setKeyName("baselineType");
		baselineSetting.setDataValue("TYPICAL");
		analysisSettings.add(baselineSetting);
		
		// specify the target system size
		PropertyData targetEnergyConsumptionSetting = new PropertyData();
		targetEnergyConsumptionSetting.setScenarios("before,after");
		targetEnergyConsumptionSetting.setKeyName("loadSize");
		targetEnergyConsumptionSetting.setDataValue(targetEnergyConsumption);
		targetEnergyConsumptionSetting.setUnit("kWh");
		analysisSettings.add(targetEnergyConsumptionSetting);
		
		// specify the solar offset
		PropertyData targetSolarOffsetSetting = new PropertyData();
		targetSolarOffsetSetting.setScenarios("after,solar");
		targetSolarOffsetSetting.setKeyName("solarPvLoadOffset");
		targetSolarOffsetSetting.setDataValue("80");
		analysisSettings.add(targetSolarOffsetSetting);
		
		String errorMessage = "Looks like there was an error calculating your savings. Exiting...";
		try {
			request.setPropertyInputs(analysisSettings);
			AccountAnalysisService service = client.getAccountAnalysisService();
			Response<AccountAnalysis> result = service.calculateSavingsAnalysis(request);
			
			if (result.getStatus().equals(Response.STATUS_SUCCESS)) {
				AccountAnalysis analysisResults = result.getResults().get(0);
				Map<String, BigDecimal> summary = analysisResults.getSummary();
				
				System.out.println(String.format("You could save up to $%s in the first year by going solar!",
						summary.get("netAvoidedCost")));
			} else {
				System.out.println(errorMessage);
			}			
		} catch (Exception e) {
			System.out.println(errorMessage);
		} finally {
			deleteAccount(theAccount);	
		}		
	}
	
	public Account setAccountProperty(Account theAccount, String keyName, String dataValue) {
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
	
	
	public void deleteAccount(Account theAccount) {
		DeleteAccountRequest request = new DeleteAccountRequest();
		request.setHardDelete(true);
		request.setAccountId(theAccount.getAccountId());
		
		AccountService service = client.getAccountService();
		service.deleteAccount(request);
		theAccount = null;
	}
}
