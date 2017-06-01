package edu.ucsf.rbvi.aMatReader.internal.tasks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class AMatReaderTask extends AbstractCyNetworkReader {
	public final BufferedReader input;
	public final String inputName;

	private enum Delimiters {
		TAB("<tab>","\t"),
		COMMA(",",","),
		BAR("|","|"),
		SPACE("<space>"," ");

		String title;
		String delimiter;

		Delimiters(String title, String delimiter) {
			this.title = title;
			this.delimiter = delimiter;
		}

		public String toString() { return title; }
		public String getDelimiter() { return delimiter; }
	}

	public Map<String, CyNode> nodeMap;
	public CyNetwork finishedNetwork = null;

	@Tunable(description="Delimiter between cells", gravity=11)
	public ListSingleSelection<Delimiters> delimiter = new ListSingleSelection<>(Delimiters.values());

	@Tunable(description="Treat edges as undirected", gravity=12)
	public boolean undirected = false;

	@Tunable(description="Set N/A values to 0.0", groups={"Advanced Options"}, params="displayState=collapsed", gravity=20)
	public boolean zeroNA = false;

	@Tunable(description="Create weight column as local", groups={"Advanced Options"}, params="displayState=collapsed", gravity=21)
	public boolean localWeight = false;

	@Tunable(description="Column name for weights", groups={"Advanced Options"}, params="displayState=collapsed", gravity=22)
	public String columnName = "weight";

	@Tunable(description="Interaction type", groups={"Advanced Options"}, params="displayState=collapsed", gravity=23)
	public String interactionName = "pp";

	@ProvidesTitle
	public String getTitle() { return "Adjacency Matrix Reader"; }

	public CyRootNetwork rootNetwork = null;

	private String namespace = CyNetwork.DEFAULT_ATTRS;

	public AMatReaderTask(final InputStream inputStream, final String name, final CyNetworkViewFactory cyNetworkViewFactory,
	                     final CyNetworkFactory cyNetworkFactory, final CyNetworkManager cyNetworkManager,
											 final CyRootNetworkManager cyRootNetworkManager) {
		super(inputStream, cyNetworkViewFactory, cyNetworkFactory, cyNetworkManager, cyRootNetworkManager);
		try {
			input = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		} catch(UnsupportedEncodingException e) {
			// Should never happen!
			throw new RuntimeException(e.getMessage());
		}

		this.inputName = name;
		nodeMap = new HashMap<>();
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		CyNetwork network = null;
		if (localWeight)
			namespace = CyNetwork.LOCAL_ATTRS;

		rootNetwork = getRootNetwork();
		if (rootNetwork != null) {
			// OK, we're using an existing collection.  Build our map
			// of keys based on the target column
			String keyColumn = getTargetColumnList().getSelectedValue();
			if (keyColumn == null) 
				keyColumn = CyRootNetwork.SHARED_NAME;

			network = rootNetwork.addSubNetwork();
			for (CyNode node: rootNetwork.getNodeList()) {
				String key = rootNetwork.getRow(node).get(keyColumn, String.class);
				nodeMap.put(key, node);
			}
		} else {
			// Create our network
			network = cyNetworkFactory.createNetwork();
		}
		network.getRow(network).set(CyNetwork.NAME, inputName);

		try {
			String[] headerRow = null;
			String[] row;
			int rowNumber = 0;
			while ((row = readRow(input)) != null) {
				if (row.length == 0) continue;

				if (rowNumber == 0) {
					headerRow = row;
					createColumns(network, headerRow);
					createAllNodes(network, headerRow);
				} else {
					createRow(network, headerRow, row);
				}

				rowNumber++;
			}

			networks = new CyNetwork[1];
			networks[0] = network;

		} catch(IOException ioe) {}
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {
		// Nothing fancy
		return cyNetworkViewFactory.createNetworkView(network);
	}

	void createAllNodes(CyNetwork network, String[] headerRow) {
		for (int i = 1; i < headerRow.length; i++) {
			createNode(network, headerRow[i], "ColumnNode");
		}
	}

	void createColumns(CyNetwork net, String[] headerRow) {
		CyTable nodeTable = net.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
		nodeTable.createColumn("Type", String.class, false);

		CyTable edgeTable = net.getTable(CyEdge.class, namespace);
		edgeTable.createColumn(columnName, Double.class, false);
		return;
	}

	void createRow(CyNetwork net, String[] headerRow, String[] dataRow) {
		String sourceName = dataRow[0];
		CyNode sourceNode = null;
		if (nodeMap.containsKey(sourceName))
			sourceNode = nodeMap.get(sourceName);
		else
			sourceNode = createNode(net, sourceName, "RowNode");

		if (sourceNode == null) return; //??

		// Now create the edges
		for (int i = 1; i < headerRow.length; i++) {
			CyNode targetNode = nodeMap.get(headerRow[i]);
			if (targetNode == null) continue;
			if (i < dataRow.length && dataRow[i] != null && dataRow[i].length() > 0) {
				// Do we already have an edge between these two nodes?
				if (undirected &&
				    net.getConnectingEdgeList(sourceNode, targetNode, CyEdge.Type.ANY).size() > 0)
					continue;

				CyEdge edge = null;
				if (rootNetwork != null && rootNetwork.containsEdge(sourceNode, targetNode)) {
					List<CyEdge> edgeList;
					if (undirected) {
						edgeList = rootNetwork.getConnectingEdgeList(sourceNode, targetNode, CyEdge.Type.ANY);
					} else {
						edgeList = rootNetwork.getConnectingEdgeList(sourceNode, targetNode, CyEdge.Type.DIRECTED);
					}

					// Not sure of the best way to handle this, but for now, just add one of the edges
					edge = edgeList.get(0);
					((CySubNetwork)net).addEdge(edge);
				} else {
					if (undirected)
						edge = net.addEdge(sourceNode, targetNode, false);
					else
						edge = net.addEdge(sourceNode, targetNode, true);
					net.getRow(edge).set(CyRootNetwork.SHARED_NAME, sourceName+" ("+interactionName+") "+headerRow[i]);
					net.getRow(edge).set(CyRootNetwork.SHARED_INTERACTION, interactionName);
					net.getRow(edge, CyNetwork.LOCAL_ATTRS).set(CyEdge.INTERACTION, interactionName);
					net.getRow(edge, CyNetwork.LOCAL_ATTRS).set(CyNetwork.NAME, sourceName+" ("+interactionName+") "+headerRow[i]);
				}
				Double value = getValue(dataRow[i]);
				net.getRow(edge, namespace).set(columnName, value);
			}
		}
	}

	CyNode createNode(CyNetwork net, String name, String type) {
		// If the root network has this node -- use it.  Otherwise
		// create a new one
		CyNode node;
		if (nodeMap.containsKey(name)) {
			node = nodeMap.get(name);
			if (!net.containsNode(node))
				((CySubNetwork)net).addNode(node); // Add the node to this subnetwork
		} else {
			node = net.addNode();
			nodeMap.put(name, node);
			net.getRow(node).set(CyRootNetwork.SHARED_NAME, name);
		}
		net.getRow(node, CyNetwork.LOCAL_ATTRS).set(CyNetwork.NAME, name);
		net.getRow(node, CyNetwork.LOCAL_ATTRS).set("Type", type);
		return node;
	}

	String[] readRow(BufferedReader input) throws IOException {
		String row = input.readLine();
		if (row == null) return null;
		if (row.startsWith("#"))
			return new String[0];

		String[] columns;
		String delim = delimiter.getSelectedValue().getDelimiter();
		columns = row.split(delim, -1);

		/*
		if (delim != null)
			columns = row.split(delim, -1);
		else {
			delim = "\t";
			columns = row.split(delim, -1);
			if (columns.length == 1) {
				delim = ",";
				columns = row.split(delim, -1);
				if (columns.length == 1) {
					delim = null;
					throw new RuntimeException("Only tabs and commas are supported column delimiters");
				}
			}
		}
		*/
		return columns;
	}

	int findColumn(String[] columns, String header) {
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(header))
				return i;
		}
		return -1;
	}

	// Handle NA, N/A, and any other possible non-numeric values
	Double getValue(String value) {
		Double v = null;
		try {
			v = new Double(value);
		} catch (NumberFormatException nfe) {
			if (zeroNA)
				v = 0.0;
		}
		return v;
	}
}
