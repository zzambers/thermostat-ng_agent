/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.swing.OverlayContainer;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class VmInformationPanel extends VmInformationView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(VmInformationPanel.class);

    private final JTabbedPane tabPane = new JTabbedPane();
    private JPanel visiblePanel;

    public VmInformationPanel() {
        super();
        visiblePanel = new JPanel();
        visiblePanel.setLayout(new BorderLayout());
        tabPane.setName("tabPane");
        visiblePanel.add(tabPane);
    }

    @Override
    public void addChildView(final LocalizedString title, final UIComponent view) {
        if (view instanceof SwingComponent) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    SwingComponent panel = (SwingComponent) view;
                    tabPane.addTab(title.getContents(), null, panel.getUiComponent(), null);
                    if (view instanceof OverlayContainer) {
                        OverlayContainer overlayContainer = (OverlayContainer) view;
                        tabPane.addMouseListener(overlayContainer.getOverlay().getClickOutCloseListener(tabPane));
                    }
                }
            });
        } else {
            String message = ""
                    + "There's a non-swing view registered: '" + view.toString()
                    + "'. The swing client can not use these views. This is "
                    + "most likely a developer mistake. If this is meant to "
                    + "be a swing-based view, it must implement the "
                    + "'SwingComponent' interface. If it's not meant to be a "
                    + "swing-based view, it should not have been registered.";
            logger.severe(message);
            throw new AssertionError(message);
        }
    }

    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public int getSelectedChildID() {
        return tabPane.getSelectedIndex();
    }

    @Override
    public boolean selectChildID(int id) {
        if (tabPane.getComponentCount() > id) {
            tabPane.setSelectedIndex(id);
            return true;
        }
        return false;
    }

    @Override
    public int getNumChildren() {
        return tabPane.getComponentCount();
    }
}

