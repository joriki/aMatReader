package org.cytoscape.aMatReader.internal;

import static org.cytoscape.work.ServiceProperties.*;

import java.util.Properties;
import javax.swing.JFrame;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResourceImpl;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderWrapperTaskFactory;
import org.cytoscape.aMatReader.internal.tasks.FileChooserTaskFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
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
		Properties menuProps = new Properties();
		menuProps.setProperty(PREFERRED_MENU, AMATREADER_MENU);
		menuProps.setProperty(TITLE, AMATREADER_ACTION);
		menuProps.setProperty(IN_MENU_BAR, "true");
		menuProps.setProperty(MENU_GRAVITY, "5.0");

		FileChooserTaskFactory menuTF = new FileChooserTaskFactory(resourceManager);
		registerAllServices(bc, menuTF, menuProps);

		// drag and drop import
		final BasicCyFileFilter aMatFileFilter = new BasicCyFileFilter(new String[] { "mat", "adj" },
				new String[] { "application/text" }, "Adjacency Matrix Reader", DataCategory.NETWORK, streamUtil);
		AMatReaderWrapperTaskFactory wrapperService = new AMatReaderWrapperTaskFactory(resourceManager, aMatFileFilter);

		Properties wrapperProps = new Properties();
		wrapperProps.setProperty("readerDescription", "Matrix reader");
		wrapperProps.setProperty("readerId", "aMatReader");
		wrapperProps.setProperty(IN_MENU_BAR, "false");
		registerAllServices(bc, wrapperService, wrapperProps);

		AMatReaderResourceImpl restService = new AMatReaderResourceImpl(serviceRegistrar, resourceManager);
		registerService(bc, restService, AMatReaderResourceImpl.class);
	}

	@Override
	public void shutDown() {
		super.shutDown();
		PARENT_FRAME = null;
	}
}
