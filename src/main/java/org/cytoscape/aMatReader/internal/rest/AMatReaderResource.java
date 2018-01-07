package org.cytoscape.aMatReader.internal.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.aMatReader.internal.tasks.AMatReaderTaskFactory;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = { "Apps: aMatReader" })
@Path("/aMatReader/v1/")
public class AMatReaderResource {

	public static final String INVALID_FILE_CODE = "1";
	public static final String INVALID_PARAMETERS_CODE = "2";
	public static final String TASK_EXECUTION_ERROR_CODE = "3";

	private final AMatReaderTaskFactory aMatReaderTaskFactory;
	private final SynchronousTaskManager<?> taskManager;
	private final CyNetworkManager netMngr;
	private final CyRootNetworkManager rootMngr;

	private final CIResponseFactory ciResponseFactory;
	private final CIErrorFactory ciErrorFactory;
	private final String rootList;

	private static final String GENERIC_SWAGGER_NOTES = "Import an adjacency matrix to a Cytoscape network" + '\n';
	
	public AMatReaderResource(final CyServiceRegistrar registrar, final AMatReaderTaskFactory aMatReaderTaskFactory){
		super();
		this.taskManager = registrar.getService(SynchronousTaskManager.class);
		this.aMatReaderTaskFactory  = aMatReaderTaskFactory;
		this.ciErrorFactory = registrar.getService(CIErrorFactory.class);
		this.ciResponseFactory = registrar.getService(CIResponseFactory.class);
		this.netMngr = registrar.getService(CyNetworkManager.class);
		this.rootMngr = registrar.getService(CyRootNetworkManager.class);
		
		HashSet<String> rootSUIDs = new HashSet<String>();
		for (CyNetwork n : netMngr.getNetworkSet()){
			CyRootNetwork root = rootMngr.getRootNetwork(n);
			rootSUIDs.add(String.valueOf(root.getSUID()));
		}
		
		rootList = String.join(", ", rootSUIDs);
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

	@ApiModel(value = "aMatReader Response", description = "Number of nodes created from adjacency matrix", parent = CIResponse.class)
	public static class AMatReaderResponse extends CIResponse<Integer> {
		public AMatReaderResponse(int nodesAffected) {
			this.data = nodesAffected;
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
	public Response aMatReaderWithRoot(
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters) {
		AMatReaderTaskObserver taskObserver = new AMatReaderTaskObserver(this, "aMatReader", TASK_EXECUTION_ERROR_CODE);

		File adjFile = new File(aMatReaderParameters.filename);
		if (!adjFile.exists())
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(500, "aMatReader", INVALID_FILE_CODE,
							"File " + aMatReaderParameters.filename + " does not exist", null))
					.build();

		try {
			importAdjacencyMatrix(taskObserver, aMatReaderParameters);
		} catch (Exception e) {
			return Response
					.status(taskObserver.getResponse().errors.size() == 0 ? Response.Status.OK
							: Response.Status.INTERNAL_SERVER_ERROR)
					.type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(500, "aMatReader", TASK_EXECUTION_ERROR_CODE, e.getMessage(), e))
					.build();
		}
		return Response
				.status(taskObserver.getResponse().errors.size() == 0 ? Response.Status.OK
						: Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON).entity(taskObserver.getResponse()).build();
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("extend/{networkCollectionSUID}")
	@ApiOperation(value = "Import a network from an adjacency matrix file", notes = GENERIC_SWAGGER_NOTES, response = AMatReaderResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistant file", response = CIResponse.class),
			@ApiResponse(code = 401, message = "Invalid parameters", response = CIResponse.class), })
	public Response aMatReader(
			@ApiParam(value = "CollectionSUID to use") @PathParam("networkCollectionSUID") long networkCollectionSUID,
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters) {

		AMatReaderTaskObserver taskObserver = new AMatReaderTaskObserver(this, "aMatReader", TASK_EXECUTION_ERROR_CODE);
		File adjFile = new File(aMatReaderParameters.filename);
		if (!adjFile.exists())
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(404, "aMatReader", INVALID_FILE_CODE,
							"File " + aMatReaderParameters.filename + " does not exist",
							new Exception("Nonexistant file")))
					.build();

		try {
			return extendAdjacencyMatrix(taskObserver, networkCollectionSUID, aMatReaderParameters);
		} catch (Exception e) {
			return Response
					.status(taskObserver.getResponse().errors.size() == 0 ? Response.Status.OK
							: Response.Status.INTERNAL_SERVER_ERROR)
					.type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(500, "aMatReader", TASK_EXECUTION_ERROR_CODE, e.getMessage(), e))
					.build();
		}
	}

	public Map<String, Object> buildContext(AMatReaderParameters params) throws Exception {
		HashMap<String, Object> context = new HashMap<String, Object>();

		ListSingleSelection<Delimiter> delim = new ListSingleSelection<Delimiter>(Delimiter.values());
		Delimiter delimiter = params.delimiter;
		delim.setSelectedValue(delimiter);
		context.put("delimiter", delim);
		context.put("undirected", params.undirected);
		context.put("interactionName", params.interactionName);

		return context;
	}

	public Response importAdjacencyMatrix(AMatReaderTaskObserver taskObserver,
			AMatReaderParameters aMatReaderParameters) throws IOException {
		try {
			Map<String, Object> context = buildContext(aMatReaderParameters);
			return runTask(aMatReaderParameters.filename, taskObserver, context);
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(500, "aMatReader", TASK_EXECUTION_ERROR_CODE, e.getMessage(), e))
					.build();
		}

	}

	private String getCollectionName(long collectionSUID)
			throws NetworkCollectionNotFoundException {
		CyRootNetwork root = null;
		for (CyNetwork net : netMngr.getNetworkSet()) {
			final CyRootNetwork rootNet = rootMngr.getRootNetwork(net);
			if (rootNet.getSUID() == collectionSUID) {
				root = rootNet;
			}
		}
		if (root == null)
			throw new NetworkCollectionNotFoundException();

		return root.getRow(root).get(CyRootNetwork.NAME, String.class);
	}

	private class NetworkCollectionNotFoundException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3386053605487034993L;
	}

	public Response extendAdjacencyMatrix(AMatReaderTaskObserver taskObserver, long collectionSUID, 
			AMatReaderParameters aMatReaderParameters) throws IOException {

		String collectionName = null;
		try {
			Map<String, Object> context = buildContext(aMatReaderParameters);
			collectionName = getCollectionName(collectionSUID);
			ListSingleSelection<String> root = new ListSingleSelection<String>(collectionName);

			context.put("rootNetworkList", root);

			return runTask(aMatReaderParameters.filename, taskObserver, context);

		} catch (NetworkCollectionNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(404, "aMatReader", INVALID_PARAMETERS_CODE,
							"Network Collection with SUID " + collectionSUID + " not found",
							new Exception("Network collection not found")))
					.build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(500, "aMatReader", TASK_EXECUTION_ERROR_CODE, e.getMessage(), e))
					.build();
		}

	}

	public Response runTask(String filename, AMatReaderTaskObserver taskObserver, Map<String, Object> context) {
		try {
			InputStream is = new FileInputStream(new File(filename));
			TaskIterator taskIterator = aMatReaderTaskFactory.createTaskIterator(is, filename);

			taskManager.setExecutionContext(context);
			taskManager.execute(taskIterator, taskObserver);
			is.close();
		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(
					buildCIErrorResponse(404, "aMatReader", INVALID_PARAMETERS_CODE, "Failed to open " + filename, e))
					.build();
		}

		return Response
				.status(taskObserver.getResponse().errors.size() == 0 ? Response.Status.OK
						: Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON).entity(taskObserver.getResponse()).build();
	}
}
