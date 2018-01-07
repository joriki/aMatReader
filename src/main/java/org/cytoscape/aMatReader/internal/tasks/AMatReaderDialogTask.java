package org.cytoscape.aMatReader.internal.tasks;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;

import org.cytoscape.aMatReader.internal.CyActivator;
import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.rest.ColumnHeadersFormat;
import org.cytoscape.aMatReader.internal.rest.Delimiter;
import org.cytoscape.aMatReader.internal.rest.RowHeadersFormat;
import org.cytoscape.app.swing.AbstractCySwingApp;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.util.ListSingleSelection;

public class AMatReaderDialogTask extends AbstractCySwingApp implements ObservableTask {
	private AMatReaderResponse result;

	public CyNetwork finishedNetwork = null;

	private JFileChooser chooser;
	private JDialog optionsDialog;
	private JButton importButton;
	private JComboBox<Delimiter> delimiterComboBox;
	private JComboBox<RowHeadersFormat> rowHeaderComboBox;
	private JComboBox<ColumnHeadersFormat> columnHeaderComboBox;

	private JComboBox<RootWrapper> rootNetworks;
	private JCheckBox undirectedCheckBox;
	private JTextField interactionEntry, collectionEntry;

	private String collectionName = "";
	private Delimiter delimiter = Delimiter.TAB;
	private boolean undirected = false;
	private String interactionName = "interacts with";
	private RowHeadersFormat rowHeader = RowHeadersFormat.NAMES;
	private ColumnHeadersFormat columnHeader = ColumnHeadersFormat.NAMES;

	@ProvidesTitle
	public String getTitle() {
		return "Adjacency Matrix Reader";
	}

	final CyNetworkViewFactory viewFactory;
	final CyNetworkFactory netFactory;
	final CyNetworkManager netManager;
	final CyRootNetworkManager rootManager;
	final SynchronousTaskManager<?> taskManager;

	private CyRootNetwork[] getRootNetworks() {
		HashSet<CyRootNetwork> roots = new HashSet<CyRootNetwork>();
		for (CyNetwork net : netManager.getNetworkSet()) {
			roots.add(((CySubNetwork) net).getRootNetwork());
		}
		CyRootNetwork rootArray[] = new CyRootNetwork[roots.size()];
		roots.toArray(rootArray);
		return rootArray;
	}

	private class RootWrapper {
		private CyRootNetwork root;

		private RootWrapper(CyRootNetwork root) {
			this.root = root;
		}

		public String toString() {
			if (root == null) {
				return "-- Create new network collection --";
			}
			return root.getDefaultNetworkTable().getRow(root.getSUID()).get(CyRootNetwork.NAME, String.class);
		}
	}

	public AMatReaderDialogTask(final CySwingAppAdapter cySwingAppAdapter, final CySwingApplication swingApp,
			final CyNetworkViewFactory cyNetworkViewFactory, final CyNetworkFactory cyNetworkFactory,
			final CyNetworkManager cyNetworkManager, final CyRootNetworkManager cyRootNetworkManager,
			final SynchronousTaskManager<?> taskManager) {
		super(cySwingAppAdapter);

		rootManager = cyRootNetworkManager;
		netManager = cyNetworkManager;
		netFactory = cyNetworkFactory;
		viewFactory = cyNetworkViewFactory;
		this.taskManager = taskManager;
	}

	private JComboBox<Delimiter> getDelimiterComboBox() {
		if (delimiterComboBox == null) {
			delimiterComboBox = new JComboBox<Delimiter>(Delimiter.values());
			delimiterComboBox.setSelectedItem(delimiter);
		}
		return delimiterComboBox;
	}

	private JCheckBox getUndirectedCheckBox() {
		if (undirectedCheckBox == null) {
			undirectedCheckBox = new JCheckBox("Treat edges as undirected");
			undirectedCheckBox.setSelected(undirected);
		}
		return undirectedCheckBox;
	}

	private JTextField getInteractionEntry() {
		if (interactionEntry == null) {
			interactionEntry = new JTextField(interactionName);
		}
		return interactionEntry;
	}

	private JComboBox<RowHeadersFormat> getRowHeaderComboBox() {

		if (rowHeaderComboBox == null) {
			rowHeaderComboBox = new JComboBox<RowHeadersFormat>(RowHeadersFormat.values());
			rowHeaderComboBox.setSelectedItem(rowHeaderComboBox);
		}
		return rowHeaderComboBox;
	}

