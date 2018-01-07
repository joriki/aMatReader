package org.cytoscape.aMatReader.internal.rest;

public enum ColumnHeadersFormat {
	//AUTO("Automatic"),
	NAMES("First row contains node names"),
	IGNORE("Skip first row"),
	NONE("No header row");

	String title;

	ColumnHeadersFormat(String title) {
		this.title = title;
	}
	
	public String toString() { return title; }
}
