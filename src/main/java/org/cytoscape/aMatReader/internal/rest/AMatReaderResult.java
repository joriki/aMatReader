package org.cytoscape.aMatReader.internal.rest;

public class AMatReaderResult {
	int newEdges;
	int updatedEdges;
	long suid;

	public AMatReaderResult(long suid, int newEdges, int updatedEdges) {
		this.suid = suid;
		this.newEdges = newEdges;
		this.updatedEdges = updatedEdges;
	}
}
