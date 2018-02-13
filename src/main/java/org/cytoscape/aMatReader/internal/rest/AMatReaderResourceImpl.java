package org.cytoscape.aMatReader.internal.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderTask;
import org.cytoscape.aMatReader.internal.util.Delimiter;
import org.cytoscape.aMatReader.internal.util.ResettableBufferedReader;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;

@Api(tags = { "Apps: aMatReader" })
@Path("/aMatReader/v1/")
public class AMatReaderResourceImpl implements AMatReaderResource {

	private final ResourceManager resourceManager;
	private final CyNetworkManager netMngr;

	private final CIResponseFactory ciResponseFactory;
	private final CIErrorFactory ciErrorFactory;
	private final AMatReaderTaskObserver taskObserver;

	public AMatReaderResourceImpl(final CyServiceRegistrar registrar, final ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
		this.ciErrorFactory = registrar.getService(CIErrorFactory.class);
		this.ciResponseFactory = registrar.getService(CIResponseFactory.class);
		this.netMngr = registrar.getService(CyNetworkManager.class);

		taskObserver = new AMatReaderTaskObserver(this, "aMatReader", TASK_EXECUTION_ERROR_CODE);
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

	private Response buildErrorResponse(Status server_code, String error_code, Exception e) {
		return Response.status(server_code).type(MediaType.APPLICATION_JSON)
				.entity(buildCIErrorResponse(server_code.getStatusCode(), "aMatReader", error_code, e.getMessage(), e))
				.build();

	}

	@CIWrapping
	public Response aMatReader(AMatReaderParameters aMatReaderParameters) {
		return aMatReaderImport(null, aMatReaderParameters);

	}

	@CIWrapping
	public Response aMatReaderExtend(long networkSUID, AMatReaderParameters aMatReaderParameters) {
		return aMatReaderImport(new Long(networkSUID), aMatReaderParameters);
	}

	public Response aMatReaderImport(Long networkSUID, AMatReaderParameters params) {
		if (params == null) {
			return buildErrorResponse(Response.Status.BAD_REQUEST, INVALID_PARAMETERS_CODE,
					new Exception("No parameters provided"));
		}
		if (params.files == null) {
			return buildErrorResponse(Response.Status.BAD_REQUEST, INVALID_PARAMETERS_CODE,
					new Exception("No files provided"));
		}

		Map<String, Object> context = null;
		try {
			context = buildContext(params);
		} catch (Exception e) {
			return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, INVALID_PARAMETERS_CODE, e);
		}
		CyNetwork network = null;
		if (networkSUID != null) {
			network = netMngr.getNetwork(networkSUID);
			if (network == null) {
				String message = "Network with SUID " + networkSUID + " not found";
				return buildErrorResponse(Response.Status.NOT_FOUND, INVALID_PARAMETERS_CODE, new Exception(message));
			}
		}

		Response resp = importFiles(network, params, context);

		taskObserver.allFinished(FinishStatus.getSucceeded());

		return resp;

	}

	public Map<String, Object> buildContext(AMatReaderParameters params) throws Exception {
		HashMap<String, Object> context = new HashMap<String, Object>();
		ListSingleSelection<Delimiter> delim = new ListSingleSelection<Delimiter>(Delimiter.values());
		
		Delimiter delimiter = params.delimiter;
		delim.setSelectedValue(delimiter);

		if (delim.getSelectedValue() == null) {
			throw new NullPointerException("Delimiter value not recognized. Must be one of " + Delimiter.values());
		}
		if (params.symmetry == null) {
			throw new NullPointerException(
					"Unrecognized value for symmetry. Must be one of [ASYMMETRIC, SYMMETRIC_TOP, SYMMETRIC_BOTTOM]");
		}

		context.put("delimiter", delim);
		context.put("symmetry", params.symmetry);
		context.put("interactionName", params.interactionName);
		context.put("rowNames", params.rowNames);
		context.put("columnNames", params.columnNames);
		context.put("ignoreZeros", params.ignoreZeros);
		context.put("removeColumnPrefix", params.removeColumnPrefix);

		return context;
	}

	public Response importFiles(CyNetwork network, AMatReaderParameters params, Map<String, Object> context) {
		CyNetwork net = network;

		for (String f : params.files) {
			try {
				CIResponse<?> response = importFile(net, f, context);

				if (response instanceof AMatReaderResponse) {
					Long suid = taskObserver.aMatResponse.data.suid;
					if (net == null && suid != null) {
						net = resourceManager.netManager.getNetwork(suid);
					}
				} else {
					if (net != null)
						resourceManager.netManager.destroyNetwork(net);
					break;
				}
			} catch (FileNotFoundException e) {
				if (net != null)
					resourceManager.netManager.destroyNetwork(net);
				return buildErrorResponse(Response.Status.NOT_FOUND, INVALID_FILE_CODE, e);
			} catch (SecurityException | IOException e) {
				if (net != null)
					resourceManager.netManager.destroyNetwork(net);
				return buildErrorResponse(Response.Status.BAD_REQUEST, INVALID_FILE_CODE, e);
			} catch (NullPointerException e) {
				if (net != null)
					resourceManager.netManager.destroyNetwork(net);
				return buildErrorResponse(Response.Status.BAD_REQUEST, TASK_EXECUTION_ERROR_CODE, e);
			}
		}
		taskObserver.allFinished(FinishStatus.getSucceeded());
		CIResponse<?> resp = taskObserver.getResponse();

		return Response.status(resp.errors.size() == 0 ? Response.Status.OK : Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON).entity(resp).build();

	}

	public CIResponse<?> runTask(CyNetwork network, InputStream is, String name, Map<String, Object> context)
			throws IOException, NullPointerException {
		AMatReaderTask task;
		ResettableBufferedReader reader = new ResettableBufferedReader(is);
		if (network == null)
			task = new AMatReaderTask(reader, name, resourceManager);
		else
			task = new AMatReaderTask(network, reader, name, resourceManager);

		resourceManager.taskManager.setExecutionContext(context);
		resourceManager.taskManager.execute(new TaskIterator(task));

		taskObserver.taskFinished(task);

		return taskObserver.getResponse();
	}

	public CIResponse<?> importFile(CyNetwork network, String filename, Map<String, Object> context)
			throws FileNotFoundException, NullPointerException, SecurityException, IOException {
		File f = new File(filename);
		InputStream is = new FileInputStream(f);
		return runTask(network, is, f.getName(), context);

	}

	public CIResponse<?> importString(String matrix, String name, Map<String, Object> context)
			throws NullPointerException, IOException {
		
		ByteArrayInputStream bais = new ByteArrayInputStream(matrix.getBytes());
		return runTask(null, bais, name, context);

	}

}
