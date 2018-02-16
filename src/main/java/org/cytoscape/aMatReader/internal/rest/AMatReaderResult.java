package org.cytoscape.aMatReader.internal.rest;

public class AMatReaderResult {
	public int newEdges;
	public int updatedEdges;
	public Long suid;

	public AMatReaderResult(Long suid, int newEdges, int updatedEdges) {
		this.suid = suid;
		this.newEdges = newEdges;
		this.updatedEdges = updatedEdges;
	}
}
