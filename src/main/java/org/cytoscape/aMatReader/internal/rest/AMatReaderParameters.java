package org.cytoscape.aMatReader.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to AMatReaderTask
 * 
 * @author brettjsettle
 *
 */

@ApiModel(value = "Adjacency Matrix Reader", description = "Import adjacency matrix .mat files as Cytoscape networks")
public class AMatReaderParameters {
	@ApiModelProperty(value = "Path of .mat/.adj file to be imported", required=true)
	public String filename;
	
	@ApiModelProperty(value = "Delimiter between cells", example="TAB")
	public Delimiter delimiter = Delimiter.TAB;
	
	@ApiModelProperty(value = "Treat edges as undirected", example="false", required=false)
	public boolean undirected = false;

	@ApiModelProperty(value = "Interaction type", example = "interacts with", required=false)
	public String interactionName = "interacts with";
	

}
