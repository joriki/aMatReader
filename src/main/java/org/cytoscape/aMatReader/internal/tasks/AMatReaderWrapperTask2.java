package org.cytoscape.aMatReader.internal.tasks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.cytoscape.aMatReader.internal.CyActivator;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.aMatReader.internal.rest.AMatReaderParameters;
import org.cytoscape.aMatReader.internal.rest.AMatReaderResource.AMatReaderResponse;
import org.cytoscape.aMatReader.internal.util.Delimiter;
import org.cytoscape.aMatReader.internal.util.HeaderColumnFormat;
import org.cytoscape.aMatReader.internal.util.HeaderRowFormat;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class AMatReaderWrapperTask2 extends AbstractTask implements CyNetworkReader, ObservableTask {

	private AMatReaderResponse result;

	private JDialog optionsDialog;
	private JButton importButton;
	private JComboBox<Delimiter> delimiterComboBox;
	private JComboBox<HeaderColumnFormat> headerColumnComboBox;
	private JComboBox<HeaderRowFormat> headerRowComboBox;
	private JPanel optionsPanel, advancedPanel;
	private JComboBox<RootWrapper> rootNetworks;
	private JCheckBox undirectedCheckBox, ignoreZerosCheckbox;
	private JTextField interactionEntry, collectionEntry;

	private final File[] files;
	private final InputStream inputStream;
	private final String name;

	@ProvidesTitle
	public String getTitle() {
		return "Adjacency Matrix Reader";
	}

	final ResourceManager resourceManager;
	final SynchronousTaskManager<?> taskManager;

	public AMatReaderWrapperTask2(InputStream inputStream, String name, ResourceManager rm) {
		super();
		this.name = name;
		taskManager = rm.cyRegistrar.getService(SynchronousTaskManager.class);
		this.resourceManager = rm;
		files = new File[0];
		this.inputStream = inputStream;
	}

	public AMatReaderWrapperTask2(File[] files, String name, ResourceManager rm) {
		super();
		this.name = name;
		taskManager = rm.cyRegistrar.getService(SynchronousTaskManager.class);
		this.resourceManager = rm;
		this.files = files;
		this.inputStream = null;
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {

		return null;
	}

	private CyRootNetwork[] getRootNetworks() {
		HashSet<CyRootNetwork> roots = new HashSet<CyRootNetwork>();
		for (CyNetwork net : resourceManager.netManager.getNetworkSet()) {
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
			return root.getRow(root).get(CyRootNetwork.NAME, String.class);
		}
	}

	private JComboBox<Delimiter> getDelimiterComboBox() {
		if (delimiterComboBox == null) {
			delimiterComboBox = new JComboBox<Delimiter>(Delimiter.values());
			delimiterComboBox.setSelectedItem(Delimiter.TAB);
		}
		return delimiterComboBox;
	}

	private JCheckBox getUndirectedCheckBox() {
		if (undirectedCheckBox == null) {
			undirectedCheckBox = new JCheckBox("Treat edges as undirected");
			undirectedCheckBox.setSelected(false);
		}
		return undirectedCheckBox;
	}

	private JCheckBox getIgnoreZerosCheckBox() {
		if (ignoreZerosCheckbox == null) {
			ignoreZerosCheckbox = new JCheckBox("Ignore Zero Values");
			ignoreZerosCheckbox.setSelected(true);
		}
		return ignoreZerosCheckbox;
	}

	private JTextField getInteractionEntry() {
		if (interactionEntry == null) {
			interactionEntry = new JTextField("interacts with");
		}
		return interactionEntry;
	}

	private JComboBox<HeaderColumnFormat> getHeaderColumnComboBox() {

		if (headerColumnComboBox == null) {
			headerColumnComboBox = new JComboBox<HeaderColumnFormat>(HeaderColumnFormat.values());
			headerColumnComboBox.setSelectedItem(headerColumnComboBox);
		}
		return headerColumnComboBox;
	}

	private JComboBox<HeaderRowFormat> getHeaderRowComboBox() {
		if (headerRowComboBox == null) {
			headerRowComboBox = new JComboBox<HeaderRowFormat>(HeaderRowFormat.values());
			headerRowComboBox.setSelectedItem(headerRowComboBox);
		}
		return headerRowComboBox;
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
		else if (component instanceof JPanel)
			gbc.insets = new Insets(0, 0, 0, 0);
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

		}
		return rootNetworks;
	}

	private JTextField getCollectionEntry() {
		if (collectionEntry == null) {
			collectionEntry = new JTextField(name);
		}
		return collectionEntry;
	}

	private JPanel getOptionsPanel() {
		if (optionsPanel == null) {
			optionsPanel = new JPanel();
			GridBagLayout gridBagLayout = new GridBagLayout();
			gridBagLayout.columnWidths = new int[] { 100, 150 };
			gridBagLayout.rowHeights = new int[] { 24, 24, 24 };
			gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
			gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0 };
			optionsPanel.setLayout(gridBagLayout);

			addRow(optionsPanel, "Delimiter: ", getDelimiterComboBox(), 0);
			addRow(optionsPanel, "", getUndirectedCheckBox(), 1);
			addRow(optionsPanel, null, getAdvancedPanel(null), 2);
		}
		return optionsPanel;
	}

	private JPanel getAdvancedPanel(JPanel panel) {
		if (advancedPanel == null) {
			advancedPanel = new JPanel();
			GridBagLayout gridBagLayout = new GridBagLayout();
			gridBagLayout.columnWidths = new int[] { 100, 150 };
			gridBagLayout.rowHeights = new int[] { 24, 24, 24 };
			gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
			gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0 };
			advancedPanel.setLayout(gridBagLayout);

			addRow(advancedPanel, "", getIgnoreZerosCheckBox(), 0);
			addRow(advancedPanel, "Interaction Type: ", getInteractionEntry(), 1);
			addRow(advancedPanel, null, getHeaderPanel(), 2);

			Border border_out = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10),
					BorderFactory.createTitledBorder("Advanced Options"));
			Border border = BorderFactory.createCompoundBorder(border_out,
					BorderFactory.createEmptyBorder(10, 10, 10, 10));

			advancedPanel.setBorder(border);
		}

		return advancedPanel;
	}

	private JDialog getOptionsDialog() {
		if (optionsDialog == null) {
			optionsDialog = new JDialog(CyActivator.PARENT_FRAME);
			optionsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
			optionsDialog.setTitle("Import Adjacency Matrix - Advanced Options");

			optionsDialog.setLayout(new BorderLayout());
			optionsDialog.add(getOutputPanel(), BorderLayout.NORTH);
			optionsDialog.add(getOptionsPanel(), BorderLayout.CENTER);

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

	public void doImport() {

		AMatReaderParameters params = new AMatReaderParameters();
		params.delimiter = (Delimiter) delimiterComboBox.getSelectedItem();
		params.filename = collectionEntry.getText();
		params.headerColumn = (HeaderColumnFormat) headerColumnComboBox.getSelectedItem();
		params.headerRow = (HeaderRowFormat) headerRowComboBox.getSelectedItem();
		params.interactionName = interactionEntry.getText();
		params.undirected = undirectedCheckBox.isSelected();
		params.ignoreZeros = ignoreZerosCheckbox.isSelected();

		if (files.length > 0) {

			for (File f : files) {
				try {
					InputStream is = new FileInputStream(f);

					importMatrix(is, params);

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("Matrix file could not be found.");
				}

			}
		} else {
			importMatrix(inputStream, params);
		}

		/*
		 * final CyNetworkView toLayout = view; SwingUtilities.invokeLater(new
		 * Runnable(){
		 * 
		 * @Override public void run() { layoutView(toLayout); }
		 * 
		 * });
		 */

		optionsDialog.setVisible(false);
	}

	private void importMatrix(InputStream is, AMatReaderParameters params) {

		RootWrapper rootWrapper = (RootWrapper) rootNetworks.getSelectedItem();
		AMatReaderTask task;
		if (rootWrapper.root != null) {
			task = new AMatReaderTask(rootWrapper.root, is, params.filename, resourceManager);
		} else {
			task = new AMatReaderTask(is, params.filename, resourceManager);
		}

		task.headerRow.setSelectedValue(params.headerRow);
		task.headerColumn.setSelectedValue(params.headerColumn);
		task.delimiter.setSelectedValue(params.delimiter);
		task.undirected = params.undirected;
		task.ignoreZeros = params.ignoreZeros;
		task.interactionName = params.interactionName;

		// taskManager.setExecutionContext(context);
		taskManager.execute(new TaskIterator(task));

		if (rootWrapper.root == null) {
			if (task.getNetworks() != null && task.getNetworks().length > 0) {

				CyNetwork net = task.getNetworks()[0];
				resourceManager.netManager.addNetwork(net);
				task.buildCyNetworkView(net);

				CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
				rootWrapper = new RootWrapper(root);

			} else {
				System.out.println("No resulting network?");
			}
		}
	}

	private void layoutView(CyNetworkView view) {
		CyLayoutAlgorithm algor = resourceManager.layoutManager.getDefaultLayout();
		String layoutAttribute = CyNetwork.NAME;
		Object context = algor.getDefaultLayoutContext();
		TaskIterator itr = algor.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, layoutAttribute);
		taskManager.execute(itr);
	}

	private JPanel getHeaderPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Matrix Headers"));

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 100, 150 };
		gridBagLayout.rowHeights = new int[] { 24, 24 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0 };
		panel.setLayout(gridBagLayout);

		addRow(panel, "Source node header column:", getHeaderColumnComboBox(), 0);
		addRow(panel, "Target node header row: ", getHeaderRowComboBox(), 1);

		return panel;
	}

	private JPanel getOutputPanel() {
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

	private void predictParameters(File[] files) throws Exception {
		Delimiter delimiter = MatrixParser.predictDelimiter(files[0]);
		getDelimiterComboBox().setSelectedItem(delimiter);
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		System.out.println("RUNNING");
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {

				try {
					if (files.length > 0)
						predictParameters(files);
				} catch (Exception e) {
					System.out.println("Unable to predict parameters.");
				}
				getOptionsDialog().pack();
				optionsDialog.setLocationRelativeTo(CyActivator.PARENT_FRAME);
				optionsDialog.setVisible(true);

			}

		});

	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) result;
	}

	@Override
	public void cancel() {
		optionsDialog.setVisible(false);
	}

	@Override
	public CyNetwork[] getNetworks() {
		// TODO Auto-generated method stub
		return null;
	}

}
