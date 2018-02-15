package org.cytoscape.aMatReader.internal.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.HashMap;

import javax.swing.SwingUtilities;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.aMatReader.internal.rest.AMatReaderParameters;
import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.util.MatrixImportDialog;
import org.cytoscape.aMatReader.internal.util.MatrixParser;
import org.cytoscape.aMatReader.internal.util.MatrixParser.MatrixParameterPrediction;
import org.cytoscape.aMatReader.internal.util.ResettableBufferedReader;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class AMatReaderWrapperTask extends AbstractTask implements CyNetworkReader, ObservableTask {
	private static AMatReaderResponse result;
	private static MatrixImportDialog dialog;
	private static boolean open = false;

	private static ResettableBufferedReader reader;
	private static ArrayDeque<String> names = new ArrayDeque<String>();
	private static HashMap<String, InputStream> inputStreamMap = new HashMap<String, InputStream>();

	@ProvidesTitle
	public String getTitle() {
		return "Adjacency Matrix Reader";
	}

	final ResourceManager resourceManager;

	public AMatReaderWrapperTask(InputStream inputStream, String name, ResourceManager rm) {
		super();
		this.resourceManager = rm;

		if (name.startsWith("file:")) {
			try {
				File f = new File(name.substring(5, name.length()));
				inputStream = new FileInputStream(f);
				name = f.getName();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			name = resourceManager.naming.getSuggestedNetworkTitle(name);
		}

		inputStreamMap.put(name, inputStream);
		names.add(name);
	}

	public AMatReaderWrapperTask(File[] files, String name, ResourceManager rm) throws FileNotFoundException {
		super();
		this.resourceManager = rm;
		for (File f : files) {
			inputStreamMap.put(f.getName(), new FileInputStream(f));
			names.add(f.getName());
		}
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {

		return null;
	}

	public void doImport(ResettableBufferedReader reader) {
		MatrixImportDialog dialog = getDialog();
		AMatReaderParameters params = dialog.getParameters();
		CyNetwork net = dialog.getNetwork();
		boolean newNetwork = net == null;

		net = importMatrix(net, reader, dialog.getColumnName(), params);

		if (newNetwork) {
			String name = dialog.getNetworkName();
			// String name =
			// resourceManager.naming.getSuggestedNetworkTitle(networkName);
			net.getRow(net).set(CyNetwork.NAME, name);
			CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
			root.getRow(root).set(CyRootNetwork.NAME, name);
		}

	}
	private HashMap<String, Object> buildContext(AMatReaderParameters params){
		HashMap<String, Object> context = new HashMap<String, Object>();
		
		context.put("delimiter", params.delimiter);
		context.put("ignoreZeros", params.ignoreZeros);
		context.put("interactionName", params.interactionName);
		context.put("removeColumnPrefix", params.removeColumnPrefix);
		context.put("undirected", params.undirected);
		context.put("rowNames", params.rowNames);
		context.put("columnNames", params.columnNames);
		
		return context;
	}

	private CyNetwork importMatrix(CyNetwork network, ResettableBufferedReader reader, String name,
			AMatReaderParameters params) {

		AMatReaderTask task;
		if (network != null) {
			task = new AMatReaderTask(network, reader, name, resourceManager);
		} else {
			task = new AMatReaderTask(reader, name, resourceManager);
		}

		HashMap<String, Object> context = buildContext(params);
		resourceManager.taskManager.setExecutionContext(context);
		resourceManager.taskManager.execute(new TaskIterator(task));

		if (network == null) {
			if (task.getNetworks() != null && task.getNetworks().length > 0) {

				CyNetwork net = task.getNetworks()[0];
				resourceManager.netManager.addNetwork(net);
				network = net;

			} else {
				System.out.println("No resulting network?");
			}
		}
		return network;
	}

	private void updateDialog(String name) {
		if (name != null) {
			InputStream is = inputStreamMap.get(name);
			reader = new ResettableBufferedReader(is);
			MatrixParameterPrediction prediction = new MatrixParameterPrediction();
			try {
				prediction = MatrixParser.predictParameters(reader);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			dialog.updateOptions(name, prediction);

		} else {
			open = false;
			dialog.setVisible(false);
			names.clear();
			inputStreamMap.clear();
		}
	}

	private MatrixImportDialog getDialog() {
		if (dialog == null) {
			dialog = new MatrixImportDialog(resourceManager);

			dialog.setSize(400, 600);
			dialog.pack();
			dialog.setLocationRelativeTo(resourceManager.PARENT_FRAME);

			dialog.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == dialog.getOkButton()) {
						doImport(reader);
						updateDialog(names.poll());
					} else {
						dialog.setVisible(false);
						open = false;
					}
				}
			});

		}
		return dialog;
	}

	public void showDialog() {
		if (!open) {
			open = true;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					String name = names.pop();
					InputStream is = inputStreamMap.get(name);
					reader = new ResettableBufferedReader(is);

					MatrixParameterPrediction prediction;
					try {
						prediction = MatrixParser.predictParameters(reader);
						getDialog().updateOptions(name, prediction);
					} catch (IOException e) {

					}

					getDialog().setVisible(true);
				}
			});
		}
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		if (inputStreamMap.isEmpty()) {
			open = false;
			return;
		}

		showDialog();

	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}

	@Override
	public void cancel() {
		if (dialog != null)
			dialog.setVisible(false);
	}

	@Override
	public CyNetwork[] getNetworks() {
		return null;
	}

}
