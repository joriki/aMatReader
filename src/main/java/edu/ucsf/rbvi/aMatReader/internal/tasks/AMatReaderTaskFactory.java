package edu.ucsf.rbvi.aMatReader.internal.tasks;

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
	public final CyFileFilter aMatFilter;
	public final CyServiceRegistrar cyRegistrar;

	public AMatReaderTaskFactory(final CyServiceRegistrar cyRegistrar, final CyFileFilter aMatFilter) {
		super(aMatFilter);
		this.aMatFilter = aMatFilter;
		this.cyRegistrar = cyRegistrar;
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {
		CyNetworkViewFactory viewFactory = cyRegistrar.getService(CyNetworkViewFactory.class);
		CyNetworkFactory netFactory = cyRegistrar.getService(CyNetworkFactory.class);
		CyNetworkManager netManager = cyRegistrar.getService(CyNetworkManager.class);
		CyRootNetworkManager netRootManager = cyRegistrar.getService(CyRootNetworkManager.class);

		TaskIterator ti = new TaskIterator(new AMatReaderTask(is, inputName, viewFactory, netFactory,
		                                                     netManager, netRootManager));
		return ti;
	}

}
