package org.cytoscape.aMatReader.internal.tasks;

import java.io.File;
import java.io.InputStream;

import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.work.TaskIterator;

public class AMatReaderWrapperTaskFactory extends AbstractInputStreamTaskFactory {

	final ResourceManager resourceManager;

	public AMatReaderWrapperTaskFactory(final ResourceManager resourceManager, final CyFileFilter fileFilter) {
		super(fileFilter);
		this.resourceManager = resourceManager;
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {
		if (inputName.startsWith("file:")) {
			File f = new File(inputName);
			inputName = f.getName();
		}
		return new TaskIterator(new AMatReaderWrapperTask(is, inputName, resourceManager));
	}

}
