package org.cytoscape.aMatReader.internal.tasks;

import java.awt.event.ActionEvent;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;


public class AMatReaderDialogAction extends AbstractCyAction {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final CySwingAppAdapter appAdapter;
	private final CySwingApplication swingApp;
	private final SynchronousTaskManager<AMatReaderDialogTask> taskMgr;
	private final CyNetworkViewFactory viewFactory;
	private final CyNetworkFactory netFactory;
	private final CyNetworkManager netManager;
	private final CyRootNetworkManager netRootManager;

	@SuppressWarnings("unchecked")
	public AMatReaderDialogAction(final CyServiceRegistrar cyRegistrar) {
		super("Import Adjacency Matrices");
		setPreferredMenu("Apps.aMatReader");
		viewFactory = cyRegistrar.getService(CyNetworkViewFactory.class);
		netFactory = cyRegistrar.getService(CyNetworkFactory.class);
		netManager = cyRegistrar.getService(CyNetworkManager.class);
		netRootManager = cyRegistrar.getService(CyRootNetworkManager.class);
		swingApp = cyRegistrar.getService(CySwingApplication.class);
		taskMgr = cyRegistrar.getService(SynchronousTaskManager.class);
		appAdapter = cyRegistrar.getService(CySwingAppAdapter.class);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TaskIterator ti = new TaskIterator(new AMatReaderDialogTask(appAdapter, swingApp, viewFactory, netFactory, netManager, netRootManager, taskMgr));
		taskMgr.execute(ti);
	}

}
