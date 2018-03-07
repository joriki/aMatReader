package org.cytoscape.aMatReader.internal.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.aMatReader.internal.rest.AMatReaderParameters;
import org.cytoscape.aMatReader.internal.util.MatrixParser.MatrixParameters;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.swing.LookAndFeelUtil;

public class MatrixImportDialog extends JDialog {
	/**
	 * 
	 */

	private static final long serialVersionUID = 1L;
	private Vector<String> networkNames = new Vector<String>();
	private MatrixTriangleSelector matrixButtons;
	private JComboBox<Delimiter> delimiterComboBox;
	private JComboBox<String> networkComboBox;
	private JCheckBox ignoreZerosCheckBox;
	private JTextField columnNameEntry;
	private JTextField interactionEntry;
	private JCheckBox removeColumnPrefixCheckBox;
	private JPanel buttonPanel;
	private JButton okButton, cancelButton;

	private ResourceManager rm;
	private int componentCount = 0;

	private Vector<JComponent> components = new Vector<JComponent>();

	public MatrixImportDialog(ResourceManager rm) {
		super(rm != null ? rm.PARENT_FRAME : null);
		this.rm = rm;
		GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout(gridBagLayout);
		initComponents();
		double[] rowWeights = new double[componentCount + 1];
		int[] rowHeights = new int[componentCount + 1];

		for (int i = 0; i < componentCount; i++) {
			JComponent comp = components.get(i);
			rowWeights[i] = comp == null ? 1 : 0;
			rowHeights[i] = comp == null ? 20 : comp.getPreferredSize().height;
			if (comp != null)
				comp.setMinimumSize(comp.getPreferredSize());
		}

		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.columnWidths = new int[] { 100, 150 };
		gridBagLayout.rowWeights = rowWeights;
		gridBagLayout.rowHeights = rowHeights;

		pack();
		setMinimumSize(getSize());
	}

	private JComboBox<String> getNetworkComboBox() {
		if (networkComboBox == null) {
			networkComboBox = new JComboBox<String>();
			networkComboBox.setEditable(true);

			networkComboBox.getEditor().getEditorComponent().addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {

				}

				@Override
				public void keyReleased(KeyEvent e) {
					JTextField editor = (JTextField) networkComboBox.getEditor().getEditorComponent();
					String name = editor.getText();
					boolean exists = networkNames.contains(name.trim());
					Font font = networkComboBox.getFont();
					editor.setFont(new Font(font.getName(), exists ? Font.BOLD : Font.PLAIN, font.getSize()));
				}

				@Override
				public void keyPressed(KeyEvent e) {
					// TODO Auto-generated method stub

				}
			});

