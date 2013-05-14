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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapDumpListener;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapSelectionEvent;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.XYPlot;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.chart.OverviewChart;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.HeapChartPanel;
import com.redhat.thermostat.vm.heap.analysis.client.swing.internal.stats.StatsPanel;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

public class HeapSwingView extends HeapView implements SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private StatsPanel stats;

    private HeapPanel heapDetailPanel;
    private HeaderPanel overview;
    
    private JPanel visiblePane;
    
    public HeapSwingView() {
        stats = new StatsPanel();
        stats.addHeapDumperListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                heapDumperNotifier.fireAction(HeapDumperAction.DUMP_REQUESTED);
            }
        });

        stats.addDumpListListener(new HeapDumpListener() {
            @Override
            public void actionPerformed(HeapSelectionEvent e) {
                HeapDump dump = e.getSource().getHeapDump();
                heapDumperNotifier.fireAction(HeapDumperAction.ANALYSE, dump);
            }
        });
        
        visiblePane = new JPanel();
        visiblePane.setLayout(new BoxLayout(visiblePane, BoxLayout.X_AXIS));
        
        heapDetailPanel = new HeapPanel();
        
        overview = new HeaderPanel(translator.localize(LocaleResources.HEAP_OVERVIEW_TITLE));
        overview.setContent(stats);
        overview.addHierarchyListener(new ViewVisibleListener());

        // at the beginning, only the overview is visible
        visiblePane.add(overview);
    }
    
    private class ViewVisibleListener extends ComponentVisibleListener {
        @Override
        public void componentShown(Component component) {
            HeapSwingView.this.notify(Action.VISIBLE);
        }

        @Override
        public void componentHidden(Component component) {
            HeapSwingView.this.notify(Action.HIDDEN);
        }
    }

    @Override
    public void setModel(final OverviewChart model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // TODO, move to controller
                model.createChart(stats.getWidth(), stats.getBackground());
                
                final HeapChartPanel charts = new HeapChartPanel(model.getChart());
                
                XYPlot plot = model.getChart().getXYPlot();
                DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
                domainAxis.addChangeListener(new AxisChangeListener() {
                    @Override
                    public void axisChanged(AxisChangeEvent event) {
                        // somehow the chart panel doesn't see this
                        charts.revalidate();
                    }
                });
                
                /*
                 * By default, ChartPanel scales itself instead of redrawing things when
                 * it's re-sized. To have it resize automatically, we need to set minimum
                 * and maximum sizes. Lets constrain the minimum, but not the maximum
                 * size.
                 */
                final int MINIMUM_DRAW_SIZE = 100;

                charts.setMinimumDrawHeight(MINIMUM_DRAW_SIZE);
                charts.setMinimumDrawWidth(MINIMUM_DRAW_SIZE);

                charts.setMaximumDrawHeight(Integer.MAX_VALUE);
                charts.setMaximumDrawWidth(Integer.MAX_VALUE);

                stats.setChartPanel(charts);
            }
        });
    }

    @Override
    public void updateUsedAndCapacity(final String used, final String capacity) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                stats.setMax(capacity);
                stats.setUsed(used);
            }
        });
    }

    @Override
    public void enableHeapDumping() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.enableHeapDumperControl();
            }
        });
    }

    @Override
    public void disableHeapDumping(final DumpDisabledReason reason) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.disableHeapDumperControl(reason);
            }
        });
    }

    @Override
    public void addHeapDump(final HeapDump dump) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.addDump(dump);
            }
        });
    }
    
    @Override
    public void clearHeapDumpList() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.clearDumpList();
            }
        });
    }
    
    @Override
    public void openDumpView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                visiblePane.removeAll();
                heapDetailPanel.divideView();
                heapDetailPanel.setTop(overview);

                visiblePane.add(heapDetailPanel);
                visiblePane.revalidate();
            }
        });
    }

    @Override
    public void setChildView(BasicView childView) {
        if (childView instanceof HeapDetailsSwing) {
            final HeapDetailsSwing view = (HeapDetailsSwing)childView;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    heapDetailPanel.setBottom(view.getUiComponent());
                    visiblePane.revalidate();
                }
            });
        }
    }

    @Override
    public void setActiveDump(final HeapDump dump) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.selectOverlay(dump);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePane;
    }

    @Override
    public void notifyHeapDumpComplete() {
        enableHeapDumping();
    }

    @Override
    public void updateHeapDumpList(final List<HeapDump> heapDumps) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                stats.updateHeapDumpList(heapDumps);
            }
        });
    }

    @Override
    public void displayWarning(String string) {
        JOptionPane.showMessageDialog(visiblePane, string, "Warning", JOptionPane.WARNING_MESSAGE);
    }
}

