package org.cytoscape.aMatReader.internal.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.util.Delimiter;
import org.cytoscape.aMatReader.internal.util.HeaderColumnFormat;
import org.cytoscape.aMatReader.internal.util.HeaderRowFormat;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class AMatReaderTask2 extends AbstractTask implements CyNetworkReader, ObservableTask {
	private String columnName;
	private CyRootNetwork rootNetwork;
	private AMatReaderResponse result;
	private boolean createView = false;
	private final Map<Object, CyNode> nodeMap;
	private final InputStream inputStream;
	private CyNetwork resultNetwork;

	private final ResourceManager rm;

	private final String SOURCE_NAME = "SourceNode";
	private final String TARGET_NAME = "TargetNode";

	@Tunable(description = "Delimiter between cells", gravity = 11)
	public ListSingleSelection<Delimiter> delimiter = new ListSingleSelection<>(Delimiter.values());

	@Tunable(description = "Treat edges as undirected", gravity = 12)
	public boolean undirected = false;

	@Tunable(description = "Ignore zero values", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 13)
	public boolean ignoreZeros = true;

	@Tunable(description = "Interaction type", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 14)
	public String interactionName = "interacts with";

	@Tunable(description = "Source node names in first column", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 15)
	public ListSingleSelection<HeaderColumnFormat> headerColumn = new ListSingleSelection<HeaderColumnFormat>(
			HeaderColumnFormat.values());

	@Tunable(description = "Target node names in first row", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 16)
	public ListSingleSelection<HeaderRowFormat> headerRow = new ListSingleSelection<HeaderRowFormat>(
			HeaderRowFormat.values());

	@ProvidesTitle
	public String getTitle() {
		return "Adjacency Matrix Reader";
	}

	public AMatReaderTask2(final InputStream inputStream, final String name, final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		rootNetwork = null;
		nodeMap = new HashMap<Object, CyNode>();

		columnName = getBaseName(name);
		this.rm = rm;
	}

	private String getBaseName(String name) {
		String shortName = name;
		if (shortName.contains("."))
			shortName = shortName.substring(0, shortName.lastIndexOf('.'));
		if (shortName.contains("/"))
			shortName = shortName.substring(shortName.lastIndexOf('/') + 1);
		return shortName;
	}

	public AMatReaderTask2(final CyRootNetwork rootNetwork, final InputStream inputStream, final String name,
			final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		this.rootNetwork = rootNetwork;
		nodeMap = new HashMap<Object, CyNode>();

		columnName = getBaseName(name);
		this.rm = rm;
	}

	void createColumns(CyNetwork net) {
		CyTable nodeTable = net.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
		if (nodeTable.getColumn("Type") == null)
			nodeTable.createColumn("Type", String.class, false);

		CyTable edgeTable = net.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);

		if (edgeTable.getColumn(columnName) != null) {
			int n = 2;
			while (edgeTable.getColumn(columnName + " " + n) != null) {
				n++;
			}
			columnName = columnName + " " + n;
		}
		edgeTable.createColumn(columnName, Double.class, false);
		return;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws InvocationTargetException, InterruptedException, IOException {

		Delimiter delim = delimiter.getSelectedValue();
		HeaderColumnFormat headerColumn = this.headerColumn.getSelectedValue();
		HeaderRowFormat headerRow = this.headerRow.getSelectedValue();

		final MatrixParser parser = new MatrixParser(inputStream, delim, undirected, ignoreZeros, headerColumn,
				headerRow);
		final CyNetwork network;

		if (rootNetwork == null) {
			network = rm.netFactory.createNetwork();

		} else {
			network = rootNetwork.addSubNetwork();
			for (CyNode node : rootNetwork.getNodeList()) {
				String key = rootNetwork.getRow(node).get(CyNetwork.NAME, String.class);
				nodeMap.put(key, node);
			}
			for (CyNode node : network.getNodeList()) {
				String key = rootNetwork.getRow(node).get(CyNetwork.NAME, String.class);
				nodeMap.put(key, node);
			}
		}
		String netName = rm.naming.getSuggestedNetworkTitle(columnName);
		network.getRow(network).set(CyNetwork.NAME, netName);
		createView = network.getEdgeCount() < 10000;
		resultNetwork = network;
		rm.netManager.addNetwork(network);

		/*
		 * if (rootNetwork != null) { if (rootNetwork.getSubNetworkList().size()
		 * > 0) { network = rootNetwork.getSubNetworkList().get(0); } else {
		 * network = rootNetwork.addSubNetwork(); String netName =
		 * rm.naming.getSuggestedSubnetworkTitle(network);
		 * network.getRow(network).set(CyNetwork.NAME, netName); } String
		 * keyColumn = CyNetwork.NAME;
		 * 
		 * 
		 * for (CyNode node : rootNetwork.getNodeList()) { String key =
		 * rootNetwork.getRow(node).get(keyColumn, String.class);
		 * nodeMap.put(key, node); } for (CyNode node : network.getNodeList()) {
		 * String key = rootNetwork.getRow(node).get(keyColumn, String.class);
		 * nodeMap.put(key, node); } } else { network =
		 * rm.netFactory.createNetwork(); String netName =
		 * rm.naming.getSuggestedNetworkTitle(columnName);
		 * network.getRow(network).set(CyNetwork.NAME, netName); resultNetwork =
		 * network; rm.netManager.addNetwork(network); createView =
		 * network.getEdgeCount() < 10000; }
		 */
		createColumns(network);
		for (int i = 0; i < parser.sourceCount(); i++) {
			createNode(network, parser.getSourceName(i), SOURCE_NAME);
		}
		for (int i = 0; i < parser.targetCount(); i++) {
			createNode(network, parser.getTargetName(i), TARGET_NAME);
		}

		int newEdgeCount = 0, updatedEdgeCount = 0;
		Map<Integer, Map<Integer, Double>> edgeMap = parser.getEdges();
		for (int src : edgeMap.keySet()) {
			Map<Integer, Double> tgtMap = edgeMap.get(src);
			String srcName = parser.getSourceName(src);
			for (int tgt : tgtMap.keySet()) {
				String tgtName = parser.getTargetName(tgt);
				Double value = tgtMap.get(tgt);
				if (createEdge(network, srcName, tgtName, value))
					newEdgeCount++;
				else {
					updatedEdgeCount++;
				}
			}
		}
		inputStream.close();

		rm.eventHelper.flushPayloadEvents();
		result = new AMatReaderResponse(network.getSUID(), newEdgeCount, updatedEdgeCount);

		if (createView)
			layoutNetwork(network);

		taskMonitor.setProgress(1.0);

	}

	private void layoutNetwork(CyNetwork network) {
		CyNetworkView view = buildCyNetworkView(network);
		System.out.println(view);
		CyLayoutAlgorithm algor = rm.layoutManager.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,
				null);
		insertTasksAfterCurrentTask(itr);
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

	private boolean createEdge(CyNetwork network, String srcName, String tgtName, Double value) {

		CyNode src = createNode(network, srcName, SOURCE_NAME);
		CyNode tgt = createNode(network, tgtName, TARGET_NAME);
		CyEdge edge = null;

		boolean created = false;

		for (CyEdge e : network.getConnectingEdgeList(src, tgt,
				undirected ? CyEdge.Type.UNDIRECTED : CyEdge.Type.DIRECTED)) {
			if (!undirected && src == e.getSource() && tgt == e.getTarget()) {
				edge = e;
				break;
			}
		}

		if (edge == null) {
			edge = network.addEdge(src, tgt, !undirected);
			network.getRow(edge).set(CyEdge.INTERACTION, interactionName);
			network.getRow(edge).set("name", String.format("%s (%s) %s", srcName, interactionName, tgtName));
			created = true;
		}

		network.getDefaultEdgeTable().getRow(edge.getSUID()).set(columnName, value);
		return created;
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {
		if (network == null) {
			return null;
		}
		Collection<CyNetworkView> views = rm.viewManager.getNetworkViews(network);
		if (!views.isEmpty()) {
			return views.iterator().next();
		}
		CyNetworkView resView = rm.viewFactory.createNetworkView(network);

		if (createView || !views.contains(resView))
			rm.viewManager.addNetworkView(resView);

		return resView;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}

	@Override
	public CyNetwork[] getNetworks() {
		// TODO Auto-generated method stub
		return new CyNetwork[] { resultNetwork };
	}
}
