/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.client.swing.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.AgentInformationDisplayView;
import com.redhat.thermostat.client.core.views.AgentInformationViewProvider;
import com.redhat.thermostat.client.core.views.ClientConfigViewProvider;
import com.redhat.thermostat.client.core.views.ClientConfigurationView;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.SummaryViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.swing.internal.MainView.Action;
import com.redhat.thermostat.client.swing.internal.osgi.HostContextActionServiceTracker;
import com.redhat.thermostat.client.swing.internal.osgi.InformationServiceTracker;
import com.redhat.thermostat.client.swing.internal.osgi.VMContextActionServiceTracker;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorProviderExtensionListener;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.FilterManager;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController.ReferenceSelection;
import com.redhat.thermostat.client.ui.AgentInformationDisplayController;
import com.redhat.thermostat.client.ui.AgentInformationDisplayModel;
import com.redhat.thermostat.client.ui.ClientConfigurationController;
import com.redhat.thermostat.client.ui.HostInformationController;
import com.redhat.thermostat.client.ui.MainWindowController;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.client.ui.MenuRegistry;
import com.redhat.thermostat.client.ui.SummaryController;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.AllPassFilter;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;
import com.redhat.thermostat.utils.keyring.Keyring;

public class MainWindowControllerImpl implements MainWindowController {
    
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    private static final Logger logger = LoggingUtils.getLogger(MainWindowControllerImpl.class);

    private final ApplicationInfo appInfo = new ApplicationInfo();

    private ApplicationService appSvc;

    private MainView view;
    private Keyring keyring;
    
    private HostInfoDAO hostInfoDAO;
    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDAO;
    private BackendInfoDAO backendInfoDAO;

    private SummaryViewProvider summaryViewProvider;
    private HostInformationViewProvider hostInfoViewProvider;
    private VmInformationViewProvider vmInfoViewProvider;
    private AgentInformationViewProvider agentInfoViewProvider;
    private ClientConfigViewProvider clientConfigViewProvider;

    private InformationServiceTracker infoServiceTracker;
    private HostContextActionServiceTracker hostContextActionTracker;
    private VMContextActionServiceTracker vmContextActionTracker;
    private MultipleServiceTracker depTracker;
    
    private CountDownLatch shutdown;
    private CountDownLatch initViewLatch = new CountDownLatch(1);

    private NetworkMonitor networkMonitor;
    private HostMonitor hostMonitor;
    
    private VMMonitorController vmMonitor;

    private MenuRegistry menuRegistry;
    private ActionListener<ThermostatExtensionRegistry.Action> menuListener =
            new ActionListener<ThermostatExtensionRegistry.Action>()
    {
        @Override
        public void actionPerformed(
            ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
            MenuAction action = (MenuAction) actionEvent.getPayload();

            switch (actionEvent.getActionId()) {
            case SERVICE_ADDED:
                view.addMenu(action);
                break;

            case SERVICE_REMOVED:
                view.removeMenu(action);
                break;

            default:
                logger.log(Level.WARNING, "received unknown event from MenuRegistry: " +
                                           actionEvent.getActionId());
                break;
            }
        }
    };

    private HostTreeDecoratorRegistry hostDecoratorRegistry;
    private VMTreeDecoratorRegistry vmDecoratorRegistry;

    private VMInformationRegistry vmInfoRegistry;
    private ActionListener<ThermostatExtensionRegistry.Action> vmInfoRegistryListener =
            new ActionListener<ThermostatExtensionRegistry.Action> ()
    {
        public void actionPerformed(com.redhat.thermostat.common.ActionEvent<ThermostatExtensionRegistry.Action>
                                    actionEvent)
        {
            // TODO
            // System.err.println(actionEvent.getPayload());
        };
    };

    private VmInformationControllerProvider vmInfoControllerProvider;
    private VmFilterRegistry vmFilterRegistry;
    private HostFilterRegistry hostFilterRegistry;
    private FilterManager filterManager;
    
    public MainWindowControllerImpl(BundleContext context, ApplicationService appSvc,
            CountDownLatch shutdown) {
        this(context, appSvc, new MainWindow(), new RegistryFactory(context), shutdown);
    }