			networkComboBox.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					JTextField editor = (JTextField) networkComboBox.getEditor().getEditorComponent();
					String name = editor.getText();
					boolean exists = networkNames.contains(name.trim());
					Font font = networkComboBox.getFont();
					editor.setFont(new Font(font.getName(), exists ? Font.BOLD : Font.PLAIN, font.getSize()));
				}
			});
			resetNetworkComboBox(true);

		}
		return networkComboBox;
	}

	private JTextField getInteractionEntry() {
		if (interactionEntry == null) {
			interactionEntry = new JTextField("interacts with");
		}
		return interactionEntry;
	}

	private JTextField getColumnNameEntry() {
		if (columnNameEntry == null) {
			columnNameEntry = new JTextField();
		}
		return columnNameEntry;
	}

	private JCheckBox getRemoveColumnPrefixCheckBox() {
		if (removeColumnPrefixCheckBox == null) {
			removeColumnPrefixCheckBox = new JCheckBox("Remove column prefix");
			removeColumnPrefixCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
		}
		return removeColumnPrefixCheckBox;
	}

	private void resetNetworkComboBox(boolean changeVal) {
		String name = "New network";
		if (!changeVal) {
			name = (String) networkComboBox.getSelectedItem();
		}
		networkComboBox.removeAllItems();
		networkNames.clear();
		if (rm != null) {
			for (CyNetwork net : rm.netManager.getNetworkSet()) {
				String netName = net.getRow(net).get(CyNetwork.NAME, String.class);
				networkNames.add(netName);
				networkComboBox.addItem(netName);
			}

			if (changeVal)
				name = rm.naming.getSuggestedNetworkTitle(name);
		}
		networkComboBox.setSelectedItem(name);
	}

	private JCheckBox getIgnoreZerosCheckBox() {
		if (ignoreZerosCheckBox == null) {
			ignoreZerosCheckBox = new JCheckBox("Ignore zero values", true);
		}
		return ignoreZerosCheckBox;
	}

	private void addRow(String s, JComponent component) {
		GridBagConstraints gbc = new GridBagConstraints();
		int x = 0;
		if (s != null) {
			JLabel label = new JLabel(s);
			label.setLabelFor(component);
			gbc.insets = new Insets(0, 50, 5, 0);
			gbc.gridx = x++;
			gbc.gridy = componentCount;
			gbc.anchor = GridBagConstraints.EAST;
			add(label, gbc);
			gbc.anchor = GridBagConstraints.WEST;
		} else {
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.gridwidth = 2;
		}
		Insets insets = new Insets(0, 0, 5, 0);
		if (component instanceof JButton) {
			insets.left = 5;
			insets.right = 5;
		} else if (s != null)
			insets.right = 50;

		gbc.insets = insets;

		gbc.gridx = x;
		gbc.gridy = componentCount;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(component, gbc);
		componentCount++;
		components.add(component);
	}

	private void initComponents() {
		addRow("Network: ", getNetworkComboBox());
		addRow("New column name: ", getColumnNameEntry());
		addRow("Delimiter: ", getDelimiterComboBox());
		addRow("Interaction type: ", getInteractionEntry());
		addRow("", getIgnoreZerosCheckBox());
		JLabel label = new JLabel("Select the matrix area(s) to import");
		label.setHorizontalAlignment(JLabel.CENTER);
		addRow(null, label);
		addRow(null, getMatrixButtons());
		addRow(null, getRemoveColumnPrefixCheckBox());
		componentCount++;
		components.add(null);
		addRow(null, getButtonPanel());
	}

	public void addActionListener(ActionListener listener) {
		getOkButton().addActionListener(listener);
		getCancelButton().addActionListener(listener);
	}

	public JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton("Import");
			getRootPane().setDefaultButton(okButton);
		}
		return okButton;
	}

	public JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
		}
		return cancelButton;
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = LookAndFeelUtil.createOkCancelPanel(getOkButton(), getCancelButton());
		}
		return buttonPanel;
	}

	private JComboBox<Delimiter> getDelimiterComboBox() {
		if (delimiterComboBox == null) {
			delimiterComboBox = new JComboBox<Delimiter>(Delimiter.values());
		}
		return delimiterComboBox;
	}

	private MatrixTriangleSelector getMatrixButtons() {
		if (matrixButtons == null) {
			matrixButtons = new MatrixTriangleSelector(true, true, false);
			matrixButtons.setPreferredSize(new Dimension(200, 200));
			matrixButtons.setMinimumSize(new Dimension(50, 150));
		}
		return matrixButtons;
	}

	public AMatReaderParameters getParameters() {
		AMatReaderParameters params = new AMatReaderParameters();
		params.delimiter = (Delimiter) getDelimiterComboBox().getSelectedItem();
		params.ignoreZeros = getIgnoreZerosCheckBox().isSelected();
		params.interactionName = getInteractionEntry().getText();
		params.undirected = getMatrixButtons().isUndirected();
		params.removeColumnPrefix = getRemoveColumnPrefixCheckBox().isSelected();
		params.rowNames = getMatrixButtons().hasRowNames();
		params.columnNames = getMatrixButtons().hasColumnNames();
		return params;
	}

	public String getColumnName() {
		return getColumnNameEntry().getText();
	}

	public String getNetworkName() {
		return (String) getNetworkComboBox().getSelectedItem();
	}

	public CyNetwork getNetwork() {
		for (CyNetwork network : rm.netManager.getNetworkSet()) {
			String name = (String) networkComboBox.getEditor().getItem();
			if (network.getRow(network).get(CyNetwork.NAME, String.class).equals(name))
				return network;
		}
		return null;

	}

	public void updateOptions(String name, MatrixParameters prediction, boolean newCollection) {
		getMatrixButtons().setButtons(prediction.hasRowNames, prediction.hasColumnNames, null);
		getDelimiterComboBox().setSelectedItem(prediction.delimiter);
		getColumnNameEntry().setText(name);
		boolean prefixed = !prediction.columnPrefix.isEmpty();
		JCheckBox cb = getRemoveColumnPrefixCheckBox();
		cb.setVisible(prefixed);
		cb.setSelected(prefixed);
		if (prefixed) {
			cb.setText("Remove column prefix: '" + prediction.columnPrefix + "'");
		} else {
			cb.setSelected(false);
		}
		resetNetworkComboBox(false);
		if (newCollection) {
			getNetworkComboBox().setSelectedItem(name);
		}
	}

}
