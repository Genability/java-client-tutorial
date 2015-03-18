package com.genability.client.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Menu implements MenuAction {
	private List<String> menuItems;
	private Map<String, MenuAction> menuActions;
	private int indentLevel = 1;
	private String promptString = "Select an option: ";
	private Scanner inputScanner;
	
	public Menu(Scanner inputScanner) {
		this.inputScanner = inputScanner;
		menuItems = new ArrayList<String>();
		menuActions = new HashMap<String, MenuAction>();
	}
	
	public Menu(Scanner inputScanner, String promptString) {
		this(inputScanner);
		this.promptString = promptString;
	}
	
	public void addMenuItem(String item, String actionKey, MenuAction action) {
		menuItems.add(item);
		menuActions.put(actionKey, action);
	}
	
	@Override
	public String toString() {
		StringBuilder menu = new StringBuilder();
		menu.append("\n");
		
		int item = 1;
		for(String menuItem : menuItems) {
			for(int i = 0; i < indentLevel; i++) {
				menu.append("\t");
			}
			menu.append(String.format("%d. ", item));
			menu.append(menuItem);
			menu.append("\n");
			
			item++;
		}
		
		menu.append("\n");
		menu.append(promptString);
		
		return menu.toString();		
	}

	public int getIndentLevel() {
		return indentLevel;
	}

	public void setIndentLevel(int indentLevel) {
		this.indentLevel = indentLevel;
	}

	public String getPromptString() {
		return promptString;
	}

	public void setPromptString(String promptString) {
		this.promptString = promptString;
	}
	
	public void run() {
		MenuAction theAction = null;
		
		while (theAction == null) {
			System.out.print(this);
			String result = inputScanner.nextLine();
			theAction = menuActions.get(result);
		}

		theAction.run();
	}
}