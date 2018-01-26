package org.cytoscape.aMatReader.internal.tasks;

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

public class AMatReaderTask extends AbstractTask implements CyNetworkReader, ObservableTask {
	private String columnName;
	private AMatReaderResponse result;
	private boolean createView = false;
	private CyRootNetwork rootNetwork;
	private CyNetwork resultNetwork;

	private final Map<Object, CyNode> nodeMap;
	private final InputStream inputStream;
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

	public AMatReaderTask(final InputStream inputStream, final String name, final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		nodeMap = new HashMap<Object, CyNode>();
		columnName = name;
		this.rm = rm;
	}
	
	public AMatReaderTask(final CyNetwork network, final InputStream inputStream, final String name,
			final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		this.resultNetwork = network;
		nodeMap = new HashMap<Object, CyNode>();
		columnName = name;
		this.rm = rm;
	}
	
	
	public AMatReaderTask(final CyRootNetwork rootNetwork, final InputStream inputStream, final String name,
			final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		this.rootNetwork = rootNetwork;
		nodeMap = new HashMap<Object, CyNode>();
		columnName = name;
		this.rm = rm;
	}

	void createColumns() {

		CyTable nodeTable = rootNetwork.getSharedNodeTable();
		if (nodeTable.getColumn("Type") == null)
			nodeTable.createColumn("Type", String.class, false);

		CyTable edgeTable = rootNetwork.getSharedEdgeTable();

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

		String netName = rm.naming.getSuggestedNetworkTitle(columnName);

		if (rootNetwork == null) {
			resultNetwork = rm.netFactory.createNetwork();
			rm.netManager.addNetwork(resultNetwork);
			resultNetwork.getRow(resultNetwork).set(CyNetwork.NAME, netName);
			rootNetwork = ((CySubNetwork) resultNetwork).getRootNetwork();
			rootNetwork.getRow(rootNetwork).set(CyRootNetwork.NAME, netName + " Collection");
			createView = true;
		} else {
			resultNetwork = rootNetwork.addSubNetwork();
			for (CyNode node : rootNetwork.getNodeList()) {
				String key = rootNetwork.getRow(node).get(CyNetwork.NAME, String.class);
				nodeMap.put(key, node);
			}
			rm.netManager.addNetwork(resultNetwork);
		}

		System.out.println("Nodemap nodes");
		for (Object o : nodeMap.keySet()) {
			System.out.println(o + " " + nodeMap.get(o));
		}

		System.out.println("Net nodes");
		for (CyNode node : resultNetwork.getNodeList()) {
			System.out.println(node);
		}
		System.out.println("Net edges");
		for (CyEdge edge : resultNetwork.getEdgeList()) {
			System.out.println(edge);
		}

		System.out.println("Root nodes");
		for (CyNode node : rootNetwork.getNodeList()) {
			System.out.println(node);
		}
		System.out.println("Root edges");
		for (CyEdge edge : rootNetwork.getEdgeList()) {
			System.out.println(edge);
		}

		createColumns();
		for (int i = 0; i < parser.sourceCount(); i++) {
			createNode(parser.getSourceName(i), SOURCE_NAME);
		}
		for (int i = 0; i < parser.targetCount(); i++) {
			createNode(parser.getTargetName(i), TARGET_NAME);
		}

		int newEdgeCount = 0, updatedEdgeCount = 0;
		Map<Integer, Map<Integer, Double>> edgeMap = parser.getEdges();
		for (int src : edgeMap.keySet()) {
			Map<Integer, Double> tgtMap = edgeMap.get(src);
			String srcName = parser.getSourceName(src);
			for (int tgt : tgtMap.keySet()) {
				String tgtName = parser.getTargetName(tgt);
				Double value = tgtMap.get(tgt);
				boolean added = createEdge(srcName, tgtName, value);
				System.out.println((added ? "Adding " : "Updating") + srcName + " -> " + tgtName + " = " + value);

				if (added)
					newEdgeCount++;
				else {
					updatedEdgeCount++;
				}
			}
		}

		inputStream.close();

		rm.eventHelper.flushPayloadEvents();
		result = new AMatReaderResponse(rootNetwork.getSUID(), newEdgeCount, updatedEdgeCount);

		if (createView && resultNetwork.getEdgeCount() < 10000) {
			System.out.println(resultNetwork.getEdgeCount() < 10000);
			System.out.println(createView);

			layoutNetwork(resultNetwork);
		}

		taskMonitor.setProgress(1.0);

	}

	private void layoutNetwork(CyNetwork network) {
		CyNetworkView view = buildCyNetworkView(network);
		CyLayoutAlgorithm algor = rm.layoutManager.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,
				null);
		insertTasksAfterCurrentTask(itr);
	}

	private CyNode createNode(String name, String type) {

		CyTable table = rootNetwork.getSharedNodeTable();
		
		try {
			if (table.getColumn(CyNetwork.NAME) == null)
				table.createColumn(CyNetwork.NAME, String.class, false);
		} catch (IllegalArgumentException e) {

		}
		CyNode node = null;
		Collection<CyRow> rows = null;
		try{
			rows = table.getMatchingRows(CyNetwork.NAME, name);
		}catch(NullPointerException e){
			System.out.println("Null returned?");
		}
		
			if (nodeMap.containsKey(name)) {
				node = nodeMap.get(name);
				if (!resultNetwork.containsNode(node))
					((CySubNetwork) resultNetwork).addNode(node);
			} else {
				try {
					if (rows != null && rows.size() > 1) {
						CyRow row = rows.iterator().next();
						node = rootNetwork.getNode(row.get(CyNode.SUID, Long.class));
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				if (node == null) {
					node = rootNetwork.addNode();
					((CySubNetwork) resultNetwork).addNode(node);
					nodeMap.put(name, node);
					rootNetwork.getRow(node).set(CyNetwork.NAME, name);
				}
				rootNetwork.getRow(node, CyNetwork.DEFAULT_ATTRS).set(CyNetwork.NAME, name);
				rootNetwork.getRow(node, CyNetwork.DEFAULT_ATTRS).set("Type", type);
			}
		
		return node;
	}

	private boolean createEdge(String srcName, String tgtName, Double value) {
		CyNode src = createNode(srcName, SOURCE_NAME);
		CyNode tgt = createNode(tgtName, TARGET_NAME);
		CyEdge edge = null;

		boolean created = false;
		System.out.println("Finding " + src + " " + tgt);
		for (CyEdge e : rootNetwork.getConnectingEdgeList(src, tgt,
				undirected ? CyEdge.Type.UNDIRECTED : CyEdge.Type.DIRECTED)) {
			System.out.println(e.getSource() + " " + e.getSource());
			if (!undirected && src == e.getSource() && tgt == e.getTarget()) {
				edge = e;
				break;
			}
		}

		if (edge == null) {
			edge = rootNetwork.addEdge(src, tgt, !undirected);
			rootNetwork.getRow(edge).set(CyEdge.INTERACTION, interactionName);
			rootNetwork.getRow(edge).set("name", String.format("%s (%s) %s", srcName, interactionName, tgtName));
			created = true;
			((CySubNetwork) resultNetwork).addEdge(edge);
		}
		
		rootNetwork.getSharedEdgeTable().getRow(edge.getSUID()).set(columnName, value);
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
