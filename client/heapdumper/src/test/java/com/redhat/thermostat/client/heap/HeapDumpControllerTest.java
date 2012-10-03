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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.heap.HeapView.HeapDumperAction;
import com.redhat.thermostat.client.heap.cli.HeapDumperCommand;
import com.redhat.thermostat.client.osgi.service.ApplicationCache;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.model.HeapInfo;

public class HeapDumpControllerTest {

    private ActionListener<HeapView.Action> actionListener;
    private ActionListener<HeapView.HeapDumperAction> heapDumperListener;
    
    private Timer timer;
    
    private AgentInfoDAO agentDao;
    private HeapDAO heapDAO;
    private VmMemoryStatDAO vmDao;
    private HeapView view;
    private HeapDumpDetailsView detailsView;
    
    private HeapDumpController controller;
    private HeapDumperCommand heapDumperCommand;
    private ApplicationService appService;
    private ArgumentCaptor<Runnable> timerActionCaptor;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        VmClassStatDAO vmClassStatDAO = mock(VmClassStatDAO.class);
        
        DAOFactory daoFactory = mock(MongoDAOFactory.class);
        when(daoFactory.getVmClassStatsDAO()).thenReturn(vmClassStatDAO);
        
        agentDao = mock(AgentInfoDAO.class);
        heapDAO = mock(HeapDAO.class);
        when(daoFactory.getHeapDAO()).thenReturn(heapDAO);
        vmDao = mock(VmMemoryStatDAO.class);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        setUpTimers();
        setUpView();
    }
    
    private void setUpTimers() {
        timer = mock(Timer.class);
        timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);
    }
    
    private void setUpView() {
        ViewFactory viewFactory = mock(ViewFactory.class);

        view = mock(HeapView.class);
        when(viewFactory.getView(eq(HeapView.class))).thenReturn(view);

        detailsView = mock(HeapDumpDetailsView.class);
        when(viewFactory.getView(HeapDumpDetailsView.class)).thenReturn(detailsView);
        
        HeapHistogramView histogramView = mock(HeapHistogramView.class);
        when(viewFactory.getView(HeapHistogramView.class)).thenReturn(histogramView);

        ObjectDetailsView objectDetailsView = mock(ObjectDetailsView.class);
        when(viewFactory.getView(ObjectDetailsView.class)).thenReturn(objectDetailsView);

        ApplicationContext.getInstance().setViewFactory(viewFactory);
    }
    
    private void setUpListeners() {        
        ArgumentCaptor<ActionListener> viewArgumentCaptor1 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor1.capture());
        
        ArgumentCaptor<ActionListener> viewArgumentCaptor2 = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addDumperListener(viewArgumentCaptor2.capture());
        
        createController();
        
        actionListener = viewArgumentCaptor1.getValue();
        heapDumperListener = viewArgumentCaptor2.getValue();
    }
    
    private void createController() {
        ApplicationCache cache = mock(ApplicationCache.class);
        appService = mock(ApplicationService.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        VmRef ref = mock(VmRef.class);
        heapDumperCommand = mock(HeapDumperCommand.class);
        controller = new HeapDumpController(agentDao, vmDao, ref, appService, heapDumperCommand);
    }
    
    @After
    public void tearDown() {
    	controller = null;
    	vmDao = null;
    	heapDAO = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testTimerStartOnViewVisible() {
        
        setUpListeners();

        actionListener.actionPerformed(new ActionEvent<>(view, HeapView.Action.VISIBLE));
        verify(timer).start();
    }

    @Test
    public void testTimerStopsOnViewHidden() {
        
        setUpListeners();
        
        actionListener.actionPerformed(new ActionEvent<>(view, HeapView.Action.HIDDEN));
        verify(timer).stop();
    }
    
    @Test
    public void testNotAddHeapDumpsAtStartupWhenNoDumps() {
                
        when(heapDAO.getAllHeapInfo(any(VmRef.class))).thenReturn(new ArrayList<HeapInfo>());
        
        createController();
        
        verify(view, times(0)).addHeapDump(any(HeapDump.class));
    }
    
    @Test
    public void testAddHeapDumpsAtStartupWhenDumpsAreThere() {
        HeapInfo info1 = mock(HeapInfo.class);
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDAO.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);

        createController();
        
        verify(view, times(2)).addHeapDump(any(HeapDump.class));
    }
    
    @Test
    public void testOpenDumpCalledWhenPreviousDump() {
        
        HeapDump dump = mock(HeapDump.class);
        
        HeapInfo info1 = mock(HeapInfo.class);
        when(dump.getInfo()).thenReturn(info1);
        
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDAO.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);
        
        ApplicationCache cache = mock(ApplicationCache.class);
        when(cache.getAttribute(any(VmRef.class))).thenReturn(dump);
        
        appService = mock(ApplicationService.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        VmRef ref = mock(VmRef.class);
        controller = new HeapDumpController(agentDao, vmDao, ref, appService);
        
        verify(view, times(1)).setChildView(any(HeapView.class));
        verify(view, times(1)).openDumpView();
    }
    
    @Test
    public void testNotOpenDumpCalledWhenNoPreviousDump() {

        HeapInfo info1 = mock(HeapInfo.class);        
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDAO.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);
        
        ApplicationCache cache = mock(ApplicationCache.class);
        when(cache.getAttribute(any(VmRef.class))).thenReturn(null);
        
        appService = mock(ApplicationService.class);
        when(appService.getApplicationCache()).thenReturn(cache);
        VmRef ref = mock(VmRef.class);
        controller = new HeapDumpController(agentDao, vmDao, ref, appService);
        
        verify(view, times(0)).openDumpView();
    }

    @Test
    public void testRequestHeapDump() {

        setUpListeners();

        heapDumperListener.actionPerformed(new ActionEvent<HeapDumperAction>(view, HeapDumperAction.DUMP_REQUESTED));

        ArgumentCaptor<Runnable> heapDumpCompleteAction = ArgumentCaptor.forClass(Runnable.class);
        verify(heapDumperCommand).execute(same(agentDao), any(VmRef.class), heapDumpCompleteAction.capture());
        heapDumpCompleteAction.getValue().run();
        verify(view).notifyHeapDumpComplete();

    }
 
    @SuppressWarnings("unchecked")
	@Test
    public void testTimerChecksForNewHeapDumps() {

        HeapInfo info1 = mock(HeapInfo.class);
        HeapInfo info2 = mock(HeapInfo.class);
        Collection<HeapInfo> infos = new ArrayList<HeapInfo>();
        infos.add(info1);
        infos.add(info2);
        
        when(heapDAO.getAllHeapInfo(any(VmRef.class))).thenReturn(infos);

        createController();

        timerActionCaptor.getValue().run();

        @SuppressWarnings("rawtypes")
		ArgumentCaptor<List> heapDumps = ArgumentCaptor.forClass(List.class);
        verify(view).updateHeapDumpList(heapDumps.capture());
        assertTrue(heapDumps.getValue().contains(new HeapDump(info1, heapDAO)));
        assertTrue(heapDumps.getValue().contains(new HeapDump(info2, heapDAO)));
    }
}
