package org.cytoscape.aMatReader.internal.tasks;

import java.io.InputStream;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskIterator;

public class AMatReaderTaskFactory extends AbstractInputStreamTaskFactory {
	private final CyNetworkViewFactory viewFactory;
	private final CyNetworkFactory netFactory;
	private final CyNetworkManager netManager;
	private final CyRootNetworkManager netRootManager;
	
	public AMatReaderTaskFactory(final CyServiceRegistrar cyRegistrar, final CyFileFilter aMatFilter) {
		super(aMatFilter);
		
		viewFactory = cyRegistrar.getService(CyNetworkViewFactory.class);
		netFactory = cyRegistrar.getService(CyNetworkFactory.class);
		netManager = cyRegistrar.getService(CyNetworkManager.class);
		netRootManager = cyRegistrar.getService(CyRootNetworkManager.class);
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {
		return new TaskIterator(new AMatReaderTask(is, inputName, viewFactory, netFactory,
		                                                     netManager, netRootManager));
	}

}
