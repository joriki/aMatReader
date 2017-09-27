package org.cytoscape.aMatReader.internal.rest;

public enum Delimiters {
	TAB("<tab>","\t"),
	COMMA(",",","),
	BAR("|","|"),
	SPACE("<space>"," ");

	String title;
	String delimiter;

	Delimiters(String title, String delimiter) {
		this.title = title;
		this.delimiter = delimiter;
	}
	
	public String toString() { return title; }
	public String getDelimiter() { return delimiter; }
}
