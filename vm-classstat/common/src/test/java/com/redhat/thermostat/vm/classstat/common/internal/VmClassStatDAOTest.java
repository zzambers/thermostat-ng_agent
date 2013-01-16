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

package com.redhat.thermostat.vm.classstat.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.VmClassStat;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;

public class VmClassStatDAOTest {

    private static final Long TIMESTAMP = 1234L;
    private static final Integer VM_ID = 123;
    private static final Long LOADED_CLASSES = 12345L;

    @Test
    public void testCategory() {
        assertEquals("vm-class-stats", VmClassStatDAO.vmClassStatsCategory.getName());
        Collection<Key<?>> keys = VmClassStatDAO.vmClassStatsCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<Integer>("vmId", true)));
        assertTrue(keys.contains(new Key<Long>("timeStamp", false)));
        assertTrue(keys.contains(new Key<Long>("loadedClasses", false)));
        assertEquals(4, keys.size());
    }

    @Test
    public void testGetLatestClassStatsBasic() {

        VmClassStat vmClassStat = getClassStat();

        @SuppressWarnings("unchecked")
        Cursor<VmClassStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(vmClassStat);

        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class))).thenReturn(query);
        when(query.execute()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(321);

        VmClassStatDAO dao = new VmClassStatDAOImpl(storage);
        List<VmClassStat> vmClassStats = dao.getLatestClassStats(vmRef, Long.MIN_VALUE);

        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, "system");
        verify(query).where(Key.VM_ID, Criteria.EQUALS, 321);
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verify(query).execute();
        verifyNoMoreInteractions(query);

        assertEquals(1, vmClassStats.size());
        VmClassStat stat = vmClassStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(LOADED_CLASSES, (Long) stat.getLoadedClasses());
        assertEquals(VM_ID, (Integer) stat.getVmId());
    }

    private VmClassStat getClassStat() {
        return new VmClassStat(VM_ID, TIMESTAMP, LOADED_CLASSES);
    }

    @Test
    public void testPutVmClassStat() {

        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);

        VmClassStat stat = new VmClassStat(VM_ID, TIMESTAMP, LOADED_CLASSES);
        VmClassStatDAO dao = new VmClassStatDAOImpl(storage);
        dao.putVmClassStat(stat);

        verify(storage).createAdd(VmClassStatDAO.vmClassStatsCategory);
        verify(add).setPojo(stat);
        verify(add).apply();
    }
}

