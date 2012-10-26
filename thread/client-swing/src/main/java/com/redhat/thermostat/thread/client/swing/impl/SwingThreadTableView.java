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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.ui.ComponentVisibleListener;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.ThreadTableView;

public class SwingThreadTableView extends ThreadTableView implements SwingComponent {

    private boolean tableRepacked = false; 
    
    private int currentSelection = -1;
    
    private ThermostatTable table;
    private ThreadTable tablePanel;
    
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    public SwingThreadTableView() {
        tablePanel = new ThreadTable();
        tablePanel.addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                SwingThreadTableView.this.notify(Action.VISIBLE);
            }
            
            @Override
            public void componentHidden(Component component) {
                SwingThreadTableView.this.notify(Action.HIDDEN);
            }
        });
        
        table = new ThermostatTable(new ThreadViewTableModel(new ArrayList<ThreadTableBean>()));
        table.setName("threadBeansTable");
        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                // NOTE: The fireTableDataChanged executes this listener
                // before the internal listener, this means this update will
                // be overridden since the default listener resets the model.
                // So, although we are in the EDT, we need to ensure that
                // we schedule this operation for later, rather than do it
                // right away... isn't Swing fun?
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (currentSelection != -1) {
                            table.setRowSelectionInterval(currentSelection, currentSelection);
                        }
                    }
                });
            }
        });
        tablePanel.add(table.wrap());
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ThreadViewTableModel model = (ThreadViewTableModel) table.getModel();
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        selectedRow = table.convertRowIndexToModel(selectedRow);
                        final ThreadTableBean bean = model.infos.get(selectedRow);
                        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                            protected Void doInBackground() throws Exception {
                                threadTableNotifier.fireAction(ThreadSelectionAction.SHOW_THREAD_DETAILS, bean);
                                return null;
                            }
                        };
                        worker.execute();
                    }
                }
            }
        });
    }
    
    @Override
    public Component getUiComponent() {
        return tablePanel;
    }
    
    @Override
    public void display(final List<ThreadTableBean> infos) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                // reset the selection for the next iteration
                // everything is happening in one thread, so there's no fear
                currentSelection = -1;
                
                ThreadViewTableModel model = (ThreadViewTableModel) table.getModel();
                int selectedRow = table.getSelectedRow();
                
                ThreadTableBean info = null;
                if (selectedRow != -1) {
                    info = model.infos.get(selectedRow);
                }
                
                model.infos = infos;

                if (info != null) {
                    int index = 0;
                    for (ThreadTableBean inModel : model.infos) {
                        if (info.equals(inModel)) {
                            currentSelection = index;
                            break;
                        }
                        index++;
                    }
                }
                
                // just repack once, or the user will see the table moving around
                if (!tableRepacked) {
                    table.repackCells();
                    tableRepacked = true;
                }
                
                model.fireTableDataChanged();
            }
        });
    }

    @SuppressWarnings("serial")
    private class ThreadViewTableModel extends DefaultTableModel {

        private String [] columns = {
                t.localize(LocaleResources.NAME),
                t.localize(LocaleResources.ID),
                t.localize(LocaleResources.START),
                t.localize(LocaleResources.STOP),
                t.localize(LocaleResources.WAIT_COUNT),
                t.localize(LocaleResources.BLOCK_COUNT),
                t.localize(LocaleResources.RUNNING),
                t.localize(LocaleResources.WAITING),
                t.localize(LocaleResources.SLEEPING),
                t.localize(LocaleResources.MONITOR), //, "Heap", "CPU Time", "User CPU Time"
        };
        
        private List<ThreadTableBean> infos;
        public ThreadViewTableModel(List<ThreadTableBean> infos) {
            this.infos = infos;
        }
    
        @Override
        public String getColumnName(int column) {
            return columns[column];
        }
        
        @Override
        public int getColumnCount() {
            return columns.length;
        }
        
        @Override
        public int getRowCount() {
            if (infos == null) {
                return 0;
            }
            return infos.size();
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
            case 1:
            case 2:
            case 3:                
                return String.class;                
            case 6:
            case 7:
                return Double.class;
            default:
                return Long.class;
            }
        }
        
        @Override
        public Object getValueAt(int row, int column) {

            Object result = null;
            
            ThreadTableBean info = infos.get(row);
            switch (column) {
            case 0:
                result = info.getId();
                break;
            case 1:
                result = info.getName();
                break;
            case 2:
                result = new Date(info.getStartTimeStamp()).toString();
                break;
            case 3:
                if (info.getStopTimeStamp() != 0) {
                    result = new Date(info.getStopTimeStamp()).toString();
                } else {
                    result = "-";
                }
                break;
            case 4:
                result = info.getWaitedCount();
                break;
            case 5:
                result = info.getBlockedCount();
                break;
            case 6:
                result = info.getRunningPercent();
                break;
            case 7:
                result = info.getWaitingPercent();
                break;
            case 8:
                result = info.getSleepingPercent();
                break;
            case 9:
                result = info.getMonitorPercent();
                break;
             default:
                 result = "n/a";
                 break;
            }
            return result;
        }
    }
}

