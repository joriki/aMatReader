package org.cytoscape.aMatReader.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
// Commented out until 3.2 is released
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ID;
import java.util.Properties;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderTaskFactory;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final StreamUtil streamUtil = getService(bc, StreamUtil.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);

		SynchronousTaskManager<?> taskManager = getService(bc, SynchronousTaskManager.class);

		CIResponseFactory ciResponseFactory = this.getService(bc, CIResponseFactory.class);
		CIErrorFactory ciErrorFactory = this.getService(bc, CIErrorFactory.class);

		// ///////////////// Readers ////////////////////////////
		final BasicCyFileFilter aMatFileFilter = new BasicCyFileFilter(new String[] { "mat", "adj" },
				new String[] { "application/text" }, "Adjacency Matrix Reader", DataCategory.NETWORK, streamUtil);
		final AMatReaderTaskFactory aMatReaderFactory = new AMatReaderTaskFactory(serviceRegistrar, aMatFileFilter);

		Properties aMatReaderProps = new Properties();
		aMatReaderProps.put(ID, "aMatNetworkReaderFactory");
		aMatReaderProps.setProperty(COMMAND_NAMESPACE, "network");
		aMatReaderProps.setProperty(COMMAND, "import");
		aMatReaderProps.setProperty(COMMAND_DESCRIPTION, "Import a network from an adjacency matrix file");
		//registerService(bc, aMatReaderFactory, InputStreamTaskFactory.class, aMatReaderProps);

		AMatReaderResource resource = new AMatReaderResource(taskManager, aMatReaderFactory, ciResponseFactory,
				ciErrorFactory);
		registerService(bc, resource, AMatReaderResource.class, new Properties());
	}
}
