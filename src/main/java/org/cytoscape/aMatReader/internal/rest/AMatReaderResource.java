package org.cytoscape.aMatReader.internal.rest;

import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
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
public interface AMatReaderResource {

	public static final String INVALID_FILE_CODE = "1";
	public static final String INVALID_PARAMETERS_CODE = "2";
	public static final String TASK_EXECUTION_ERROR_CODE = "3";

	static final String GENERIC_SWAGGER_NOTES = "Adjacency matrix files usually specify the node names in the first column and/or first row "
			+ '\n'
			+ "and all values are delimited by a tab, space, comma, or pipe '|'. This app only allows for integer/floating point values for easy parsing.";
	static final String IMPORT_NOTES = "Import a adjacency matrix file(s) as a new Cytoscape network. " + '\n';
	static final String EXTEND_NOTES = "Use adjacency matrix file(s) to add edge attributes to an existing Cytoscape network. "
			+ '\n';
	static final String PREDICT_NOTES = "Get the suggested parameters for a matrix file by peeking at the first two lines.  NOTE: The parser can not predict if the matrix represents an undirected network.";

	static final Logger logger = LoggerFactory.getLogger(AMatReaderResource.class);
	final static String resourceErrorRoot = "urn:cytoscape:ci:aMatReader-app:v1";

	@ApiModel(value = "aMatReader Response", description = "aMatReader new/updated nodes in Cytoscape", parent = CIResponse.class)

	public static class AMatReaderResponse extends CIResponse<AMatReaderResult> {
		public AMatReaderResponse(Long suid, int newEdges, int updatedEdges) {
			this.data = new AMatReaderResult(suid, newEdges, updatedEdges);
			this.errors = new ArrayList<CIError>();
		}

		public void update(AMatReaderResponse resp) {
			data.newEdges += resp.data.newEdges;
			data.updatedEdges += resp.data.updatedEdges;
			data.suid = resp.data.suid;
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("predictParameters")
	@ApiOperation(value = "Peek at a file to determine if row/column names exist, columns have prefixes.", notes = PREDICT_NOTES, response = Response.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistent file", response = CIResponse.class) })
	public abstract Response aMatReaderPredict(
			@ApiParam(value = "Adjacency matrix file path", required = true, defaultValue="/path/to/matrix.adj") @QueryParam("path") String path);

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("import")
	@ApiOperation(value = "Import a new network from adjacency matrix file(s)", notes = IMPORT_NOTES
			+ GENERIC_SWAGGER_NOTES, response = AMatReaderResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistent file", response = CIResponse.class),
			@ApiResponse(code = 401, message = "Invalid parameters", response = CIResponse.class), })
	public abstract Response aMatReader(
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters);

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("extend/{networkSUID}")
	@ApiOperation(value = "Add an edge attribute column to an existing network from adjacency matrix file(s)", notes = EXTEND_NOTES
			+ GENERIC_SWAGGER_NOTES, response = AMatReaderResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistent file", response = CIResponse.class),
			@ApiResponse(code = 401, message = "Invalid parameters", response = CIResponse.class), })
	public abstract Response aMatReaderExtend(
			@ApiParam(value = "Network SUID to extend") @PathParam("networkSUID") long networkSUID,
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters);
}
