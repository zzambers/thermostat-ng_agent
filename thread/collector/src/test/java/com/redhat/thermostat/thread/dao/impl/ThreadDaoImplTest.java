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

package com.redhat.thermostat.thread.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class ThreadDaoImplTest {

    @Test
    public void testThreadDaoCategoryRegistration() {
        Storage storage = mock(Storage.class);
        
        @SuppressWarnings("unused")
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        
        verify(storage).registerCategory(ThreadDao.THREAD_CAPABILITIES);
        verify(storage).registerCategory(ThreadDao.THREAD_HARVESTING_STATUS);
        verify(storage).registerCategory(ThreadDao.THREAD_INFO);
        verify(storage).registerCategory(ThreadDao.THREAD_SUMMARY);
    }
    
    @Test
    public void testLoadVMCapabilities() {
        Query query = mock(Query.class);
        Storage storage = mock(Storage.class);
        when(storage.createQuery(any(Category.class))).thenReturn(query);
        VmRef ref = mock(VmRef.class);
        when(ref.getId()).thenReturn(42);
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        
        when(ref.getAgent()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities();
        expected.setSupportedFeaturesList(new String[] { ThreadDao.CPU_TIME, ThreadDao.THREAD_ALLOCATED_MEMORY });
        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(expected).thenReturn(null);
        when(query.execute()).thenReturn(cursor);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VMThreadCapabilities caps = dao.loadCapabilities(ref);

        verify(query).where(Key.VM_ID, Criteria.EQUALS, 42);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, "0xcafe");
        verify(query).limit(1);
        verify(query).execute();
        verifyNoMoreInteractions(query);

        assertFalse(caps.supportContentionMonitor());
        assertTrue(caps.supportCPUTime());
        assertTrue(caps.supportThreadAllocatedMemory());
    }
    
    @Test
    public void testLoadVMCapabilitiesWithoutAnyDataInStorage() {
        Query query = mock(Query.class);
        Storage storage = mock(Storage.class);
        when(storage.createQuery(any(Category.class))).thenReturn(query);
        VmRef ref = mock(VmRef.class);
        when(ref.getId()).thenReturn(42);

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");

        when(ref.getAgent()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities();
        expected.setSupportedFeaturesList(new String[] { ThreadDao.CPU_TIME, ThreadDao.THREAD_ALLOCATED_MEMORY });
        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(cursor.next()).thenThrow(new NoSuchElementException());
        when(query.execute()).thenReturn(cursor);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VMThreadCapabilities caps = dao.loadCapabilities(ref);

        verify(query).where(Key.VM_ID, Criteria.EQUALS, 42);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, "0xcafe");
        verify(query).limit(1);
        verify(query).execute();
        verifyNoMoreInteractions(query);

        assertEquals(null, caps);
    }

    @Test
    public void testSaveVMCapabilities() {
        Storage storage = mock(Storage.class);
        Replace replace = mock(Replace.class);
        when(storage.createReplace(any(Category.class))).thenReturn(replace);

        VMThreadCapabilities caps = mock(VMThreadCapabilities.class);
        when(caps.supportContentionMonitor()).thenReturn(true);
        when(caps.supportCPUTime()).thenReturn(true);
        when(caps.supportThreadAllocatedMemory()).thenReturn(true);
        when(caps.getVmId()).thenReturn(42);
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveCapabilities(caps);

        verify(storage).createReplace(ThreadDao.THREAD_CAPABILITIES);
        verify(replace).setPojo(caps);
        verify(replace).apply();
    }

    @Test
    public void testLoadLatestDeadLockStatus() {
        VmRef vm = mock(VmRef.class);
        when(vm.getId()).thenReturn(42);
        when(vm.getIdString()).thenReturn("42");

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        when(vm.getAgent()).thenReturn(agent);

        Storage storage = mock(Storage.class);
        Query<VmDeadLockData> query = mock(Query.class);
        Cursor<VmDeadLockData> cursor = mock(Cursor.class);
        VmDeadLockData data = mock(VmDeadLockData.class);

        when(cursor.hasNext()).thenReturn(true);
        when(cursor.next()).thenReturn(data);
        when(query.execute()).thenReturn(cursor);

        when(storage.createQuery(ThreadDaoImpl.DEADLOCK_INFO)).thenReturn(query);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VmDeadLockData result = dao.loadLatestDeadLockStatus(vm);

        assertSame(data, result);

        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, agent.getAgentId());
        verify(query).where(Key.VM_ID, Criteria.EQUALS, vm.getId());
        verify(query).sort(Key.TIMESTAMP, SortDirection.DESCENDING);
        verify(query).execute();
        verify(query).limit(1);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testSaveDeadLockStatus() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(ThreadDaoImpl.DEADLOCK_INFO)).thenReturn(add);

        VmDeadLockData status = mock(VmDeadLockData.class);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveDeadLockStatus(status);

        verify(add).setPojo(status);
        verify(add).apply();
    }

    @Test
    public void testGetLatestHarvestingStatus() {
        VmRef vm = mock(VmRef.class);
        when(vm.getId()).thenReturn(42);
        when(vm.getIdString()).thenReturn("42");

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        when(vm.getAgent()).thenReturn(agent);

        Storage storage = mock(Storage.class);
        Query<ThreadHarvestingStatus> query = mock(Query.class);
        Cursor<ThreadHarvestingStatus> cursor = mock(Cursor.class);
        ThreadHarvestingStatus status = mock(ThreadHarvestingStatus.class);

        when(cursor.hasNext()).thenReturn(true);
        when(cursor.next()).thenReturn(status);
        when(query.execute()).thenReturn(cursor);

        when(storage.createQuery(ThreadDaoImpl.THREAD_HARVESTING_STATUS)).thenReturn(query);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        ThreadHarvestingStatus result = dao.getLatestHarvestingStatus(vm);

        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, agent.getAgentId());
        verify(query).where(Key.VM_ID, Criteria.EQUALS, vm.getId());
        verify(query).sort(Key.TIMESTAMP, SortDirection.DESCENDING);
        verify(query).execute();
        verify(query).limit(1);
        verifyNoMoreInteractions(query);

        assertSame(status, result);
    }

    @Test
    public void testSetHarvestingStatus() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(ThreadDaoImpl.THREAD_HARVESTING_STATUS)).thenReturn(add);

        ThreadHarvestingStatus status = mock(ThreadHarvestingStatus.class);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveHarvestingStatus(status);

        verify(add).setPojo(status);
        verify(add).apply();
    }
}

