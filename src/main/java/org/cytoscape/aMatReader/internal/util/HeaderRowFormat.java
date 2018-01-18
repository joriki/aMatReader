package org.cytoscape.aMatReader.internal.util;

public enum HeaderRowFormat {
	
	NAMES("First row contains node names"),
	IGNORE("Skip first row"),
	NONE("No header row");

	String title;

	HeaderRowFormat(String title) {
		this.title = title;
	}
	
	public String toString() { return title; }
}
