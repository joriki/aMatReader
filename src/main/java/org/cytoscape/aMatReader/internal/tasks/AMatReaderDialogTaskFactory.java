package org.cytoscape.aMatReader.internal.tasks;

import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class AMatReaderDialogTaskFactory extends AbstractTaskFactory {
	private final ResourceManager resourceManager;

	public AMatReaderDialogTaskFactory(final ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AMatReaderDialogTask(resourceManager));
	}
}
