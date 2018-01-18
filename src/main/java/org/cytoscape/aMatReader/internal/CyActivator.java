package org.cytoscape.aMatReader.internal;

import static org.cytoscape.work.ServiceProperties.*;

import java.util.Properties;
import javax.swing.JFrame;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResourceImpl;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderDialogTaskFactory;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderTaskFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {
	public static JFrame PARENT_FRAME;
	private static final String AMATREADER_MENU = "Apps.AMatReader";
	private static final String AMATREADER_ACTION = "Import Matrix Files";

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		final ResourceManager resourceManager = new ResourceManager(serviceRegistrar);
		final StreamUtil streamUtil = getService(bc, StreamUtil.class);
		final CySwingApplication swingApp = getService(bc, CySwingApplication.class);

		PARENT_FRAME = swingApp.getJFrame();
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, AMATREADER_MENU);
		props.setProperty(TITLE, AMATREADER_ACTION);
		props.setProperty(IN_MENU_BAR, "true");
		props.setProperty(MENU_GRAVITY, "5.0");

		AMatReaderDialogTaskFactory dialogTF = new AMatReaderDialogTaskFactory(resourceManager);
		//registerService(bc, dialogTF, AMatReaderDialogTaskFactory.class, props);
		registerAllServices(bc, dialogTF, props);

		// drag and drop import
		final BasicCyFileFilter aMatFileFilter = new BasicCyFileFilter(new String[] { "mat", "adj" },
				new String[] { "application/text" }, "Adjacency Matrix Reader", DataCategory.NETWORK, streamUtil);
		AMatReaderTaskFactory readerTF = new AMatReaderTaskFactory(resourceManager, aMatFileFilter);
		Properties readerProps = new Properties();
		readerProps.setProperty("readerDescription", "Matrix reader");
		readerProps.setProperty("readerId", "aMatReader");
		readerProps.setProperty(IN_MENU_BAR, "false");
		//registerService(bc, readerTF, AMatReaderTaskFactory.class, readerProps);
		registerAllServices(bc,readerTF, readerProps);
		
		AMatReaderResourceImpl resource = new AMatReaderResourceImpl(serviceRegistrar, readerTF);
		registerService(bc, resource, AMatReaderResourceImpl.class);
	}

	@Override
	public void shutDown() {
		super.shutDown();
		PARENT_FRAME = null;
	}
}
