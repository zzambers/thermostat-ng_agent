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

package com.redhat.thermostat.client.heap;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.heap.HeapView.HeadDumperAction;
import com.redhat.thermostat.client.heap.chart.OverviewChart;
import com.redhat.thermostat.client.heap.cli.HeapDumperCommand;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.osgi.service.VmInformationServiceController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.BasicView;
import com.redhat.thermostat.common.BasicView.Action;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.utils.DisplayableValues.Scale;

public class HeapDumpController implements VmInformationServiceController {

    private final VmMemoryStatDAO vmDao;
    private final VmRef ref;
    
    private final HeapDAO heapDAO;
        
    private HeapView view;
    private final Timer timer;
    
    private OverviewChart model;
    private ApplicationService appService;

    public HeapDumpController(final VmRef ref, final ApplicationService appService) {
        
        this.appService = appService;
        this.ref = ref;
        this.vmDao = ApplicationContext.getInstance().getDAOFactory().getVmMemoryStatDAO();
        this.heapDAO = ApplicationContext.getInstance().getDAOFactory().getHeapDAO();
        
        model = new OverviewChart(
                    Translate.localize(LocaleResources.HEAP_CHART_TITLE),
                    Translate.localize(LocaleResources.HEAP_CHART_TIME_AXIS),
                    Translate.localize(LocaleResources.HEAP_CHART_HEAP_AXIS),
                    Translate.localize(LocaleResources.HEAP_CHART_CAPACITY),
                    Translate.localize(LocaleResources.HEAP_CHART_USED));
        
        timer = ApplicationContext.getInstance().getTimerFactory().createTimer();
        timer.setAction(new HeapOverviewDataCollector());
        
        timer.setInitialDelay(0);
        timer.setDelay(1000);
        model.setRange(3600);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        
        view = ApplicationContext.getInstance().getViewFactory().getView(HeapView.class);
        
        HeapDump dump = null;
        view.clearHeapDumpList();
        Collection<HeapInfo> infos = heapDAO.getAllHeapInfo(ref);
        for (HeapInfo info : infos) {
            dump = new HeapDump(info, heapDAO);
            view.addHeapDump(dump);
        }
        
        // check if we were reading some of the dumps
        dump = (HeapDump) appService.getApplicationCache().getAttribute(ref);
        if (dump != null && infos.contains(dump.getInfo())) {
            showHeapDumpDetails(dump);
        }
        
        view.addActionListener(new ActionListener<Action>() {            
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case HIDDEN:
                    timer.stop();
                    break;
                
                case VISIBLE:                    
                    timer.start();
                    break;

                default:
                    throw new NotImplementedException("unknown event: " + actionEvent.getActionId());
                }
            }
        });
        
        final HeapDumperCommand command = new HeapDumperCommand();
        view.addDumperListener(new ActionListener<HeapView.HeadDumperAction>() {
            @Override
            public void actionPerformed(ActionEvent<HeadDumperAction> actionEvent) {
                HeapDump dump = null;
                switch (actionEvent.getActionId()) {
                case DUMP_REQUESTED:
                    dump = command.execute(ref);
                    view.addHeapDump(dump);
                    
                    // also, only if it's the fist dump, we jump there
                    // it doesn't disrupt the workflow if it's the first dump
                    if (appService.getApplicationCache().getAttribute(ref) == null) {
                        analyseDump(dump);
                    }
                    
                    break;
                
                case ANALYSE:
                    dump = (HeapDump) actionEvent.getPayload();
                    analyseDump(dump);
                    break;
                }
            }
        });
    }

    
    private void analyseDump(final HeapDump dump) {
        appService.getApplicationExecutor().execute(new Runnable() {
            
            @Override
            public void run() {
                showHeapDumpDetails(dump);
                appService.getApplicationCache().addAttribute(ref, dump);
            }
        });
    }

    private void showHeapDumpDetails(HeapDump dump) {
        HeapDumpDetailsController controller = new HeapDumpDetailsController();
        controller.setDump(dump);
        view.setChildView(controller.getView());
        view.openDumpView();
    }

    @Override
    public BasicView getView() {
        return view;
    }

    @Override
    public String getLocalizedName() {
        return Translate.localize(LocaleResources.HEAP_SECTION_TITLE);
    }

    class HeapOverviewDataCollector implements Runnable {
        @Override
        public void run() {
            
            List<VmMemoryStat> vmInfo = vmDao.getLatestVmMemoryStats(ref, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
            for (VmMemoryStat memoryStats: vmInfo) {
                long used = 0l;
                long capacity = 0l;
                long max = 0l;
                List<Generation> generations = memoryStats.getGenerations();
                for (Generation generation : generations) {
                    
                    // non heap
                    if (generation.name.equals("perm")) {
                        continue;
                    }
                    
                    List<Space> spaces = generation.spaces;
                    for (Space space: spaces) {
                        used += space.used;
                        capacity += space.capacity;
                        
                        // TODO
                        max =+ space.maxCapacity;
                    }
                }
                model.addData(memoryStats.getTimeStamp(), used, capacity);
                
                NumberFormat formatter = DecimalFormat.getInstance();

                double res = Scale.convertTo(Scale.B, used);
                String _used = formatter.format(res) + " " + Scale.B;
                
                res = Scale.convertTo(Scale.B, capacity);
                String _capacity= formatter.format(capacity) + " " + Scale.B;
                
                view.updateOverview(model, _used, _capacity);
            }
        }
    }
}
