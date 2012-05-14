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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.Component;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.ui.HostMemoryView.Action;
import com.redhat.thermostat.client.ui.HostMemoryView.GraphVisibilityChangeListener;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.model.MemoryStat;
import com.redhat.thermostat.common.model.MemoryType;

public class HostMemoryController {

    private final HostMemoryView view;

    private final HostInfoDAO hostInfoDAO;
    private final MemoryStatDAO memoryStatDAO;
    private final HostRef ref;

    private final Timer backgroundUpdateTimer;
    private final GraphVisibilityChangeListener listener = new ShowHideGraph();

    public HostMemoryController(final HostRef ref) {
        this.ref = ref;
        DAOFactory daos = ApplicationContext.getInstance().getDAOFactory();
        hostInfoDAO = daos.getHostInfoDAO();
        memoryStatDAO = daos.getMemoryStatDAO();

        view = ApplicationContext.getInstance().getViewFactory().getView(HostMemoryView.class);

        view.addMemoryChart(MemoryType.MEMORY_TOTAL.name(), localize(LocaleResources.HOST_MEMORY_TOTAL));
        view.addMemoryChart(MemoryType.MEMORY_FREE.name(), localize(LocaleResources.HOST_MEMORY_FREE));
        view.addMemoryChart(MemoryType.MEMORY_USED.name(), localize(LocaleResources.HOST_MEMORY_USED));
        view.addMemoryChart(MemoryType.SWAP_TOTAL.name(), localize(LocaleResources.HOST_SWAP_TOTAL));
        view.addMemoryChart(MemoryType.SWAP_FREE.name(), localize(LocaleResources.HOST_SWAP_FREE));
        view.addMemoryChart(MemoryType.BUFFERS.name(), localize(LocaleResources.HOST_BUFFERS));

        view.addGraphVisibilityListener(listener);
        view.addActionListener(new ActionListener<HostMemoryView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        stopBackgroundUpdates();
                        break;
                    case VISIBLE:
                        startBackgroundUpdates();
                        break;
                    default:
                        throw new NotImplementedException("action event not handled: " + actionEvent.getActionId());
                }
            }
        });

        backgroundUpdateTimer = ApplicationContext.getInstance().getTimerFactory().createTimer();
        backgroundUpdateTimer.setAction(new Runnable() {
            @Override
            public void run() {
                view.setTotalMemory(String.valueOf(hostInfoDAO.getHostInfo(ref).getTotalMemory()));
                doMemoryChartUpdate();
            }
        });
        backgroundUpdateTimer.setSchedulingType(SchedulingType.FIXED_RATE);
        backgroundUpdateTimer.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdateTimer.setInitialDelay(0);
        backgroundUpdateTimer.setDelay(5);
    }

    private void startBackgroundUpdates() {
        for (MemoryType type : MemoryType.values()) {
            view.showMemoryChart(type.name());
        }

        backgroundUpdateTimer.start();
    }

    private void stopBackgroundUpdates() {
        backgroundUpdateTimer.stop();
        for (MemoryType type : MemoryType.values()) {
            view.hideMemoryChart(type.name());
        }
    }

    public Component getComponent() {
        return view.getUiComponent();
    }

    private void doMemoryChartUpdate() {
        List<DiscreteTimeData<? extends Number>> memFree = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> memTotal = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> memUsed = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> buf = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> swapTotal = new LinkedList<>();
        List<DiscreteTimeData<? extends Number>> swapFree = new LinkedList<>();

        for (MemoryStat stat : memoryStatDAO.getLatestMemoryStats(ref)) {
            long timeStamp = stat.getTimeStamp();
            memFree.add(new DiscreteTimeData<Long>(timeStamp, stat.getFree()));
            memTotal.add(new DiscreteTimeData<Long>(timeStamp, stat.getTotal()));
            memUsed.add(new DiscreteTimeData<Long>(timeStamp, stat.getTotal() - stat.getFree()));
            buf.add(new DiscreteTimeData<Long>(timeStamp, stat.getBuffers()));
            swapTotal.add(new DiscreteTimeData<Long>(timeStamp, stat.getSwapTotal()));
            swapFree.add(new DiscreteTimeData<Long>(timeStamp, stat.getSwapFree()));
        }

        view.addMemoryData(MemoryType.MEMORY_FREE.name(), memFree);
        view.addMemoryData(MemoryType.MEMORY_TOTAL.name(), memTotal);
        view.addMemoryData(MemoryType.MEMORY_USED.name(), memUsed);
        view.addMemoryData(MemoryType.BUFFERS.name(), buf);
        view.addMemoryData(MemoryType.SWAP_FREE.name(), swapFree);
        view.addMemoryData(MemoryType.SWAP_TOTAL.name(), swapTotal);
    }

    private class ShowHideGraph implements GraphVisibilityChangeListener {
        @Override
        public void show(String tag) {
            view.showMemoryChart(tag);
        }
        @Override
        public void hide(String tag) {
            view.hideMemoryChart(tag);
        }
    }
}
