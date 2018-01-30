package org.cytoscape.aMatReader.internal.rest;

import javax.ws.rs.core.Response;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class AMatReaderTaskObserver implements TaskObserver {

	/**
	 * 
	 */
	private final AMatReaderResourceImpl aMatReaderResource;
	CIResponse<?> response;
	AMatReaderResponse aMatResponse;

	public CIResponse<?> getResponse() {
		return response;
	}

	private String resourcePath;
	private String errorCode;

	public AMatReaderTaskObserver(AMatReaderResourceImpl aMatReaderResource, String resourcePath, String errorCode) {
		this.aMatReaderResource = aMatReaderResource;
		aMatResponse = new AMatReaderResponse(null, 0, 0);
		this.resourcePath = resourcePath;
		this.errorCode = errorCode;
	}
	

	public void allFinished(FinishStatus arg0) {

		if (arg0.getType() == FinishStatus.Type.SUCCEEDED || arg0.getType() == FinishStatus.Type.CANCELLED) {
			response = aMatResponse;
			aMatResponse = new AMatReaderResponse(null, 0, 0);
		} else {
			response = this.aMatReaderResource.buildCIErrorResponse(
					Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resourcePath, errorCode,
					arg0.getException().getMessage(), arg0.getException());
		}

	}

	public void taskFinished(ObservableTask arg0) {
		AMatReaderResponse resp = arg0.getResults(AMatReaderResponse.class);
		aMatResponse.update(resp);
	}


	public void printStatus() {
		System.out.printf("Current state: %d new, %d updated, net: %s", aMatResponse.data.newEdges, aMatResponse.data.updatedEdges, aMatResponse.data.suid);
	}
}
