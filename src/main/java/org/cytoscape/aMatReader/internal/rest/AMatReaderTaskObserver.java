package org.cytoscape.aMatReader.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.ci.model.CIError;
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

	public CIResponse<?> getResponse() {
		return response;
	}

	private AMatReaderResponse result;
	private String resourcePath;
	private String errorCode;

	public AMatReaderTaskObserver(AMatReaderResourceImpl aMatReaderResource, String resourcePath, String errorCode) {
		this.aMatReaderResource = aMatReaderResource;
		response = null;
		this.resourcePath = resourcePath;
		this.errorCode = errorCode;
	}

	@SuppressWarnings("unchecked")
	public void allFinished(FinishStatus arg0) {
		if (arg0.getType() == FinishStatus.Type.SUCCEEDED || arg0.getType() == FinishStatus.Type.CANCELLED) {
			response = new CIResponse<AMatReaderResponse>();

			((CIResponse<AMatReaderResponse>) response).data = result;
			response.errors = new ArrayList<CIError>();
		} else {
			response = this.aMatReaderResource.buildCIErrorResponse(
					Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resourcePath, errorCode,
					arg0.getException().getMessage(), arg0.getException());
		}

	}

	public void taskFinished(ObservableTask arg0) {
		result = arg0.getResults(AMatReaderResponse.class);
	}
}
