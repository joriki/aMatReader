package org.cytoscape.aMatReader.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.cytoscape.aMatReader.internal.tasks.AMatReaderParameters;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

public class AMatReaderTaskObserver implements TaskObserver {

	/**
	 * 
	 */
	private final AMatReaderResource copyLayoutResource;
	CIResponse<?> response;

	public CIResponse<?> getResponse() {
		return response;
	}

	private AMatReaderParameters result;
	private String resourcePath;
	private String errorCode;

	public AMatReaderTaskObserver(AMatReaderResource copyLayoutResource, String resourcePath, String errorCode) {
		this.copyLayoutResource = copyLayoutResource;
		response = null;
		this.resourcePath = resourcePath;
		this.errorCode = errorCode;
	}

	@SuppressWarnings("unchecked")
	public void allFinished(FinishStatus arg0) {
		if (arg0.getType() == FinishStatus.Type.SUCCEEDED || arg0.getType() == FinishStatus.Type.CANCELLED) {
			response = new CIResponse<AMatReaderParameters>();
			
			((CIResponse<AMatReaderParameters>) response).data = result;
			response.errors = new ArrayList<CIError>();
		} else {
			response = this.copyLayoutResource.buildCIErrorResponse(
					Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resourcePath, errorCode,
					arg0.getException().getMessage(), arg0.getException());
		}
		
	}

	
	public void taskFinished(ObservableTask arg0) {
		AMatReaderParameters res = arg0.getResults(AMatReaderParameters.class);
		result = res;
	}

	public int getNumNodesCreated() {
		// TODO Auto-generated method stub
		return 0;
	}
}
