package edu.ucsf.rbvi.aMatReader.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
// Commented out until 3.2 is released
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ID;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.aMatReader.internal.tasks.AMatReaderTaskFactory;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final StreamUtil streamUtil = getService(bc, StreamUtil.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);

		// ///////////////// Readers ////////////////////////////
		final BasicCyFileFilter aMatFileFilter = new BasicCyFileFilter(new String[] { "mat" },
		                              new String[] { "application/text" }, "Adjacency Matrix Reader", DataCategory.NETWORK, streamUtil);
		final AMatReaderTaskFactory aMatReaderFactory = new AMatReaderTaskFactory(serviceRegistrar, aMatFileFilter);

		Properties aMatReaderProps = new Properties();
		aMatReaderProps.put(ID, "aMatNetworkReaderFactory");
		registerService(bc, aMatReaderFactory, InputStreamTaskFactory.class, aMatReaderProps);


	}
}
