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

package com.redhat.thermostat.vm.cpu.agent.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import sun.jvmstat.monitor.event.VmStatusChangeEvent;

public class VmCpuHostListenerTest {
    
    private VmCpuHostListener hostListener;
    private VmCpuStatBuilder builder;

    @Before
    public void setup() {
        builder = mock(VmCpuStatBuilder.class);
        
        hostListener = new VmCpuHostListener(builder);
    }

    @Test
    public void testNewVM() throws InterruptedException {
        startVMs();
        
        // Check that pids are added to set
        Set<Integer> pids = hostListener.getPidsToMonitor();
        assertTrue(pids.contains(1));
        assertTrue(pids.contains(2));
    }
    
    @Test
    public void testStoppedVM() throws InterruptedException {
        final Set<Integer> stopped = new HashSet<>();
        stopped.add(1);
        
        startVMs();
        
        // Trigger a change event
        VmStatusChangeEvent event = mock(VmStatusChangeEvent.class);
        when(event.getStarted()).thenReturn(Collections.emptySet());
        when(event.getTerminated()).thenReturn(stopped);
        hostListener.vmStatusChanged(event);
        
        // Ensure only 1 removed
        verify(builder).forgetAbout(1);
        verify(builder, never()).forgetAbout(2);
    }

    private void startVMs() throws InterruptedException {
        final Set<Integer> started = new HashSet<>();
        started.add(1);
        started.add(2);
        
        // Trigger a change event
        VmStatusChangeEvent event = mock(VmStatusChangeEvent.class);
        when(event.getStarted()).thenReturn(started);
        when(event.getTerminated()).thenReturn(Collections.emptySet());
        hostListener.vmStatusChanged(event);
    }
    
}
