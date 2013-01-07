/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.gc.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;

public class VmGcHostListenerTest {
    
    private VmGcHostListener hostListener;
    private MonitoredHost host;
    private MonitoredVm monitoredVm1;
    private MonitoredVm monitoredVm2;

    @Before
    public void setup() throws MonitorException, URISyntaxException {
        VmGcStatDAO vmGcStatDAO = mock(VmGcStatDAO.class);
        hostListener = new VmGcHostListener(vmGcStatDAO, true);
        
        host = mock(MonitoredHost.class);
        HostIdentifier hostId = mock(HostIdentifier.class);
        monitoredVm1 = mock(MonitoredVm.class);
        monitoredVm2 = mock(MonitoredVm.class);
        VmIdentifier vmId1 = new VmIdentifier("1");
        VmIdentifier vmId2 = new VmIdentifier("2");
        when(host.getHostIdentifier()).thenReturn(hostId);
        when(host.getMonitoredVm(eq(vmId1))).thenReturn(monitoredVm1);
        when(host.getMonitoredVm(eq(vmId2))).thenReturn(monitoredVm2);
        when(hostId.resolve(eq(vmId1))).thenReturn(vmId1);
        when(hostId.resolve(eq(vmId2))).thenReturn(vmId2);
    }
    
    @Test
    public void testNewVM() throws InterruptedException, MonitorException {
        startVMs();
        
        assertTrue(hostListener.getMonitoredVms().containsKey(1));
        assertTrue(hostListener.getMonitoredVms().containsKey(2));
        assertEquals(monitoredVm1, hostListener.getMonitoredVms().get(1));
        assertEquals(monitoredVm2, hostListener.getMonitoredVms().get(2));
        
        assertTrue(hostListener.getRegisteredListeners().containsKey(monitoredVm1));
        assertTrue(hostListener.getRegisteredListeners().containsKey(monitoredVm2));
    }
    
    @Test
    public void testStoppedVM() throws InterruptedException, MonitorException {
        final Set<Integer> stopped = new HashSet<>();
        stopped.add(1);
        
        startVMs();
        
        // Trigger a change event
        VmStatusChangeEvent event = mock(VmStatusChangeEvent.class);
        when(event.getMonitoredHost()).thenReturn(host);
        when(event.getStarted()).thenReturn(Collections.emptySet());
        when(event.getTerminated()).thenReturn(stopped);
        hostListener.vmStatusChanged(event);
        
        // Ensure only 1 removed
        assertFalse(hostListener.getMonitoredVms().containsKey(1));
        assertTrue(hostListener.getMonitoredVms().containsKey(2));
        assertEquals(monitoredVm2, hostListener.getMonitoredVms().get(2));
        
        assertFalse(hostListener.getRegisteredListeners().containsKey(monitoredVm1));
        assertTrue(hostListener.getRegisteredListeners().containsKey(monitoredVm2));
    }

    private void startVMs() throws InterruptedException, MonitorException {
        final Set<Integer> started = new HashSet<>();
        started.add(1);
        started.add(2);

        // Trigger a change event
        VmStatusChangeEvent event = mock(VmStatusChangeEvent.class);
        when(event.getMonitoredHost()).thenReturn(host);
        when(event.getStarted()).thenReturn(started);
        when(event.getTerminated()).thenReturn(Collections.emptySet());
        hostListener.vmStatusChanged(event);
    }
}
