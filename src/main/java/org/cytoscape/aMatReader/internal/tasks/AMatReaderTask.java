package org.cytoscape.aMatReader.internal.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.rest.AMatReaderResult;
import org.cytoscape.aMatReader.internal.util.Delimiter;
import org.cytoscape.aMatReader.internal.util.RowNameState;
import org.cytoscape.aMatReader.internal.util.ColumnNameState;
import org.cytoscape.aMatReader.internal.util.MatrixParser;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;

public class AMatReaderTask extends AbstractTask implements CyNetworkReader, ObservableTask {
	private String columnName;
	private AMatReaderResponse response;
	private boolean createView = false;
	private CyNetwork network;

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
	public ListSingleSelection<RowNameState> headerColumn = new ListSingleSelection<RowNameState>(
			RowNameState.values());

	@Tunable(description = "Target node names in first row", groups = {
			"Advanced Options" }, params = "displayState=collapsed", gravity = 16)
	public ListSingleSelection<ColumnNameState> headerRow = new ListSingleSelection<ColumnNameState>(
			ColumnNameState.values());

	@ProvidesTitle
	public String getTitle() {
		return "Adjacency Matrix Reader";
	}

	public AMatReaderTask(final InputStream inputStream, final String columnName, final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		nodeMap = new HashMap<Object, CyNode>();
		this.columnName = columnName;
		this.rm = rm;
	}

	public AMatReaderTask(final CyNetwork network, final InputStream inputStream, final String columnName,
			final ResourceManager rm) {
		super();
		this.inputStream = inputStream;
		this.network = network;
		nodeMap = new HashMap<Object, CyNode>();
		this.columnName = columnName;
		this.rm = rm;
	}

	void createColumns() {

		CyTable nodeTable = network.getDefaultNodeTable();
		if (nodeTable.getColumn("Type") == null)
			nodeTable.createColumn("Type", String.class, false);

		CyTable edgeTable = network.getDefaultEdgeTable();

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
	public void run(TaskMonitor taskMonitor) throws NullPointerException, IOException {

		Delimiter delim = delimiter.getSelectedValue();
		RowNameState headerColumn = this.headerColumn.getSelectedValue();
		ColumnNameState headerRow = this.headerRow.getSelectedValue();
		if (delim == null){
			throw new NullPointerException("Delimiter value not recognized");
		}
		
		final MatrixParser parser = new MatrixParser(inputStream, delim, undirected, ignoreZeros, headerColumn,
				headerRow);

		//String netName = rm.naming.getSuggestedNetworkTitle(columnName);

		if (network == null) {
			network = rm.netFactory.createNetwork();
			rm.netManager.addNetwork(network);
			
			createView = true;
		} else {
			for (CyNode node : network.getNodeList()) {
				String key = network.getRow(node).get(CyNetwork.NAME, String.class);
				nodeMap.put(key, node);
			}
		}
		
		createColumns();
		for (int i = 0; i < parser.sourceCount(); i++) {
			String name = parser.getSourceName(i);
			createNode(name, SOURCE_NAME);
			
		}
		
		for (int i = 0; i < parser.targetCount(); i++) {
			String name = parser.getTargetName(i);
			createNode(name, TARGET_NAME);
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

				if (added)
					newEdgeCount++;
				else {
					updatedEdgeCount++;
				}
			}
		}

		inputStream.close();

		rm.eventHelper.flushPayloadEvents();
		
		if (createView && network.getEdgeCount() < 10000) {
			layoutNetwork(network);
		}

		taskMonitor.setProgress(1.0);
		response = new AMatReaderResponse(network.getSUID(), newEdgeCount, updatedEdgeCount);
		
	}

	private void layoutNetwork(CyNetwork network) {
		CyNetworkView view = buildCyNetworkView(network);
		CyLayoutAlgorithm algor = rm.layoutManager.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,
				null);
		insertTasksAfterCurrentTask(itr);
	}

	private CyNode createNode(String name, String type) {

		CyTable table = network.getDefaultNodeTable();

		try {
			if (table.getColumn(CyNetwork.NAME) == null)
				table.createColumn(CyNetwork.NAME, String.class, false);
		} catch (IllegalArgumentException e) {

		}
		CyNode node = null;
		Collection<CyRow> rows = null;
		try {
			rows = table.getMatchingRows(CyNetwork.NAME, name);
		} catch (NullPointerException e) {
			System.out.println("Unable to get node rows with matching name");
			return null;
		}

		if (nodeMap.containsKey(name)) {
			node = nodeMap.get(name);

		} else {
			try {
				if (rows != null && rows.size() > 1) {
					CyRow row = rows.iterator().next();
					node = network.getNode(row.get(CyNode.SUID, Long.class));
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			if (node == null) {
				node = network.addNode();

				nodeMap.put(name, node);
				network.getRow(node).set(CyNetwork.NAME, name);
			}
			network.getRow(node, CyNetwork.DEFAULT_ATTRS).set(CyNetwork.NAME, name);
			network.getRow(node, CyNetwork.DEFAULT_ATTRS).set("Type", type);
		}

		return node;
	}

	private boolean createEdge(String srcName, String tgtName, Double value) {
		CyNode src = createNode(srcName, SOURCE_NAME);
		CyNode tgt = createNode(tgtName, TARGET_NAME);
		CyEdge edge = null;

		boolean created = false;
		
		for (CyEdge e : network.getConnectingEdgeList(src, tgt, CyEdge.Type.ANY)) {
			if ((undirected && !e.isDirected())
					|| (!undirected && e.isDirected() && e.getSource() == src && e.getTarget() == tgt)) {
				edge = e;
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

	private static final String getResultString(AMatReaderResult result) {
		return "Created " + result.newEdges + " new edges and updated " + result.updatedEdges + " in network with SUID " + result.suid;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			AMatReaderResult result = response.data;
			return (R) getResultString(result);
		} else if (type.equals(AMatReaderResponse.class)) {
			return (R) response;
		} else if (type.equals(JSONResult.class)) {
			return (R)getJson(response.data);
		}
		return null;
	}

	public final static String getJson(AMatReaderResult result) {
		return "{\"newEdges\": " + result.newEdges + ", \"updatedEdges\": " + result.updatedEdges + ", \"suid\": " + result.suid + "}";
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Collections
				.unmodifiableList(Arrays.asList(String.class, AMatReaderResponse.class, JSONResult.class));
	}

	@Override
	public CyNetwork[] getNetworks() {
		// TODO Auto-generated method stub
		return new CyNetwork[] { network };
	}
}
