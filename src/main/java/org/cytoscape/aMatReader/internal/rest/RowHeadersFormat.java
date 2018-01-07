package org.cytoscape.aMatReader.internal.rest;

public enum RowHeadersFormat {
	//AUTO("Automatic"),
	NAMES("First column contains node names"),
	IGNORE("Skip first column"),
	NONE("No header column");

	String title;

	RowHeadersFormat(String title) {
		this.title = title;
	}
	
	public String toString() { return title; }

}
