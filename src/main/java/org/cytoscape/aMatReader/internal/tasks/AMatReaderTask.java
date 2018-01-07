package org.cytoscape.aMatReader.internal.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.swing.JOptionPane;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.CyActivator;
import org.cytoscape.aMatReader.internal.rest.ColumnHeadersFormat;
import org.cytoscape.aMatReader.internal.rest.Delimiter;
import org.cytoscape.aMatReader.internal.rest.RowHeadersFormat;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class AMatReaderTask extends AbstractCyNetworkReader implements ObservableTask {
	private String columnName;
	private CyRootNetwork rootNetwork;
	private AMatReaderResponse result;
	private final Map<Object, CyNode> nodeMap;

	@Tunable(description = "Delimiter between cells", gravity = 11)
	public ListSingleSelection<Delimiter> delimiter = new ListSingleSelection<>(Delimiter.values());

	@Tunable(description = "Treat edges as undirected", gravity = 12)
	public boolean undirected = false;

	@Tunable(description = "Interaction type", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 13)
	public String interactionName = "interacts with";

	@Tunable(description = "Column headers as first row", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 14)
	public ListSingleSelection<ColumnHeadersFormat> columnHeaders = new ListSingleSelection<ColumnHeadersFormat>(
			ColumnHeadersFormat.values());

	@Tunable(description = "Row headers as first column", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 15)
	public ListSingleSelection<RowHeadersFormat> rowHeaders = new ListSingleSelection<RowHeadersFormat>(
			RowHeadersFormat.values());

	@ProvidesTitle
	public String getTitle() {
		return "Adjacency Matrix Reader";
	}

	public AMatReaderTask(final InputStream inputStream, final String name,
			final CyNetworkViewFactory cyNetworkViewFactory, final CyNetworkFactory cyNetworkFactory,
			final CyNetworkManager cyNetworkManager, final CyRootNetworkManager cyRootNetworkManager) {
		super(inputStream, cyNetworkViewFactory, cyNetworkFactory, cyNetworkManager, cyRootNetworkManager);
		rootNetwork = getRootNetwork();
		nodeMap = getNodeMap();

		String shortName = name;
		if (shortName.contains("."))
			shortName = shortName.substring(0, shortName.lastIndexOf('.'));
		if (shortName.contains("/"))
			shortName = shortName.substring(shortName.lastIndexOf('/') + 1);
		columnName = shortName;

	}

	public AMatReaderTask(final CyRootNetwork rootNetwork, final InputStream inputStream, final String name,
			final CyNetworkViewFactory cyNetworkViewFactory, final CyNetworkFactory cyNetworkFactory,
			final CyNetworkManager cyNetworkManager, final CyRootNetworkManager cyRootNetworkManager) {
		super(inputStream, cyNetworkViewFactory, cyNetworkFactory, cyNetworkManager, cyRootNetworkManager);
		this.rootNetwork = rootNetwork;
		nodeMap = getNodeMap();

		String shortName = name;
		if (shortName.contains("."))
			shortName = shortName.substring(0, shortName.lastIndexOf('.'));
		if (shortName.contains("/"))
			shortName = shortName.substring(shortName.lastIndexOf('/') + 1);

		columnName = shortName;
	}

	void createColumns(CyNetwork net) {
		CyTable nodeTable = net.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
		if (nodeTable.getColumn("Type") == null)
			nodeTable.createColumn("Type", String.class, false);

		CyTable edgeTable = net.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);
		if (edgeTable.getColumn(columnName) != null) {
			columnName = JOptionPane.showInputDialog(CyActivator.PARENT_FRAME,
					"Column " + columnName + " already exists. Choose a different name:", columnName);
		}
		edgeTable.createColumn(columnName, Double.class, false);
		return;
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		if (rootNetwork == null)
			rootNetwork = getRootNetwork();

		Delimiter delim = delimiter.getSelectedValue();
		RowHeadersFormat rowHeader = rowHeaders.getSelectedValue();
		ColumnHeadersFormat columnHeader = columnHeaders.getSelectedValue();

		MatrixParser parser = new MatrixParser(inputStream, delim, undirected, rowHeader, columnHeader);
		CyNetwork network;
		if (rootNetwork != null) {
			if (rootNetwork.getSubNetworkList().size() > 0) {
				network = rootNetwork.getSubNetworkList().get(0);
			} else {
				network = rootNetwork.addSubNetwork();
				network.getRow(network).set(CyNetwork.NAME, columnName);
			}
			String keyColumn = getTargetColumnList().getSelectedValue();
			if (keyColumn == null)
				keyColumn = CyNetwork.NAME;

			for (CyNode node : rootNetwork.getNodeList()) {
				String key = rootNetwork.getRow(node).get(keyColumn, String.class);
				nodeMap.put(key, node);
			}
			for (CyNode node : network.getNodeList()) {
				String key = rootNetwork.getRow(node).get(keyColumn, String.class);
				nodeMap.put(key, node);
			}
		} else {
			network = cyNetworkFactory.createNetwork();
			network.getRow(network).set(CyNetwork.NAME, columnName);
		}

		createColumns(network);

		for (int i = 0; i < parser.sourceCount(); i++) {
			createNode(network, parser.getSourceName(i), "ColumnNode");
		}
		for (int i = 0; i < parser.targetCount(); i++) {
			createNode(network, parser.getTargetName(i), "SourceNode");
		}

		int edges = 0;
		Map<Integer, Map<Integer, Double>> edgeMap = parser.getEdges();
		for (int src : edgeMap.keySet()) {
			Map<Integer, Double> tgtMap = edgeMap.get(src);
			String srcName = parser.getSourceName(src);
			for (int tgt : tgtMap.keySet()) {
				String tgtName = parser.getTargetName(tgt);
				Double value = tgtMap.get(tgt);

				if (createEdge(network, srcName, tgtName, value, undirected))
					edges++;
			}
		}

		networks = new CyNetwork[] { network };
		result = new AMatReaderResponse(edges);
	}

	CyNode createNode(CyNetwork net, String name, String type) {
		// If the root network has this node -- use it. Otherwise
		// create a new one
		CyNode node;
		if (nodeMap.containsKey(name)) {
			node = nodeMap.get(name);
			if (!net.containsNode(node))
				((CySubNetwork) net).addNode(node); // Add the node to this
													// subnetwork
		} else {
			Collection<CyRow> rows = net.getDefaultNodeTable().getMatchingRows(CyNetwork.NAME, name);
			if (rows.size() > 0) {
				CyRow row = rows.iterator().next();
				node = net.getNode(row.get(CyNode.SUID, Long.class));
			} else {
				node = net.addNode();
				nodeMap.put(name, node);
				net.getRow(node).set(CyNetwork.NAME, name);
			}
		}
		net.getRow(node, CyNetwork.LOCAL_ATTRS).set(CyNetwork.NAME, name);
		net.getRow(node, CyNetwork.LOCAL_ATTRS).set("Type", type);
		return node;
	}

	String[] readRow(BufferedReader input) throws IOException {
		String row = input.readLine();
		if (row == null)
			return null;
		if (row.startsWith("#"))
			return new String[0];

		String[] columns;
		String delim = delimiter.getSelectedValue().getDelimiter();
		columns = row.split(delim, -1);

		return columns;
	}

	private boolean createEdge(CyNetwork network, String srcName, String tgtName, Double value, boolean directed) {

		CyNode src = createNode(network, srcName, "RowNode");
		CyNode tgt = createNode(network, tgtName, "ColumnNode");
		CyEdge edge = null;

		boolean created = false;

		for (CyEdge e : network.getConnectingEdgeList(src, tgt, CyEdge.Type.ANY)) {
			edge = e;
			break;
		}

		if (edge == null) {
			edge = network.addEdge(src, tgt, directed);
			network.getDefaultEdgeTable().getRow(edge.getSUID()).set(CyEdge.INTERACTION, interactionName);
			network.getDefaultEdgeTable().getRow(edge.getSUID()).set("name",
					String.format("%s (%s) %s", srcName, interactionName, tgtName));
			created = true;
		}
		network.getDefaultEdgeTable().getRow(edge.getSUID()).set(columnName, value);
		return created;
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {
		// Nothing fancy
		return cyNetworkViewFactory.createNetworkView(network);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}
}
