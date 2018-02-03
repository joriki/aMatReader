package org.cytoscape.aMatReader.internal.util;

public enum RowNameState {
	NAMES("First column contains node names"),
	IGNORE("Skip first column"),
	NONE("No header column");

	String title;

	RowNameState(String title) {
		this.title = title;
	}
	
	public String toString() { return title; }

}
