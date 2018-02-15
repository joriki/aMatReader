package org.cytoscape.aMatReader.internal.rest;

import org.cytoscape.aMatReader.internal.util.Delimiter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Parameters for passing to AMatReaderTask
 * 
 * @author brettjsettle
 *
 */

@ApiModel(value = "Adjacency Matrix Reader", description = "Import adjacency matrices as Cytoscape networks")
public class AMatReaderParameters {
	@ApiModelProperty(value = "Matrix file(s) to be imported", required=true)
	public String[] files;
	
	@ApiModelProperty(value = "Delimiter", example="TAB")
	public Delimiter delimiter = Delimiter.TAB;
	
	@ApiModelProperty(value = "Treat as undirected", example="false", required=false)
	public boolean undirected = false;
	
	@ApiModelProperty(value = "Ignore zero values", example="true", required=false)
	public boolean ignoreZeros = true;

	@ApiModelProperty(value = "Interaction type", example = "interacts with", required=false)
	public String interactionName = "interacts with";
	
	@ApiModelProperty(value="Row names", required=false, example="true")
	public boolean rowNames = true;
	
	@ApiModelProperty(value="Column names", required=false, example="true")
	public boolean columnNames = true;

	@ApiModelProperty(value="Remove column prefix", required=false, example="true", notes="e.g. prefix.Node1, prefix.Node2, etc. (common in R matrices)")
	public boolean removeColumnPrefix = false;
	
}
