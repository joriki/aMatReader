package org.cytoscape.aMatReader.internal;

import static org.cytoscape.work.ServiceProperties.ID;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import javax.swing.JFrame;

import org.cytoscape.aMatReader.internal.rest.AMatReaderResource;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderDialogAction;
import org.cytoscape.aMatReader.internal.tasks.AMatReaderTaskFactory;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {
	public static JFrame PARENT_FRAME;
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final StreamUtil streamUtil = getService(bc, StreamUtil.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		CySwingApplication swingApp = getService(bc, CySwingApplication.class);
		PARENT_FRAME = swingApp.getJFrame();
		
		// Register AMatReader input stream with file filter
		// Register CyAction menu item that opens the dialog
		final BasicCyFileFilter aMatFileFilter = new BasicCyFileFilter(new String[] { "mat", "adj"},
				new String[] { "application/text" }, "Adjacency Matrix Reader", DataCategory.NETWORK, streamUtil);
		AMatReaderTaskFactory readerTF = new AMatReaderTaskFactory(serviceRegistrar, aMatFileFilter);
		registerService(bc, readerTF, InputStreamTaskFactory.class);
		
		Properties aMatReaderDialogProps = new Properties();
		aMatReaderDialogProps.put(ID, "aMatReaderDialogFactory");
		aMatReaderDialogProps.put(PREFERRED_MENU, "aMatReader");
		aMatReaderDialogProps.put(TITLE, "Import Adjacenct Matrices");
		aMatReaderDialogProps.put(MENU_GRAVITY,  10.0);
		aMatReaderDialogProps.put(IN_MENU_BAR, true);
		
		AMatReaderDialogAction dialogAction = new AMatReaderDialogAction(serviceRegistrar);
		registerService(bc, dialogAction, CyAction.class, aMatReaderDialogProps);
		
		AMatReaderResource resource = new AMatReaderResource(serviceRegistrar, readerTF);
		registerService(bc, resource, AMatReaderResource.class);
		
	}
}
