package org.cytoscape.aMatReader.internal.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.aMatReader.internal.tasks.AMatReaderParameters;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderTaskFactory;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = { "Apps: aMatReader" })
@Path("/aMatReader/v1/")
public class AMatReaderResource {

	public static final String TASK_EXECUTION_ERROR_CODE = "3";

	private final AMatReaderTaskFactory aMatReaderTaskFactory;
	private final SynchronousTaskManager<?> taskManager;

	private final CIResponseFactory ciResponseFactory;
	private final CIErrorFactory ciErrorFactory;

	private static final String GENERIC_SWAGGER_NOTES = "Import an adjacency matrix to a Cytoscape network" + '\n';

	public AMatReaderResource(final SynchronousTaskManager<?> taskManager,
			final AMatReaderTaskFactory aMatReaderTaskFactory, final CIResponseFactory ciResponseFactory,
			final CIErrorFactory ciErrorFactory) {
		this.taskManager = taskManager;
		this.aMatReaderTaskFactory = aMatReaderTaskFactory;
		this.ciResponseFactory = ciResponseFactory;
		this.ciErrorFactory = ciErrorFactory;

	}

	private static final Logger logger = LoggerFactory.getLogger(AMatReaderResource.class);

	private final static String resourceErrorRoot = "urn:cytoscape:ci:aMatReader-app:v1";

	private CIError buildCIError(int status, String resourcePath, String code, String message, Exception e) {
		return ciErrorFactory.getCIError(status, resourceErrorRoot + ":" + resourcePath + ":" + code, message);
	}

	CIResponse<Object> buildCIErrorResponse(int status, String resourcePath, String code, String message, Exception e) {
		CIResponse<Object> response = ciResponseFactory.getCIResponse(new Object());

		CIError error = buildCIError(status, resourcePath, code, message, e);
		if (e != null) {
			logger.error(message, e);

		} else {
			logger.error(message);
		}

		response.errors.add(error);
		return response;
	}

	@ApiModel(value = "aMatReader Response", description = "Adjacency matrix import Results in CI Format", parent = CIResponse.class)
	public static class AMatReaderResponse extends CIResponse<AMatReaderParameters> {
		@ApiModelProperty(value = "Number of nodes created from adjacency matrix")
		public int nodesCreated;

		public AMatReaderResponse(int nodesCreated) {
			this.nodesCreated = nodesCreated;
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("import")
	@ApiOperation(value = "Import a network from an adjacency matrix file", notes = GENERIC_SWAGGER_NOTES, response = AMatReaderResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistant file", response = CIResponse.class),
			@ApiResponse(code = 401, message = "Invalid parameters", response = CIResponse.class), })
	public Response aMatReader(
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters) {
		AMatReaderTaskObserver taskObserver = new AMatReaderTaskObserver(this, "aMatReader", TASK_EXECUTION_ERROR_CODE);

		InputStream is = null;
		try {
			is = new FileInputStream(new File(aMatReaderParameters.filename));

		} catch (FileNotFoundException e) {
			return Response
					.status(taskObserver.getResponse().errors.size() == 0 ? Response.Status.OK
							: Response.Status.INTERNAL_SERVER_ERROR)
					.type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(500, "aMatReader", TASK_EXECUTION_ERROR_CODE, e.getMessage(), e))
					.build();
		}
		importAdjacencyMatrix(taskObserver, is, aMatReaderParameters);

		return Response
				.status(taskObserver.getResponse().errors.size() == 0 ? Response.Status.OK
						: Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON).entity(new AMatReaderResponse(taskObserver.getNumNodesCreated()))
				.build();
	}

	public void importAdjacencyMatrix(AMatReaderTaskObserver taskObserver, InputStream is,
			AMatReaderParameters aMatReaderParameters) {
		TaskIterator taskIterator = aMatReaderTaskFactory.createTaskIterator(is, aMatReaderParameters.filename);
		taskManager.execute(taskIterator, taskObserver);
	}
}
