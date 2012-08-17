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

package com.redhat.thermostat.thread.client.swing.impl;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.client.ui.SwingComponent;
import com.redhat.thermostat.swing.ChartPanel;

import com.redhat.thermostat.thread.client.common.ThreadTableView;
import com.redhat.thermostat.thread.client.common.ThreadView;
import com.redhat.thermostat.thread.client.common.VMThreadCapabilitiesView;

import com.redhat.thermostat.thread.client.common.chart.LivingDaemonThreadDifferenceChart;

public class SwingThreadView extends ThreadView implements SwingComponent {
    
    private ThreadMainPanel panel;
    private ThreadAliveDaemonTimelinePanel timelinePanel;
    
    private SwingThreadTableView threadTable;
    private SwingVMThreadCapabilitiesView vmCapsView;
    
    public SwingThreadView() {
        
        panel = new ThreadMainPanel();
        panel.addHierarchyListener(new ComponentVisibleListener() {
            
            @Override
            public void componentShown(Component component) {
                SwingThreadView.this.notify(Action.VISIBLE);
                
                // TODO: allow controller to define this value based on last
                // user setting
                panel.getSplitPane().setDividerLocation(0.30);
            }
            
            @Override
            public void componentHidden(Component component) {
                SwingThreadView.this.notify(Action.HIDDEN);
            }
        });
        
        panel.getLiveRecording().setText("Start Recording");
        panel.getLiveRecording().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                ThreadAction action = null;
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    action = ThreadAction.START_LIVE_RECORDING;
                    panel.getLiveRecording().setText("Recording probes...");
                } else {
                    action = ThreadAction.STOP_LIVE_RECORDING;
                    panel.getLiveRecording().setText("Start Recording");
                }
                final ThreadAction toNotify = action;
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        notifier.fireAction(toNotify);
                        return null;
                    }
                };
                worker.execute();
            }
        });

        timelinePanel = new ThreadAliveDaemonTimelinePanel();
        panel.getSplitPane().setTopComponent(timelinePanel);
        
        vmCapsView = new SwingVMThreadCapabilitiesView();
        JTabbedPane pane = new JTabbedPane();
        pane.addTab("Capabilites", vmCapsView.getUiComponent());
        
        threadTable = new SwingThreadTableView();
        pane.addTab("Table", threadTable.getUiComponent());
        
        panel.getSplitPane().setBottomComponent(pane);
    }
    
    @Override
    public Component getUiComponent() {
        return panel;
    }
    
    @Override
    public void setRecording(final boolean recording) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                panel.getLiveRecording().setSelected(recording);
            }
        });
    }
    
    @Override
    public void setDaemonThreads(final String daemonThreads) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                timelinePanel.getDaemonThreads().setText(daemonThreads);
            }
        });
    }
    
    public void setLiveThreads(String liveThreads) {
        timelinePanel.getLiveThreads().setText(liveThreads);
    };
    
    @Override
    public void updateLivingDaemonTimeline(final LivingDaemonThreadDifferenceChart model)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JPanel pane = timelinePanel.getTimelinePanel();
                pane.removeAll();
                
                ChartPanel charts = new ChartPanel(model);
                pane.add(charts);
                pane.revalidate();
                pane.repaint();
            }
        });
    }
    
    @Override
    public VMThreadCapabilitiesView createVMThreadCapabilitiesView() {
        return vmCapsView;
    }
    
    @Override
    public ThreadTableView createThreadTableView() {
        return threadTable;
    }
}
