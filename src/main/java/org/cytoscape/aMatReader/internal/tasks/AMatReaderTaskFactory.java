package org.cytoscape.aMatReader.internal.tasks;

import java.io.InputStream;

import org.cytoscape.aMatReader.internal.ResourceManager;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.work.TaskIterator;

public class AMatReaderTaskFactory extends AbstractInputStreamTaskFactory {
	private final ResourceManager resourceManager;

	public AMatReaderTaskFactory(final ResourceManager resourceManager, final CyFileFilter aMatFilter) {
		super(aMatFilter);
		this.resourceManager = resourceManager;
	}

	@Override
	public TaskIterator createTaskIterator(final InputStream is, final String inputName) {
		final TaskIterator ti = new TaskIterator();
		AMatReaderTask2 task = new AMatReaderTask2(is, inputName, resourceManager);
		ti.append(task);
		//ti.insertTasksAfter(task, new GenerateLayoutTask(task, resourceManager));
		return ti;
	}

}
