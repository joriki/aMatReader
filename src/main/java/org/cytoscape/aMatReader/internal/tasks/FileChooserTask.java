package org.cytoscape.aMatReader.internal.tasks;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.cytoscape.aMatReader.internal.CyActivator;
import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class FileChooserTask extends AbstractTask {

	private final ResourceManager resourceManager;
	private final SynchronousTaskManager<?> taskManager;
	private JFileChooser chooser;

	public FileChooserTask(final ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
		taskManager = resourceManager.cyRegistrar.getService(SynchronousTaskManager.class);
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				File[] files = getMatrixFiles();
				if (files.length > 0) {
					String networkName = null;
					try {
						networkName = predictNetworkName(files);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					AMatReaderWrapperTask task = new AMatReaderWrapperTask(files, networkName, resourceManager);
					taskManager.execute(new TaskIterator(task));
				}
			}
			
		});
		
		
	}

	private String predictNetworkName(File[] files) throws Exception {
		String name;
		if (files.length == 0) {
			throw new Exception("No files provided"); // this shouldn't happen
		}

		// use directory as name if multiple files are provided
		if (files.length > 1) {
			name = files[0].getParentFile().getName();
		} else {
			name = files[0].getName();
		}
		//if (name.contains(".")) // strip extension
		//	name = name.substring(0, name.lastIndexOf("."));
		return name;
	}

	private JFileChooser getChooser() {
		if (chooser == null) {
			chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(true);

			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
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
		}
		return chooser;
	}

	private File[] getMatrixFiles() {
		int response = getChooser().showOpenDialog(CyActivator.PARENT_FRAME);
		if (response == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFiles();
		return new File[] {};
	}

}
