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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.aMatReader.internal.rest.AMatReaderParameters;
import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.util.MatrixImportDialog;
import org.cytoscape.aMatReader.internal.util.MatrixParser;
import org.cytoscape.aMatReader.internal.util.ResettableBufferedReader;
import org.cytoscape.aMatReader.internal.util.MatrixParser.MatrixParameters;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.FinishStatus.Type;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

public class AMatReaderWrapperTask extends AbstractTask implements CyNetworkReader, ObservableTask {
	private static AMatReaderResponse result;
	private static MatrixImportDialog dialog;

	private static ResettableBufferedReader reader;
	private static ArrayDeque<String> names = new ArrayDeque<String>();
	private static HashMap<String, InputStream> inputStreamMap = new HashMap<String, InputStream>();
	private CyNetwork network = null;
	String networkName = "";

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
		dialog = getDialog();
		AMatReaderParameters params = dialog.getParameters();
		network = dialog.getNetwork();
		networkName = dialog.getNetworkName();
		importMatrix(reader, dialog.getColumnName(), params);

	}

	private HashMap<String, Object> buildContext(AMatReaderParameters params) {
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

	private void importMatrix(final ResettableBufferedReader reader, final String name,
			final AMatReaderParameters params) {

		final AMatReaderTask task;
		if (network != null) {
			task = new AMatReaderTask(network, reader, name, resourceManager);
		} else {
			task = new AMatReaderTask(reader, name, resourceManager);
		}

		HashMap<String, Object> context = buildContext(params);
		resourceManager.taskManager.setExecutionContext(context);
		TaskObserver to = new TaskObserver() {

			@Override
			public void taskFinished(ObservableTask task) {
				
			}

			@Override
			public void allFinished(FinishStatus finishStatus) {
				if (finishStatus.getType() == Type.SUCCEEDED) {
					CyNetwork newNetwork = task.getNetworks()[0];
					// new network was created by import, set name
					if (network == null && newNetwork != null) {
						resourceManager.netManager.addNetwork(newNetwork);
						newNetwork.getRow(newNetwork).set(CyNetwork.NAME, networkName);
						CyRootNetwork root = ((CySubNetwork) newNetwork).getRootNetwork();
						root.getRow(root).set(CyRootNetwork.NAME, networkName);
					}

					String name = names.poll();
					updateDialog(name);
				} else {
					Exception e = finishStatus.getException();
					JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					reset();
				}
			}
		};

		resourceManager.taskManager.execute(new TaskIterator(task), to);
	}

	private void updateDialog(String name) {

		if (name != null) {
			InputStream is = inputStreamMap.get(name);
			reader = new ResettableBufferedReader(is);
			MatrixParameters prediction = new MatrixParameters();
			try {
				prediction = MatrixParser.predictParameters(reader);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			dialog.updateOptions(name, prediction, false);

		} else {
			reset();
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
					} else {
						reset();
					}
				}
			});

		}
		return dialog;
	}

	private void reset() {
		dialog.setVisible(false);
		dialog = null;
		inputStreamMap.clear();
		names.clear();
	}

	public void showDialog() {
		if (dialog == null)
			return;
		if (!dialog.isVisible()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					String name = names.poll();
					InputStream is = inputStreamMap.get(name);
					reader = new ResettableBufferedReader(is);

					MatrixParameters prediction;
					try {
						prediction = MatrixParser.predictParameters(reader);
						getDialog().updateOptions(name, prediction, true);
					} catch (IOException e) {

					}

					dialog.setVisible(true);
				}
			});
		}
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		if (dialog == null) {
			dialog = getDialog();
			showDialog();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}

	@Override
	public void cancel() {
		if (dialog != null) {
			dialog.setVisible(false);
			dialog = null;
		}
	}

	@Override
	public CyNetwork[] getNetworks() {
		return null;
	}

}
