package org.cytoscape.aMatReader.internal.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

	static final String GENERIC_SWAGGER_NOTES = "Import an adjacency matrix to a Cytoscape network" + '\n';

	static final Logger logger = LoggerFactory.getLogger(AMatReaderResource.class);

	final static String resourceErrorRoot = "urn:cytoscape:ci:aMatReader-app:v1";

	@ApiModel(value = "aMatReader Response", description = "aMatReader new/updated nodes in Cytoscape", parent = CIResponse.class)
	public static class AMatReaderResponse extends CIResponse<AMatReaderResult> {
		public AMatReaderResponse(long suid, int newEdges, int updatedEdges) {
			this.data = new AMatReaderResult(suid, newEdges, updatedEdges);
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("import")
	@ApiOperation(value = "Import a network from adjacency matrix file(s)", notes = GENERIC_SWAGGER_NOTES, response = AMatReaderResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistant file", response = CIResponse.class),
			@ApiResponse(code = 401, message = "Invalid parameters", response = CIResponse.class), })
	public abstract Response aMatReader(
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters);

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("extend/{networkCollectionSUID}")
	@ApiOperation(value = "Import a network from adjacency matrix file(s)", notes = GENERIC_SWAGGER_NOTES, response = AMatReaderResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid or nonexistant file", response = CIResponse.class),
			@ApiResponse(code = 401, message = "Invalid parameters", response = CIResponse.class), })
	public abstract Response aMatReaderExtend(
			@ApiParam(value = "Collection SUID to extend") @PathParam("collectionSUID") long collectionSUID,
			@ApiParam(value = "Adjacency matrix import parameters", required = true) AMatReaderParameters aMatReaderParameters);
}
