package org.cytoscape.aMatReader.internal.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderTask;
import org.cytoscape.aMatReader.internal.util.Delimiter;
import org.cytoscape.aMatReader.internal.util.HeaderColumnFormat;
import org.cytoscape.aMatReader.internal.util.HeaderRowFormat;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.ci.CIWrapping;
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

@Api(tags = { "Apps: aMatReader" })
@Path("/aMatReader/v1/")
public class AMatReaderResourceImpl implements AMatReaderResource {

	private final ResourceManager resourceManager;
	private final SynchronousTaskManager<?> taskManager;
	private final CyNetworkManager netMngr;
	private final CyRootNetworkManager rootMngr;

	private final CIResponseFactory ciResponseFactory;
	private final CIErrorFactory ciErrorFactory;

	public AMatReaderResourceImpl(final CyServiceRegistrar registrar,
			final ResourceManager resourceManager) {
		super();
		this.taskManager = registrar.getService(SynchronousTaskManager.class);
		this.resourceManager = resourceManager;
		this.ciErrorFactory = registrar.getService(CIErrorFactory.class);
		this.ciResponseFactory = registrar.getService(CIResponseFactory.class);
		this.netMngr = registrar.getService(CyNetworkManager.class);
		this.rootMngr = registrar.getService(CyRootNetworkManager.class);

	}

	private static final Logger logger = LoggerFactory.getLogger(AMatReaderResource.class);

	private final static String resourceErrorRoot = "urn:cytoscape:ci:aMatReader-app:v1";

	private CIError buildCIError(int status, String resourcePath, String code, String message, Exception e) {
		return ciErrorFactory.getCIError(status, resourceErrorRoot + ":" + resourcePath + ":" + code, message);
	}

	public CIResponse<Object> buildCIErrorResponse(int status, String resourcePath, String code, String message,
			Exception e) {
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

	@CIWrapping
	public Response aMatReader(AMatReaderParameters aMatReaderParameters) {
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

	@CIWrapping
	public Response aMatReaderExtend(long collectionSUID, AMatReaderParameters aMatReaderParameters) {

		AMatReaderTaskObserver taskObserver = new AMatReaderTaskObserver(this, "aMatReader", TASK_EXECUTION_ERROR_CODE);
		File adjFile = new File(aMatReaderParameters.filename);
		if (!adjFile.exists())
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
					.entity(buildCIErrorResponse(404, "aMatReader", INVALID_FILE_CODE,
							"File " + aMatReaderParameters.filename + " does not exist",
							new Exception("Nonexistant file")))
					.build();

		try {
			return extendAdjacencyMatrix(taskObserver, collectionSUID, aMatReaderParameters);
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
		ListSingleSelection<HeaderRowFormat> row = new ListSingleSelection<HeaderRowFormat>(HeaderRowFormat.values());
		ListSingleSelection<HeaderColumnFormat> column = new ListSingleSelection<HeaderColumnFormat>(
				HeaderColumnFormat.values());
		row.setSelectedValue(params.headerRow);
		column.setSelectedValue(params.headerColumn);
		Delimiter delimiter = params.delimiter;
		delim.setSelectedValue(delimiter);
		context.put("delimiter", delim);
		context.put("undirected", params.undirected);
		context.put("interactionName", params.interactionName);
		context.put("headerRow", row);
		context.put("headerColumn", column);

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

	private String getCollectionName(long collectionSUID) throws NetworkCollectionNotFoundException {
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
			AMatReaderTask task = new AMatReaderTask(is, filename, resourceManager);
			
			taskManager.setExecutionContext(context);
			taskManager.execute(new TaskIterator(task));

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
