package org.cytoscape.aMatReader.internal.rest;

public enum Delimiter {
	TAB("<tab>","\t"),
	COMMA(",",","),
	BAR("|","\\|"),
	SPACE("<space>"," ");

	String title;
	String delimiter;

	Delimiter(String title, String delimiter) {
		this.title = title;
		this.delimiter = delimiter;
	}
	
	public String toString() { return title; }
	public String getDelimiter() { return delimiter; }
}
