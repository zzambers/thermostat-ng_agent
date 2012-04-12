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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.VmEvent;

import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.model.VmClassStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class JvmStatVmClassListenerTest {

    private static final Integer VM_ID = 123;
    private static final Long LOADED_CLASSES = 1234L;

    @Test
    public void testMonitorUpdatedClassStat() throws Exception {

        VmClassStatDAO dao = mock(VmClassStatDAO.class);
        DAOFactory df = mock(DAOFactory.class);
        when(df.getVmClassStatsDAO()).thenReturn(dao);
        JvmStatVmClassListener l = new JvmStatVmClassListener(df, VM_ID);
        VmEvent vmEvent = mock(VmEvent.class);
        MonitoredVm monitoredVm = mock(MonitoredVm.class);
        Monitor m = mock(Monitor.class);
        when(m.getValue()).thenReturn(LOADED_CLASSES);
        when(monitoredVm.findByName("java.cls.loadedClasses")).thenReturn(m);
        when(vmEvent.getMonitoredVm()).thenReturn(monitoredVm);

        l.monitorsUpdated(vmEvent);

        ArgumentCaptor<VmClassStat> arg = ArgumentCaptor.forClass(VmClassStat.class);
        verify(dao).putVmClassStat(arg.capture());
        VmClassStat stat = arg.getValue();
        assertEquals(LOADED_CLASSES, (Long) stat.getLoadedClasses());
        assertEquals(VM_ID, (Integer) stat.getVmId());
    }

    @Test
    public void testMonitorUpdatedClassStatTwice() throws Exception {

        VmClassStatDAO dao = mock(VmClassStatDAO.class);
        DAOFactory df = mock(DAOFactory.class);
        when(df.getVmClassStatsDAO()).thenReturn(dao);
        JvmStatVmClassListener l = new JvmStatVmClassListener(df, VM_ID);
        VmEvent vmEvent = mock(VmEvent.class);
        MonitoredVm monitoredVm = mock(MonitoredVm.class);
        Monitor m = mock(Monitor.class);
        when(m.getValue()).thenReturn(LOADED_CLASSES);
        when(monitoredVm.findByName("java.cls.loadedClasses")).thenReturn(m);
        when(vmEvent.getMonitoredVm()).thenReturn(monitoredVm);

        l.monitorsUpdated(vmEvent);
        l.monitorsUpdated(vmEvent);

        // This checks a bug where the Category threw an IllegalStateException because the DAO
        // created a new one on each call, thus violating the unique guarantee of Category.
    }
}
