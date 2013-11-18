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

package com.redhat.thermostat.client.swing.internal.views;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.redhat.thermostat.client.core.views.ClientConfigurationView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.Translate;

public class ClientConfigurationSwing implements ClientConfigurationView {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final WindowClosingListener windowClosingListener;

    private final ClientConfigurationPanel configurationPanel;

    private final CopyOnWriteArrayList<ActionListener<Action>> listeners = new CopyOnWriteArrayList<>();

    private JDialog dialog;

    public ClientConfigurationSwing() {
        assertInEDT();

        windowClosingListener = new WindowClosingListener();
        configurationPanel = new ClientConfigurationPanel();

        java.awt.event.ActionListener acceptOnEnterListener = new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent arg0) {
                fireAction(new ActionEvent<>(ClientConfigurationSwing.this, Action.CLOSE_ACCEPT));
            }
        };

        // handle 'enter' in text fields
        JTextField[] fields = new JTextField[] {
                configurationPanel.storageUrl,
                configurationPanel.userName,
                configurationPanel.password};

        for (JTextField field: fields) {
            field.addActionListener(acceptOnEnterListener);
        }
        
        final JOptionPane optionPane = new JOptionPane(configurationPanel);
        optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ((evt.getSource() == optionPane) &&
                    (propertyName.equals(JOptionPane.VALUE_PROPERTY))) {
                    if (dialog.isVisible()) {
                        if (evt.getNewValue().equals(JOptionPane.OK_OPTION)) {
                            fireAction(new ActionEvent<>(ClientConfigurationSwing.this, Action.CLOSE_ACCEPT));
                        } else if (evt.getNewValue().equals(JOptionPane.CANCEL_OPTION)) {
                            fireAction(new ActionEvent<>(ClientConfigurationSwing.this, Action.CLOSE_CANCEL));
                        }
                    }
                }
            }
        });

        dialog = new JDialog((Frame) null, translator.localize(LocaleResources.CLIENT_PREFS_WINDOW_TITLE).getContents());
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(optionPane);
        dialog.addWindowListener(windowClosingListener);
    }

    JDialog getDialog() {
        return dialog;
    }

    @Override
    public void showDialog() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialog.pack();
                dialog.setVisible(true);
            }
        });
    }

    @Override
    public void hideDialog() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(false);
                dialog.dispose();
                dialog = null;
            }

        });
    }

    @Override
    public String getConnectionUrl() {
        try {
            return new EdtHelper().callAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return configurationPanel.storageUrl.getText();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            InternalError error = new InternalError();
            error.initCause(e);
            throw error;
        }
    }

    @Override
    public void setConnectionUrl(final String url) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                configurationPanel.storageUrl.setText(url);
            }
        });
    }

    @Override
    public void addListener(ActionListener<Action> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ActionListener<Action> listener) {
        listeners.remove(listener);
    }

    private void fireAction(ActionEvent<Action> actionEvent) {
        for (ActionListener<Action> listener: listeners) {
            listener.actionPerformed(actionEvent);
        }
    }

    private void assertInEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be invoked in the EDT");
        }
    }

    class WindowClosingListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            fireAction(new ActionEvent<>(ClientConfigurationSwing.this, Action.CLOSE_CANCEL));
        }
    }

    @Override
    public void setPassword(final String password) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                configurationPanel.password.setText(password);
            }
        });
    }
    
    @Override
    public void setUserName(final String username) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                configurationPanel.userName.setText(username);
            }
        });
    };
    
    @Override
    public String getPassword() {
        try {
            return new EdtHelper().callAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return configurationPanel.password.getText();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            InternalError error = new InternalError();
            error.initCause(e);
            throw error;
        }
    }
    
    @Override
    public String getUserName() {
        try {
            return new EdtHelper().callAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return configurationPanel.userName.getText();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            InternalError error = new InternalError();
            error.initCause(e);
            throw error;
        }
    }
    
    @Override
    public boolean getSaveEntitlements() {
        try {
            return new EdtHelper().callAndWait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return configurationPanel.saveEntitlements.isSelected();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            InternalError error = new InternalError();
            error.initCause(e);
            throw error;
        }
    }
    
    @Override
    public void setSaveEntitlemens(final boolean save) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                configurationPanel.saveEntitlements.setSelected(save);
            }
        });
    }
}