	private JComboBox<ColumnHeadersFormat> getColumnHeaderComboBox() {
		if (columnHeaderComboBox == null) {
			columnHeaderComboBox = new JComboBox<ColumnHeadersFormat>(ColumnHeadersFormat.values());
			columnHeaderComboBox.setSelectedItem(columnHeaderComboBox);
		}
		return columnHeaderComboBox;
	}

	private File[] getMatrixFiles() {
		chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);

		// chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// chooser.setControlButtonsAreShown(false);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileFilter(new FileFilter() {
			String[] suffixes = new String[] { ".mat", ".adj", ".txt", ".tsv", ".csv" };

			@Override
			public String getDescription() {
				return String.format("Adjacency Matrices (%s)", String.join(", ", suffixes));
			}

			@Override
			public boolean accept(File f) {
				String name = f.getName();
				for (String suf : suffixes)
					if (name.endsWith(suf))
						return true;
				return f.isDirectory();
			}
		});

		chooser.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		chooser.setApproveButtonText("Import files");
		int response = chooser.showOpenDialog(CyActivator.PARENT_FRAME);
		if (response == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFiles();
		return new File[] {};
	}

	private String generateNewCollectionName(File[] files) {
		if (files.length == 0)
			return null;
		if (files.length > 1) {
			return files.length + " matrix files";
		}
		String name = files[0].getName();
		if (name.contains("."))
			name = name.substring(0, name.lastIndexOf("."));
		return name;
	}

	private void addRow(JPanel panel, String s, JComponent component, int row) {
		GridBagConstraints gbc = new GridBagConstraints();
		int x = 0;
		if (s != null) {
			JLabel label = new JLabel(s);
			label.setLabelFor(component);
			gbc.insets = new Insets(0, 0, 5, 5);
			gbc.gridx = x++;
			gbc.gridy = row;
			panel.add(label, gbc);
		} else {
			gbc.gridwidth = 2;
		}
		if (component instanceof JButton)
			gbc.insets = new Insets(0, 10, 5, 10);
		else
			gbc.insets = new Insets(0, 0, 5, 20);
		gbc.gridx = x;
		gbc.gridy = row;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, gbc);
	}

	private JButton getImportButton() {
		if (importButton == null) {
			importButton = new JButton("Import");
			importButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					collectionName = collectionEntry.getText();
					undirected = undirectedCheckBox.isSelected();
					interactionName = interactionEntry.getText();
					delimiter = (Delimiter) delimiterComboBox.getSelectedItem();
					rowHeader = (RowHeadersFormat) rowHeaderComboBox.getSelectedItem();
					columnHeader = (ColumnHeadersFormat) columnHeaderComboBox.getSelectedItem();
					doImport();
				}
			});
		}
		return importButton;
	}

	private JComboBox<RootWrapper> getRootNetworkComboBox() {
		if (rootNetworks == null) {
			rootNetworks = new JComboBox<RootWrapper>();
			rootNetworks.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					boolean nullRoot = ((RootWrapper) e.getItem()).root == null;
					getCollectionEntry().setEnabled(nullRoot);

				}
			});

			rootNetworks.addItem(new RootWrapper(null));
			for (CyRootNetwork root : getRootNetworks()) {
				rootNetworks.addItem(new RootWrapper(root));
			}
			// rootNetworks.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
			// 20));

		}
		return rootNetworks;
	}

	private JTextField getCollectionEntry() {
		if (collectionEntry == null) {
			collectionEntry = new JTextField(collectionName);
		}
		return collectionEntry;
	}

	private JPanel createOptionsPanel(JPanel panel1) {
		if (panel1 == null)
			panel1 = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 100, 150 };
		gridBagLayout.rowHeights = new int[] { 24, 24, 24, 24, 24, 20 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		panel1.setLayout(gridBagLayout);

		addRow(panel1, "Delimiter: ", getDelimiterComboBox(), 0);
		addRow(panel1, "", getUndirectedCheckBox(), 1);
		addRow(panel1, "Interaction Type: ", getInteractionEntry(), 2);
		addRow(panel1, "Column Headers: ", getColumnHeaderComboBox(), 3);
		addRow(panel1, "Row Headers: ", getRowHeaderComboBox(), 4);

		return panel1;
	}

	private JDialog getOptionsDialog() {
		if (optionsDialog == null) {
			optionsDialog = new JDialog(CyActivator.PARENT_FRAME);
			optionsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
			optionsDialog.setTitle("Import Adjacency Matrix - Advanced Options");
			JPanel panel = createOptionsPanel(null);
			Border border_out = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10),
					BorderFactory.createTitledBorder("Adjacency Matrix Options"));
			Border border = BorderFactory.createCompoundBorder(border_out,
					BorderFactory.createEmptyBorder(10, 10, 10, 10));
			panel.setBorder(border);
			optionsDialog.setLayout(new BorderLayout());
			optionsDialog.add(getOutputPanel(), BorderLayout.NORTH);
			optionsDialog.add(panel, BorderLayout.CENTER);

			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					optionsDialog.setVisible(false);
				}
			});
			JPanel buttonPanel = LookAndFeelUtil.createOkCancelPanel(getImportButton(), cancelBtn);
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			optionsDialog.add(buttonPanel, BorderLayout.SOUTH);
			optionsDialog.setMinimumSize(new Dimension(300, 100));
			optionsDialog.pack();
		}

		return optionsDialog;
	}

	public Map<String, Object> buildContext() {
		Map<String, Object> context = new HashMap<String, Object>();

		ListSingleSelection<Delimiter> delim = new ListSingleSelection<Delimiter>(Delimiter.values());
		delim.setSelectedValue(delimiter);
		ListSingleSelection<ColumnHeadersFormat> cHeader = new ListSingleSelection<ColumnHeadersFormat>(
				ColumnHeadersFormat.values());
		cHeader.setSelectedValue(columnHeader);
		ListSingleSelection<RowHeadersFormat> rHeader = new ListSingleSelection<RowHeadersFormat>(
				RowHeadersFormat.values());
		rHeader.setSelectedValue(rowHeader);

		context.put("delimiter", delim);
		context.put("undirected", undirected);
		context.put("interactionName", interactionName);
		context.put("rowHeader", rHeader);
		context.put("columnHeader", cHeader);
		return context;
	}

	public void doImport() {
		File[] files = chooser.getSelectedFiles();

		RootWrapper rootWrapper = (RootWrapper) rootNetworks.getSelectedItem();
		// Map<String, Object> context = buildContext();

		for (File f : files) {
			try {
				InputStream is = new FileInputStream(f);

				AMatReaderTask task;
				if (rootWrapper.root != null) {
					task = new AMatReaderTask(rootWrapper.root, is, f.getName(), viewFactory, netFactory, netManager,
							rootManager);
				} else {
					task = new AMatReaderTask(is, f.getName(), viewFactory, netFactory, netManager, rootManager);
				}

				task.columnHeaders.setSelectedValue(columnHeader);
				task.rowHeaders.setSelectedValue(rowHeader);
				// task.delimiter.setSelectedValue(delimiter);
				task.interactionName = interactionName;
				task.undirected = undirected;

				// taskManager.setExecutionContext(context);
				taskManager.execute(new TaskIterator(task));
				if (rootWrapper.root == null) {
					if (task.getNetworks() == null) {
						System.out.println("No network created");
						return;
					} else if (task.getNetworks().length == 0) {
						System.out.println("No network created");
						return;
					}
					CyNetwork net = task.getNetworks()[0];
					netManager.addNetwork(net);
					task.buildCyNetworkView(net);
					CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
					root.getDefaultNetworkTable().getRow(root.getSUID()).set(CyNetwork.NAME, collectionName);
					rootWrapper = new RootWrapper(root);
					if (files.length > 1) {
						net.getDefaultNetworkTable().getRow(net.getSUID()).set(CyNetwork.NAME, collectionName);
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Matrix file could not be found.");
			}
		}

		optionsDialog.setVisible(false);
	}

	public JPanel getOutputPanel() {
		JPanel outputPanel = new JPanel();
		outputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 100, 150 };
		gridBagLayout.rowHeights = new int[] { 24, 24 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0 };
		outputPanel.setLayout(gridBagLayout);

		addRow(outputPanel, "Network:", getRootNetworkComboBox(), 0);
		addRow(outputPanel, "New Collection Name", getCollectionEntry(), 1);

		return outputPanel;
	}

	private void predictParameters(File[] files) {
		collectionName = generateNewCollectionName(files);
		try {
			delimiter = MatrixParser.predictDelimiter(files[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		File[] files = getMatrixFiles();
		if (files.length == 0)
			return;

		predictParameters(files);

		getOptionsDialog().pack();
		optionsDialog.setLocationRelativeTo(CyActivator.PARENT_FRAME);
		optionsDialog.setVisible(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		optionsDialog.setVisible(false);
	}
}
