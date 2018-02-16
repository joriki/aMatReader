package org.cytoscape.aMatReader.internal.tasks;

import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class FileChooserTaskFactory extends AbstractTaskFactory {

	final ResourceManager resourceManager;

	public FileChooserTaskFactory(final ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new FileChooserTask(resourceManager));
	}

}
