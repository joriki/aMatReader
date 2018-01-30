package org.cytoscape.aMatReader.internal.rest;

import org.cytoscape.aMatReader.internal.util.Delimiter;
import org.cytoscape.aMatReader.internal.util.HeaderColumnFormat;
import org.cytoscape.aMatReader.internal.util.HeaderRowFormat;

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
	@ApiModelProperty(value = "Paths of .mat/.adj file to be imported", required=true)
	public String[] files;
	
	@ApiModelProperty(value = "Delimiter between cells", example="TAB")
	public Delimiter delimiter = Delimiter.TAB;
	
	@ApiModelProperty(value = "Treat edges as undirected", example="false", required=false)
	public boolean undirected = false;
	
	@ApiModelProperty(value = "Ignore zero values", example="false", required=false)
	public boolean ignoreZeros = false;

	@ApiModelProperty(value = "Interaction type", example = "interacts with", required=false)
	public String interactionName = "interacts with";
	
	@ApiModelProperty(value="Source Header Column", required=false)
	public HeaderColumnFormat headerColumn = HeaderColumnFormat.NAMES;
	
	@ApiModelProperty(value="Target Header Row", required=false)
	public HeaderRowFormat headerRow = HeaderRowFormat.NAMES;

	
	
	
	
}
