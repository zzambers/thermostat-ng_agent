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

package com.redhat.thermostat.client.stats.memory.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.stats.memory.core.MemoryStatsView;
import com.redhat.thermostat.client.stats.memory.core.Payload;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.gc.remote.client.common.RequestGCAction;
import com.redhat.thermostat.gc.remote.client.swing.ToolbarGCButton;
import com.redhat.thermostat.gc.remote.common.command.GCCommand;

public class MemoryStatsViewImpl extends MemoryStatsView implements SwingComponent {

    private static final long REPAINT_DELAY = 500;
    private long lastRepaint;
    
    private HeaderPanel visiblePanel;
    private JPanel realPanel;
    
    private final Map<String, MemoryGraphPanel> regions;
    
    private RequestGCAction toobarButtonAction;
    
    private Dimension preferredSize;
    
    public MemoryStatsViewImpl() {
        super();
        visiblePanel = new HeaderPanel();
        regions = new HashMap<>();
 
        preferredSize = new Dimension(0, 0);
        
        visiblePanel.setHeader("Memory Regions");

        visiblePanel.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(Component component) {
                notifier.fireAction(Action.HIDDEN);
            }
        });

        realPanel = new JPanel();
        realPanel.setLayout(new BoxLayout(realPanel, BoxLayout.Y_AXIS));
        visiblePanel.setContent(realPanel);
        
        toobarButtonAction = new RequestGCAction();
        visiblePanel.addToolBarButton(new ToolbarGCButton(toobarButtonAction));
    }
    
    @Transient
    public Dimension getPreferredSize() {
        return new Dimension(preferredSize);
    }
    
    @Override
    public void updateRegion(final Payload region) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MemoryGraphPanel memoryGraphPanel = regions.get(region.getName());
                memoryGraphPanel.setMemoryGraphProperties(region);
            }
        });
    }
    
    @Override
    public void addGCActionListener(ActionListener<GCCommand> listener) {
        toobarButtonAction.addActionListener(listener);
    }
    
    @Override
    public void addRegion(final Payload region) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MemoryGraphPanel memoryGraphPanel = new MemoryGraphPanel();
                
                realPanel.add(memoryGraphPanel);
                realPanel.add(Box.createRigidArea(new Dimension(5,5)));
                regions.put(region.getName(), memoryGraphPanel);
                
                // components are stacked up vertically in this panel
                Dimension memoryGraphPanelMinSize = memoryGraphPanel.getMinimumSize();
                preferredSize.height += memoryGraphPanelMinSize.height + 5;
                if (preferredSize.width < (memoryGraphPanelMinSize.width + 5)) {
                    preferredSize.width = memoryGraphPanelMinSize.width + 5;
                }

                updateRegion(region);
                realPanel.revalidate();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public void requestRepaint() {
        // really only repaint every REPAINT_DELAY milliseconds
        long now = System.currentTimeMillis();
        if (now - lastRepaint > REPAINT_DELAY) {
            visiblePanel.repaint();
            lastRepaint = System.currentTimeMillis();
        }
    }
    
    public BasicView getView() {
        return this;
    }
}
