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

package com.redhat.thermostat.vm.memory.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat.Space;

public class VmMemoryStatDAOTest {

    private static final int VM_ID = 0xcafe;
    private static final String AGENT_ID = "agent";

    private Storage storage;
    private VmRef vmRef;

    private PreparedStatement<VmMemoryStat> stmt;
    private Cursor<VmMemoryStat> cursor;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws DescriptorParsingException, StatementExecutionException {
        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn(AGENT_ID);

        vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(VM_ID);

        storage = mock(Storage.class);
        stmt = (PreparedStatement<VmMemoryStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);

        cursor = (Cursor<VmMemoryStat>) mock(Cursor.class);
        when(stmt.executeQuery()).thenReturn(cursor);

        when(cursor.hasNext()).thenReturn(false);
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmMemoryStat> anyDescriptor() {
        return (StatementDescriptor<VmMemoryStat>) any(StatementDescriptor.class);
    }

    @After
    public void tearDown() {
        stmt = null;
        vmRef = null;
        cursor = null;
        storage = null;
    }

    @Test
    public void testCategories() {
        Collection<Key<?>> keys;

        assertEquals("vm-memory-stats", VmMemoryStatDAO.vmMemoryStatsCategory.getName());
        keys = VmMemoryStatDAO.vmMemoryStatsCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<Integer>("vmId", true)));
        assertTrue(keys.contains(new Key<Long>("timeStamp", false)));
        assertTrue(keys.contains(new Key<Generation[]>("generations", false)));
        assertEquals(4, keys.size());
    }

    @Test
    public void testGetLatest() throws DescriptorParsingException, StatementExecutionException {
        VmMemoryStatDAO impl = new VmMemoryStatDAOImpl(storage);
        impl.getLatestMemoryStat(vmRef);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, vmRef.getAgent().getAgentId());
        verify(stmt).setInt(1, vmRef.getId());
        verify(stmt).executeQuery();
    }

    @Test
    public void testGetLatestSince() throws DescriptorParsingException, StatementExecutionException {
        VmMemoryStatDAO impl = new VmMemoryStatDAOImpl(storage);
        impl.getLatestVmMemoryStats(vmRef, 123L);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, vmRef.getAgent().getAgentId());
        verify(stmt).setInt(1, vmRef.getId());
        verify(stmt).setLong(2, 123L);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
    }

    @Test
    public void testGetLatestReturnsNullWhenStorageEmpty() throws DescriptorParsingException, StatementExecutionException {
        when(cursor.hasNext()).thenReturn(false);
        when(cursor.next()).thenReturn(null);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmMemoryStat> stmt = (PreparedStatement<VmMemoryStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        VmMemoryStatDAO impl = new VmMemoryStatDAOImpl(storage);
        VmMemoryStat latest = impl.getLatestMemoryStat(vmRef);
        assertTrue(latest == null);
    }

    @Test
    public void testPutVmMemoryStat() {

        List<Generation> generations = new ArrayList<Generation>();

        int i = 0;
        for (String genName: new String[] { "new", "old", "perm" }) {
            Generation gen = new Generation();
            gen.setName(genName);
            gen.setCollector(gen.getName());
            generations.add(gen);
            List<Space> spaces = new ArrayList<Space>();
            String[] spaceNames = null;
            if (genName.equals("new")) {
                spaceNames = new String[] { "eden", "s0", "s1" };
            } else if (genName.equals("old")) {
                spaceNames = new String[] { "old" };
            } else {
                spaceNames = new String[] { "perm" };
            }
            for (String spaceName: spaceNames) {
                Space space = new Space();
                space.setName(spaceName);
                space.setIndex(0);
                space.setUsed(i++);
                space.setCapacity(i++);
                space.setMaxCapacity(i++);
                spaces.add(space);
            }
            gen.setSpaces(spaces.toArray(new Space[spaces.size()]));
        }
        VmMemoryStat stat = new VmMemoryStat(1, 2, generations.toArray(new Generation[generations.size()]));

        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);
        
        VmMemoryStatDAO dao = new VmMemoryStatDAOImpl(storage);
        dao.putVmMemoryStat(stat);

        verify(storage).createAdd(VmMemoryStatDAO.vmMemoryStatsCategory);
        verify(add).setPojo(stat);
        verify(add).apply();
    }
    
}

