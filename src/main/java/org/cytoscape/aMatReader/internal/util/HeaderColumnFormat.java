package org.cytoscape.aMatReader.internal.util;

public enum HeaderColumnFormat {
	NAMES("First column contains node names"),
	IGNORE("Skip first column"),
	NONE("No header column");

	String title;

	HeaderColumnFormat(String title) {
		this.title = title;
	}
	
	public String toString() { return title; }

}
