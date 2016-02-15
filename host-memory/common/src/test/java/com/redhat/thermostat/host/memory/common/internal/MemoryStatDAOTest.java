/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.host.memory.common.internal;

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
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.host.memory.common.MemoryStatDAO;
import com.redhat.thermostat.host.memory.common.model.MemoryStat;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

public class MemoryStatDAOTest {

    private static long TIMESTAMP = 1;
    private static long TOTAL = 2;
    private static long FREE = 3;
    private static long BUFFERS = 4;
    private static long CACHED = 5;
    private static long SWAP_TOTAL = 6;
    private static long SWAP_FREE = 7;
    private static long COMMIT_LIMIT = 8;
    
    @Test
    public void testDescriptorsAreSane() {
        String addMemoryStat = "ADD memory-stats SET 'agentId' = ?s , " +
                "'timeStamp' = ?l , " +
                "'total' = ?l , " +
                "'free' = ?l , " +
                "'buffers' = ?l , " +
                "'cached' = ?l , " +
                "'swapTotal' = ?l , " +
                "'swapFree' = ?l , " +
                "'commitLimit' = ?l";
        assertEquals(addMemoryStat, MemoryStatDAOImpl.DESC_ADD_MEMORY_STAT);
    }

    @Test
    public void testCategory() {
        assertEquals("memory-stats", MemoryStatDAO.memoryStatCategory.getName());
        Collection<Key<?>> keys = MemoryStatDAO.memoryStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<Long>("total")));
        assertTrue(keys.contains(new Key<Long>("free")));
        assertTrue(keys.contains(new Key<Long>("buffers")));
        assertTrue(keys.contains(new Key<Long>("cached")));
        assertTrue(keys.contains(new Key<Long>("swapTotal")));
        assertTrue(keys.contains(new Key<Long>("swapFree")));
        assertTrue(keys.contains(new Key<Long>("commitLimit")));
        assertEquals(9, keys.size());
    }

    @Test
    public void testGetLatestMemoryStats() throws DescriptorParsingException, StatementExecutionException {

        String agentId = "system";
        MemoryStat memStat1 = new MemoryStat(agentId, TIMESTAMP, TOTAL, FREE, BUFFERS, CACHED, SWAP_TOTAL, SWAP_FREE, COMMIT_LIMIT);

        @SuppressWarnings("unchecked")
        Cursor<MemoryStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(memStat1);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<MemoryStat> stmt = (PreparedStatement<MemoryStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn(agentId);

        MemoryStatDAO dao = new MemoryStatDAOImpl(storage);
        List<MemoryStat> memoryStats = dao.getLatestMemoryStats(hostRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setLong(1, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, memoryStats.size());
        MemoryStat stat = memoryStats.get(0);

        assertEquals(TIMESTAMP, stat.getTimeStamp());
        assertEquals(TOTAL, stat.getTotal());
        assertEquals(FREE, stat.getFree());
        assertEquals(BUFFERS, stat.getBuffers());
        assertEquals(CACHED, stat.getCached());
        assertEquals(SWAP_TOTAL, stat.getSwapTotal());
        assertEquals(SWAP_FREE, stat.getSwapFree());
        assertEquals(COMMIT_LIMIT, stat.getCommitLimit());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<MemoryStat> anyDescriptor() {
        return (StatementDescriptor<MemoryStat>) any(StatementDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutMemoryStat() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<MemoryStat> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        MemoryStat stat = new MemoryStat("foo", TIMESTAMP, TOTAL, FREE, BUFFERS, CACHED, SWAP_TOTAL, SWAP_FREE, COMMIT_LIMIT);
        MemoryStatDAO dao = new MemoryStatDAOImpl(storage);
        dao.putMemoryStat(stat);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(MemoryStatDAOImpl.DESC_ADD_MEMORY_STAT, desc.getDescriptor());
        verify(add).setString(0, stat.getAgentId());
        verify(add).setLong(1, stat.getTimeStamp());
        verify(add).setLong(2, stat.getTotal());
        verify(add).setLong(3, stat.getFree());
        verify(add).setLong(4, stat.getBuffers());
        verify(add).setLong(5, stat.getCached());
        verify(add).setLong(6, stat.getSwapTotal());
        verify(add).setLong(7, stat.getSwapFree());
        verify(add).setLong(8, stat.getCommitLimit());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }
    
}

