/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.client.core.internal;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.gc.client.core.VmGcView;
import com.redhat.thermostat.vm.gc.client.core.VmGcViewProvider;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;

public class VmGcControllerTest {

    private Timer timer;
    private Runnable timerAction;
    private VmGcView view;
    private ActionListener<VmGcView.Action> viewListener;
    private VmInfoDAO vmInfoDAO;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() {

        // Setup Timer
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        // Set up fake data
        List<VmGcStat> stats = new ArrayList<>();
        VmGcStat stat1 = new VmGcStat("foo-agent", "vmId", 1, "collector1", 1, 10);
        VmGcStat stat2 = new VmGcStat("foo-agent", "vmId", 2, "collector1", 5, 20);
        stats.add(stat1);
        stats.add(stat2);

        Generation gen;
        gen = new Generation();
        gen.setName("generation 1");
        gen.setCollector("collector1");
        VmMemoryStat memoryStat = new VmMemoryStat("foo-agent", 1, "vmId", new Generation[] { gen }, 2,3,4,5);

        VmInfo vmInfo = new VmInfo("foo", "vm1", 1, 0, -1, "1.8.0_45", "", "", "", "", "", "", "", null, null, null, -1, null);

        AgentInformation agentInfo = new AgentInformation("foo");

        // Setup DAO
        VmGcStatDAO vmGcStatDAO = mock(VmGcStatDAO.class);
        when(vmGcStatDAO.getLatestVmGcStats(isA(VmRef.class), isA(Long.class))).thenReturn(stats);
        VmMemoryStatDAO vmMemoryStatDAO = mock(VmMemoryStatDAO.class);
        when(vmMemoryStatDAO.getNewestMemoryStat(isA(VmRef.class))).thenReturn(memoryStat);
        vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAgentInformation(isA(AgentId.class))).thenReturn(agentInfo);

        // the following set should map to Concurrent Collector
        Set<String> cms = new HashSet<>();
        cms.add("CMS");
        cms.add("PCopy");
        
        when(vmGcStatDAO.getDistinctCollectorNames(isA(VmRef.class))).thenReturn(cms);

        // Setup View
        view = mock(VmGcView.class);
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        when(view.getUserDesiredDuration()).thenReturn(new Duration(1, TimeUnit.MINUTES));

        VmGcViewProvider viewProvider = mock(VmGcViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        GCRequest gcRequest = mock(GCRequest.class);

        // Now start the controller
        VmRef ref = mock(VmRef.class);

        new VmGcController(appSvc, vmMemoryStatDAO, vmGcStatDAO, vmInfoDAO, agentInfoDAO, ref, viewProvider, gcRequest);

        // Extract relevant objects
        viewListener = viewArgumentCaptor.getValue();
        timerAction = timerActionCaptor.getValue();
    }

    @Test
    public void verifyTimer() {
        verify(timer).setAction(isNotNull(Runnable.class));
        verify(timer).setAction(isA(Runnable.class));
        verify(timer).setDelay(5);
        verify(timer).setInitialDelay(0);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);

    }

    @Test
    public void verifyStartAndStop() {
        viewListener.actionPerformed(new ActionEvent<>(view, VmGcView.Action.VISIBLE));

        verify(timer).start();

        viewListener.actionPerformed(new ActionEvent<>(view, VmGcView.Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void verifyGcEnabled() {
        viewListener.actionPerformed(new ActionEvent<>(view, VmGcView.Action.VISIBLE));
        verify(view, atLeastOnce()).setEnableGCAction(true);
    }

    @Test
    public void verifyGcDisabledWhenVmDead() {
        VmInfo stoppedVmInfo = mock(VmInfo.class);
        when(stoppedVmInfo.isAlive()).thenReturn(false);
        when(vmInfoDAO.getVmInfo(isA(VmRef.class))).thenReturn(stoppedVmInfo);

        viewListener.actionPerformed(new ActionEvent<>(view, VmGcView.Action.VISIBLE));
        verify(view, atLeastOnce()).setEnableGCAction(false);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyAction() {
        timerAction.run();

        verify(view).addData(isA(String.class), isA(List.class));
        verify(view).setCollectorInfo(eq(CollectorCommonName.CONCURRENT_COLLECTOR), eq("1.8.0_45"));
    }

}