    MainWindowControllerImpl(final BundleContext context, ApplicationService appSvc,
            final MainView view,
            RegistryFactory registryFactory,
            final CountDownLatch shutdown)
    {
        this.appSvc = appSvc;
        this.view = view;
       
        try {
            
            vmFilterRegistry = registryFactory.createVmFilterRegistry();
            hostFilterRegistry = registryFactory.createHostFilterRegistry();
            
            hostDecoratorRegistry = registryFactory.createHostTreeDecoratorRegistry();
            vmDecoratorRegistry = registryFactory.createVMTreeDecoratorRegistry();
            menuRegistry = registryFactory.createMenuRegistry();
            vmInfoRegistry = registryFactory.createVMInformationRegistry();
            
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
                
        this.infoServiceTracker = new InformationServiceTracker(context);
        this.infoServiceTracker.open();
        
        this.hostContextActionTracker = new HostContextActionServiceTracker(context);
        this.hostContextActionTracker.open();

        this.vmContextActionTracker = new VMContextActionServiceTracker(context);
        this.vmContextActionTracker.open();
        
        this.shutdown = shutdown;

        Class<?>[] deps = new Class<?>[] {
                Keyring.class,
                HostInfoDAO.class,
                VmInfoDAO.class,
                AgentInfoDAO.class,
                BackendInfoDAO.class,
                SummaryViewProvider.class,
                HostInformationViewProvider.class,
                VmInformationViewProvider.class,
                AgentInformationViewProvider.class,
                ClientConfigViewProvider.class,
                HostMonitor.class,
                NetworkMonitor.class,
        };
        depTracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {
            
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                keyring = (Keyring) services.get(Keyring.class.getName());
                Objects.requireNonNull(keyring);
                hostInfoDAO = (HostInfoDAO) services.get(HostInfoDAO.class.getName());
                Objects.requireNonNull(hostInfoDAO);
                vmInfoDAO = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                Objects.requireNonNull(vmInfoDAO);
                agentInfoDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                Objects.requireNonNull(agentInfoDAO);
                backendInfoDAO = (BackendInfoDAO) services.get(BackendInfoDAO.class.getName());
                Objects.requireNonNull(backendInfoDAO);
                summaryViewProvider = (SummaryViewProvider) services.get(SummaryViewProvider.class.getName());
                Objects.requireNonNull(summaryViewProvider);
                hostInfoViewProvider = (HostInformationViewProvider) services.get(HostInformationViewProvider.class.getName());
                Objects.requireNonNull(hostInfoViewProvider);
                vmInfoViewProvider = (VmInformationViewProvider) services.get(VmInformationViewProvider.class.getName());
                Objects.requireNonNull(vmInfoViewProvider);
                agentInfoViewProvider = (AgentInformationViewProvider) services.get(AgentInformationViewProvider.class.getName());
                Objects.requireNonNull(agentInfoViewProvider);
                clientConfigViewProvider = (ClientConfigViewProvider) services.get(ClientConfigViewProvider.class.getName());
                Objects.requireNonNull(clientConfigViewProvider);

                networkMonitor = (NetworkMonitor) services.get(NetworkMonitor.class.getName());
                hostMonitor = (HostMonitor) services.get(HostMonitor.class.getName());
                
                initView();

                vmInfoControllerProvider = new VmInformationControllerProvider();
                
                installListenersAndStartRegistries();
                
                vmMonitor = initMonitors();
                vmMonitor.start();
                
                registerProgressNotificator(context);
            }

            @Override
            public void dependenciesUnavailable() {
                if (shutdown.getCount() > 0) {
                    // In the rare case we lose one of our deps, gracefully shutdown
                    logger.severe("Dependency unexpectedly became unavailable");
                    shutdown.countDown();
                }
            }
        });
        depTracker.open();
    }

    VMMonitorController initMonitors() {
        VMMonitorController vmMonitor =
                new VMMonitorController(networkMonitor, hostMonitor, view);
        return vmMonitor;
    }
    
    /**
     * This method is for testing purposes only
     */
    ActionListener<ThermostatExtensionRegistry.Action> getMenuListener() {
        return menuListener;
    }
    
    private void initHostVMTree() {
        HostTreeController hostController = view.getHostTreeController();
        
        // initially fill out with all known host and vms
        List<HostRef> hosts = networkMonitor.getHosts(new AllPassFilter<HostRef>());
        AllPassFilter<VmRef> vmFilter = new AllPassFilter<>();
        for (HostRef host : hosts) {
            hostController.registerHost(host);

            // get the vm for this host
            List<VmRef> vms = hostMonitor.getVirtualMachines(host, vmFilter);
            for (VmRef vm : vms) {
                hostController.registerVM(vm);
            }
        }
    }
    
    private void initView() {
        view.setWindowTitle(appInfo.getName());

        initHostVMTree();
        view.getHostTreeController().addReferenceSelectionChangeListener(new
                ActionListener<HostTreeController.ReferenceSelection>() {
            @Override
            public void actionPerformed(ActionEvent<ReferenceSelection> actionEvent) {
                updateView((Ref) actionEvent.getPayload());
            }
        });
        
        view.addActionListener(new ActionListener<MainView.Action>() {

            @Override
            public void actionPerformed(ActionEvent<MainView.Action> evt) {
                MainView.Action action = evt.getActionId();
                switch (action) {

                case HIDDEN:
                case VISIBLE:
                    break;
                    
                case SHOW_AGENT_CONFIG:
                    showAgentConfiguration();
                    break;
                case SHOW_CLIENT_CONFIG:
                    showConfigureClientPreferences();
                    break;
                case SHOW_ABOUT_DIALOG:
                    showAboutDialog();
                    break;
                case SHOW_HOST_VM_CONTEXT_MENU:
                    showContextMenu(evt);
                    break;
                case HOST_VM_CONTEXT_ACTION:
                    handleVMHooks(evt);
                    break;
                case SHUTDOWN:
                    // Main will call shutdownApplication
                    shutdown.countDown();
                    break;
                    
                default:
                    throw new IllegalStateException("unhandled action");
                }
            }

        });
        initViewLatch.countDown();
    }

    /*
     * Called by Main to cleanup when shutting down
     */
    void shutdownApplication() {
        uninstallListenersAndStopRegistries();

        view.hideMainWindow();
        appSvc.getTimerFactory().shutdown();
        
        depTracker.close();
        infoServiceTracker.close();
        hostContextActionTracker.close();
        vmContextActionTracker.close();
    }

    private void installListenersAndStartRegistries() {
        menuRegistry.addActionListener(menuListener);
        menuRegistry.start();

        HostTreeController hostTreeController = view.getHostTreeController();
        filterManager = new FilterManager(vmFilterRegistry, hostFilterRegistry,
                                          hostTreeController);

        filterManager.start();
        
        DecoratorProviderExtensionListener<HostRef> hostDecoratorListener =
                hostTreeController.getHostDecoratorListener();
        hostDecoratorRegistry.addActionListener(hostDecoratorListener);
        hostDecoratorRegistry.start();
        
        DecoratorProviderExtensionListener<VmRef> vmDecoratorListener =
                hostTreeController.getVmDecoratorListener();
        vmDecoratorRegistry.addActionListener(vmDecoratorListener);
        vmDecoratorRegistry.start();

        vmInfoRegistry.addActionListener(vmInfoRegistryListener);
        vmInfoRegistry.start();
    }

    private void registerProgressNotificator(BundleContext context) {
        ProgressNotifier notifier = view.getNotifier();
        context.registerService(ProgressNotifier.class, notifier, null);
    }
    
    private void uninstallListenersAndStopRegistries() {
        menuRegistry.removeActionListener(menuListener);
        menuListener = null;
        menuRegistry.stop();

        filterManager.stop();
        
        HostTreeController hostTreeController = view.getHostTreeController();
        
        DecoratorProviderExtensionListener<HostRef> hostDecoratorListener =
                hostTreeController.getHostDecoratorListener();
        hostDecoratorRegistry.removeActionListener(hostDecoratorListener);
        hostDecoratorRegistry.stop();

        DecoratorProviderExtensionListener<VmRef> vmDecoratorListener =
                hostTreeController.getVmDecoratorListener();
        vmDecoratorRegistry.removeActionListener(vmDecoratorListener);
        vmDecoratorRegistry.stop();

        vmInfoRegistry.removeActionListener(vmInfoRegistryListener);
        vmInfoRegistryListener = null;
        vmInfoRegistry.stop();
    }

    private void showContextMenu(ActionEvent<Action> evt) {
        // TODO
//        List<ContextAction> toShow = new ArrayList<>();
//
//        Ref ref = view.getSelectedHostOrVm();
//        if (ref instanceof HostRef) {
//            HostRef vm = (HostRef) ref;
//
//            logger.log(Level.INFO, "registering applicable HostContextActions actions to show");
//
//            for (HostContextAction action : hostContextActionTracker.getHostContextActions()) {
//                if (action.getFilter().matches(vm)) {
//                    toShow.add(action);
//                }
//            }
//        } else if (ref instanceof VmRef) {
//            VmRef vm = (VmRef) ref;
//
//            logger.log(Level.INFO, "registering applicable VMContextActions actions to show");
//
//            for (VMContextAction action : vmContextActionTracker.getVmContextActions()) {
//                if (action.getFilter().matches(vm)) {
//                    toShow.add(action);
//                }
//            }
//        }
//
//        view.showContextActions(toShow, (MouseEvent) evt.getPayload());
    }

    private void handleVMHooks(ActionEvent<MainView.Action> event) {
//        Object payload = event.getPayload();
//        try {
//            if (payload instanceof HostContextAction) {
//                HostContextAction action = (HostContextAction) payload;
//                action.execute((HostRef) view.getSelectedHostOrVm());
//            } else if (payload instanceof VMContextAction) {
//                VMContextAction action = (VMContextAction) payload;
//                action.execute((VmRef) view.getSelectedHostOrVm());
//            }
//        } catch (Throwable error) {
//            logger.log(Level.SEVERE, "error invocating context action", error);
//        }
    }

    @Override
    public void showMainMainWindow() {
        try {
            initViewLatch.await();
        } catch (InterruptedException e) {
            logger.warning("Interrupted while awaiting view initialization.");
        }
        view.showMainWindow();
    }

    private void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(appInfo);
        aboutDialog.setModal(true);
        aboutDialog.pack();
        aboutDialog.setLocationRelativeTo(view.getTopFrame());
        aboutDialog.setVisible(true);
    }

    private void showAgentConfiguration() {
        AgentInformationDisplayModel model = new AgentInformationDisplayModel(agentInfoDAO, backendInfoDAO);
        AgentInformationDisplayView view = agentInfoViewProvider.createView();
        AgentInformationDisplayController controller = new AgentInformationDisplayController(model, view);
        controller.showView();
    }

    private void showConfigureClientPreferences() {
        ClientPreferences prefs = new ClientPreferences(keyring);
        ClientConfigurationView view = clientConfigViewProvider.createView();
        ClientConfigurationController controller = new ClientConfigurationController(prefs, view);
        controller.showDialog();
    }

    private void updateView(Ref ref) {
        if (ref == null) {
            SummaryController controller = createSummaryController();
            view.setSubView(controller.getView());
        } else if (ref instanceof HostRef) {
            HostRef hostRef = (HostRef) ref;
            HostInformationController hostController = createHostInformationController(hostRef);
            view.setSubView(hostController.getView());
            view.setStatusBarPrimaryStatus(t.localize(LocaleResources.HOST_PRIMARY_STATUS,
                    hostRef.getHostName(), hostRef.getAgentId()));
        } else if (ref instanceof VmRef) {
            VmRef vmRef = (VmRef) ref;
            VmInformationController vmInformation =
                    vmInfoControllerProvider.getVmInfoController(vmRef);
            view.setSubView(vmInformation.getView());
            view.setStatusBarPrimaryStatus(t.localize(LocaleResources.VM_PRIMARY_STATUS,
                    vmRef.getName(), vmRef.getVmId(), vmRef.getHostRef().getHostName()));
        } else {
            throw new IllegalArgumentException("unknown type of ref");
        }
    }

    private class VmInformationControllerProvider {
        private VmInformationController lastSelectedVM;
        private Map<VmRef, Integer> selectedForVM = new ConcurrentHashMap<>();
        
        VmInformationController getVmInfoController(VmRef vmRef) {
            int id = 0;
            if (lastSelectedVM != null) {
                id = lastSelectedVM.getSelectedChildID();
            }
            
            lastSelectedVM = createVmController(vmRef);
            if (!lastSelectedVM.selectChildID(id)) {
                Integer _id = selectedForVM.get(vmRef);
                id = _id != null? _id : 0;
                lastSelectedVM.selectChildID(id);
            }

            selectedForVM.put(vmRef, id);
            
            return lastSelectedVM;
        }
    }
    
    private SummaryController createSummaryController() {
        return new SummaryController(appSvc, hostInfoDAO, vmInfoDAO, summaryViewProvider);
    }

    private HostInformationController createHostInformationController(HostRef ref) {
        List<InformationService<HostRef>> hostInfoServices = infoServiceTracker.getHostInformationServices();
        return new HostInformationController(hostInfoServices, ref, hostInfoViewProvider);
    }

    private VmInformationController createVmController(VmRef ref) {
        List<InformationService<VmRef>> vmInfoServices = infoServiceTracker.getVmInformationServices();
        return new VmInformationController(vmInfoServices, ref, vmInfoViewProvider);
    }

}

