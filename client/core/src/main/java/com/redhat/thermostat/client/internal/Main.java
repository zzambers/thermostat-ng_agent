/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.internal;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.redhat.swing.laf.dolphin.DolphinLookAndFeel;
import com.redhat.thermostat.client.internal.config.ConnectionConfiguration;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.ThreadPoolTimerFactory;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;
import com.redhat.thermostat.common.storage.Connection.ConnectionType;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class Main {
    
    private static final Logger logger = LoggingUtils.getLogger(Main.class);

    private UiFacadeFactory uiFacadeFactory;

    public Main(UiFacadeFactory uiFacadeFactory, String[] args) {
        this.uiFacadeFactory = uiFacadeFactory;

        ClientPreferences prefs = new ClientPreferences();
        StartupConfiguration config = new ConnectionConfiguration(prefs);

        StorageProvider connProv = new MongoStorageProvider(config);
        DAOFactory daoFactory = new MongoDAOFactory(connProv);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        TimerFactory timerFactory = new ThreadPoolTimerFactory(1);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);
        SwingViewFactory viewFactory = new SwingViewFactory();
        ApplicationContext.getInstance().setViewFactory(viewFactory);
    }

    void run() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                // check if the user has other preferences...
                // not that there is any reason!
                String laf = System.getProperty("swing.defaultlaf");
                if (laf == null) {
                    try {
                        UIManager.setLookAndFeel(new DolphinLookAndFeel());
                    } catch (UnsupportedLookAndFeelException e) {
                        logger.log(Level.WARNING, "cannot use DolphinLookAndFeel");
                    }
                }

                showGui();
            }

        });

        try {
            uiFacadeFactory.awaitShutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void showGui() {
        
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        Connection connection = ApplicationContext.getInstance().getDAOFactory().getConnection();
        connection.setType(ConnectionType.LOCAL);
        ConnectionListener connectionListener = new ConnectionListener() {
            
            @Override
            public void changed(ConnectionStatus newStatus) {
                if (newStatus == ConnectionStatus.CONNECTED) {
                    MainWindowController mainController = uiFacadeFactory.getMainWindow();
                    mainController.showMainMainWindow();

                } else if (newStatus == ConnectionStatus.FAILED_TO_CONNECT) {
                    JOptionPane.showMessageDialog(
                            null,
                            localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_DESCRIPTION),
                            localize(LocaleResources.CONNECTION_FAILED_TO_CONNECT_TITLE),
                            JOptionPane.ERROR_MESSAGE);
                    uiFacadeFactory.shutdown(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
                }
            }
        };
        
        connection.addListener(connectionListener);
        try {
            connection.connect();
            
        } catch (Throwable t) {
            logger.log(Level.WARNING, "connection attempt failed: ", t);
        }
    }
}
