package org.cytoscape.aMatReader.internal.tasks;

import org.cytoscape.aMatReader.internal.rest.Delimiters;
import org.cytoscape.work.util.ListSingleSelection;

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
	@ApiModelProperty(value = "Path of .mat file to be imported")
	public String filename;
	@ApiModelProperty(value = "Delimiter between cells")
	public ListSingleSelection<Delimiters> delimiter = new ListSingleSelection<>(Delimiters.values());

	@ApiModelProperty(value = "Treat edges as undirected")
	public boolean undirected = false;

	@ApiModelProperty(value = "Set N/A to a fixed value")
	public boolean zeroNA = false;

	@ApiModelProperty(value = "Set N/A to a fixed value")
	public double naValue = 0.0;

	@ApiModelProperty(value = "Create weight column as local")
	public boolean localWeight = false;

	@ApiModelProperty(value = "Column name for weights", example = "weight")
	public String columnName = "weight";

	@ApiModelProperty(value = "Interaction type", example = "pp")
	public String interactionName = "pp";
}
