package org.cytoscape.aMatReader.internal;

import javax.swing.JFrame;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;

public class ResourceManager {
	public final CyNetworkFactory netFactory;
	public final CyNetworkViewFactory viewFactory;
	public final CyNetworkManager netManager;
	public final CyNetworkViewManager viewManager;
	public final CySwingAppAdapter appAdapter;
	public final CySwingApplication swingApp;
	public final CyRootNetworkManager netRootManager;
	public final CyEventHelper eventHelper;
	public final CyServiceRegistrar cyRegistrar;
	public final CyNetworkNaming naming;
	public final CyLayoutAlgorithmManager layoutManager;
	public final SynchronousTaskManager<Object> taskManager;
	public final JFrame PARENT_FRAME;

	public ResourceManager(CyServiceRegistrar cyRegistrar) {
		netFactory = cyRegistrar.getService(CyNetworkFactory.class);
		viewFactory = cyRegistrar.getService(CyNetworkViewFactory.class);
		netManager = cyRegistrar.getService(CyNetworkManager.class);
		viewManager = cyRegistrar.getService(CyNetworkViewManager.class);
		netRootManager = cyRegistrar.getService(CyRootNetworkManager.class);
		swingApp = cyRegistrar.getService(CySwingApplication.class);
		appAdapter = cyRegistrar.getService(CySwingAppAdapter.class);
		eventHelper = cyRegistrar.getService(CyEventHelper.class);
		naming = cyRegistrar.getService(CyNetworkNaming.class);
		layoutManager = cyRegistrar.getService(CyLayoutAlgorithmManager.class);

		this.taskManager = cyRegistrar.getService(SynchronousTaskManager.class);
		this.cyRegistrar = cyRegistrar;
		this.PARENT_FRAME = swingApp.getJFrame();
	}

}
