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

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.core.views.ClientConfigurationView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.internal.views.ClientConfigurationSwing;
import com.redhat.thermostat.client.ui.ClientConfigReconnector;
import com.redhat.thermostat.client.ui.ClientConfigurationController;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Main {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final Logger logger = LoggingUtils.getLogger(Main.class);

    private BundleContext context;
    private ApplicationService appSvc;
    private DbServiceFactory dbServiceFactory;
    private Keyring keyring;
    private CountDownLatch shutdown;
    private MainWindowControllerImpl mainController;
    private MainWindowRunnable mainWindowRunnable;
    
    public Main(BundleContext context, Keyring keyring,
            ApplicationService appSvc, String[] args) {

        DbServiceFactory dbServiceFactory = new DbServiceFactory();
        CountDownLatch shutdown = new CountDownLatch(1);
        MainWindowRunnable mainWindowRunnable = new MainWindowRunnable();

        init(context, appSvc, dbServiceFactory, keyring, shutdown,
                mainWindowRunnable);
    }

    Main(BundleContext context, ApplicationService appSvc,
            DbServiceFactory dbServiceFactory, Keyring keyring,
            CountDownLatch shutdown, MainWindowRunnable mainWindowRunnable) {
        init(context, appSvc, dbServiceFactory, keyring, shutdown,
                mainWindowRunnable);
    }

    private void init(BundleContext context, ApplicationService appSvc,
            DbServiceFactory dbServiceFactory, Keyring keyring,
            CountDownLatch shutdown, MainWindowRunnable mainWindowRunnable) {
        this.context = context;
        this.appSvc = appSvc;
        this.dbServiceFactory = dbServiceFactory;
        this.keyring = keyring;
        this.shutdown = shutdown;
        this.mainWindowRunnable = mainWindowRunnable;
    }

    private void setLAF() {
        
        boolean useDefault = false;
        
        // check if the user has other preferences...
        String laf = System.getProperty("swing.defaultlaf");
        if (laf == null) {
            useDefault = true;
            
        } else if (laf.equalsIgnoreCase("dolphin")) {
            try {
                UIManager.setLookAndFeel("com.redhat.swing.laf.dolphin.DolphinLookAndFeel");
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                     InstantiationException | IllegalAccessException e) {
                useDefault = true;
                logger.log(Level.WARNING, "cannot set DolphinLookAndFeel");
            }
        } else if (laf.equalsIgnoreCase("system")) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                     InstantiationException | IllegalAccessException e) {
                useDefault = true;
                logger.log(Level.WARNING, "cannot set System LookAndFeel");
            }
        }
        
        if (useDefault) {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
                // well, whatever...
                logger.log(Level.WARNING, "cannot set NimbusLookAndFeel");
            }
        }
    }
    
    public void run() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                setLAF();

                // Thermostat JPopupMenu instances should all be
                // ThermostatPopupmenu, so this is redundant, but done in case
                // some client code doesn't use the internal popup
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                
                // TODO: move them in an appropriate place
                UIManager.getDefaults().put("OptionPane.buttonOrientation", SwingConstants.RIGHT);
                UIManager.getDefaults().put("OptionPane.isYesLast", true);
                UIManager.getDefaults().put("OptionPane.sameSizeButtons", true);
                
            }

        });

        tryConnecting();

        try {
            shutdown.await();
            
            // Shutdown MainController
            if (mainController != null) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            mainController.shutdownApplication();
                        }
                    });
                } catch (InvocationTargetException e) {
                    logger.log(Level.WARNING, "Unable to shutdown MainWindowController", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void tryConnecting() {
        final ExecutorService service = appSvc.getApplicationExecutor();
        ClientPreferences prefs = new ClientPreferences(keyring);
        connect(prefs, service);
    }
    
    private class ConnectionAttempt implements Runnable {
        private ExecutorService service;
        public ConnectionAttempt(ExecutorService service) {
            this.service = service;
        }
        
        @Override
        public void run() {
            Object[] options = {
                    translator.localize(LocaleResources.CONNECTION_WIZARD),
                    translator.localize(LocaleResources.CONNECTION_QUIT),
            };
            int n = JOptionPane
                    .showOptionDialog(
                            null,
                            translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION),
                            translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);

            switch (n) {

            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.NO_OPTION:
                shutdown.countDown();
                break;

            case JOptionPane.YES_OPTION:
            default:
                createPreferencesDialog(service);
                break;
            }
        }
    }
    
    private void connect(ClientPreferences prefs, ExecutorService service) {
        final ConnectionHandler reconnectionHandler = new ConnectionHandler(service);
        try {
            // create DbService with potentially modified parameters
            final DbService dbService = dbServiceFactory.createDbService(prefs.getUserName(), prefs.getPassword(), prefs.getConnectionUrl());
            dbService.addConnectionListener(reconnectionHandler);
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        dbService.connect();
                    } catch (Throwable t) {
                        // Note: DbService fires a ConnectionListener event when it
                        // fails to connect. No need to notify our handler manually.
                        logger.log(Level.WARNING, "connection attempt failed: ", t);
                    }
                }
            });
        } catch (StorageException se) {
            logger.log(Level.WARNING, "Unable to find appropriate storage provider", se);
            // Prevent Icedtea BZ#1294, where no matching StorageProvider
            // could potentially be found for the given connection URL.
            // Indicate connection failure immediately.
            reconnectionHandler.changed(ConnectionStatus.FAILED_TO_CONNECT);
        }
    }
    
    private class MainClientConfigReconnector implements ClientConfigReconnector {
        private ExecutorService service;
        public MainClientConfigReconnector(ExecutorService service) {
            this.service = service;
        }
        
        @Override
        public void reconnect(ClientPreferences prefs) {
            connect(prefs, service);
        }

        @Override
        public void abort() {
            shutdown.countDown();
        }
    }
    
    private void createPreferencesDialog(final ExecutorService service) {
        ClientPreferences prefs = new ClientPreferences(keyring);
        ClientConfigurationView configDialog = new ClientConfigurationSwing();
        ClientConfigurationController controller =
                new ClientConfigurationController(prefs, configDialog, new MainClientConfigReconnector(service));
        
        controller.showDialog();
    }
    
    private class ConnectionHandler implements ConnectionListener {
        private boolean retry;
        private ExecutorService service;
        public ConnectionHandler(ExecutorService service) {
            this.retry = true;
            this.service = service;
        }
        
        private void showConnectionAttemptWarning() {
            SwingUtilities.invokeLater(new ConnectionAttempt(service));
        }
        
        @Override
        public void changed(ConnectionStatus newStatus) {
            if (newStatus == ConnectionStatus.CONNECTED) {
                SwingUtilities.invokeLater(mainWindowRunnable);
            } else if (newStatus == ConnectionStatus.FAILED_TO_CONNECT) {
                if (retry) {
                    retry = false;
                    showConnectionAttemptWarning();
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION),
                            translator.localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE),
                            JOptionPane.ERROR_MESSAGE);
                    shutdown.countDown();
                }
            }
        }
    }
    
    public void shutdown() {
        shutdown.countDown();
    }
    
    /*
     * This Runnable is extracted to a class for testing purposes.
     */
    class MainWindowRunnable implements Runnable {

        @Override
        public void run() {
            mainController = new MainWindowControllerImpl(context, appSvc, shutdown);
            mainController.showMainMainWindow();
        }
    }
}

