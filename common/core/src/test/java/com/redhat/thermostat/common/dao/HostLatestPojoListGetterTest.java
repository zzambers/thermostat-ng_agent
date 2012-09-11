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

package com.redhat.thermostat.common.dao;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.utils.ArrayUtils;
import com.redhat.thermostat.test.MockQuery;

public class HostLatestPojoListGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final String CATEGORY_NAME = "hostcategory";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category cat =  new Category(CATEGORY_NAME);

    private static long t1 = 1;
    private static long t2 = 5;
    private static long t3 = 10;

    private static Double load5_1 = 2.0;
    private static Double load5_2 = 6.0;
    private static Double load5_3 = 11.0;

    private static Double load10_1 = 3.0;
    private static Double load10_2 = 7.0;
    private static Double load10_3 = 12.0;

    private static Double load15_1 = 4.0;
    private static Double load15_2 = 8.0;
    private static Double load15_3 = 13.0;

    private HostRef ref;
    private Converter<CpuStat> converter;
    private Chunk result1, result2, result3;

    @Before
    public void setUp() {
        ref = new HostRef(AGENT_ID, HOSTNAME);
        converter = new CpuStatConverter();
        result1 = new Chunk(cat, false);
        result1.put(Key.AGENT_ID, AGENT_ID);
        result1.put(Key.TIMESTAMP, t1);
        result1.put(CpuStatDAO.cpuLoadKey, Arrays.asList(load5_1, load10_1, load15_1));
        result2 = new Chunk(cat, false);
        result2.put(Key.AGENT_ID, AGENT_ID);
        result2.put(Key.TIMESTAMP, t2);
        result2.put(CpuStatDAO.cpuLoadKey, Arrays.asList(load5_2, load10_2, load15_2));
        result3 = new Chunk(cat, false);
        result3.put(Key.AGENT_ID, AGENT_ID);
        result3.put(Key.TIMESTAMP, t3);
        result3.put(CpuStatDAO.cpuLoadKey, Arrays.asList(load5_3, load10_3, load15_3));
    }

    @Test
    public void testBuildQuery() {
        Storage storage = mock(Storage.class);
        MockQuery query = new MockQuery();
        when (storage.createQuery()).thenReturn(query);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, converter, ref);
        query = (MockQuery) getter.buildQuery();

        assertNotNull(query);
        assertEquals(cat, query.getCategory());
        assertEquals(1, query.getWhereClausesCount());
        assertFalse(query.hasWhereClauseFor(Key.TIMESTAMP));
        assertTrue(query.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID));
    }

    @Test
    public void testBuildQueryWithSince() {
        Storage storage = mock(Storage.class);
        MockQuery query = new MockQuery();
        when (storage.createQuery()).thenReturn(query);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, converter, ref, 123);
        query = (MockQuery) getter.buildQuery();

        assertNotNull(query);
        assertEquals(cat, query.getCategory());
        assertEquals(2, query.getWhereClausesCount());
        assertTrue(query.hasWhereClause(Key.TIMESTAMP, Criteria.GREATER_THAN, 123l));
        assertTrue(query.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID));
    }

    @Test
    public void testBuildQueryPopulatesUpdateTimes() {
        Storage storage = mock(Storage.class);
        MockQuery ignored = new MockQuery();
        MockQuery query = new MockQuery();
        when(storage.createQuery()).thenReturn(ignored).thenReturn(query);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, converter, ref);
        ignored = (MockQuery) getter.buildQuery(); // Ignore first return value.

        query = (MockQuery) getter.buildQuery();

        assertNotNull(query);
        assertEquals(cat, query.getCategory());
        assertEquals(2, query.getWhereClausesCount());
        assertTrue(query.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID));
        assertTrue(query.hasWhereClause(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE));
    }

    @Test
    public void testGetLatest() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Storage storage = mock(Storage.class);
        Query query = new MockQuery();
        when(storage.createQuery()).thenReturn(query);
        when(storage.findAll(query)).thenReturn(cursor);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, converter, ref);

        List<CpuStat> stats = getter.getLatest();

        assertNotNull(stats);
        assertEquals(2, stats.size());
        CpuStat stat1 = stats.get(0);
        assertEquals(t1, stat1.getTimeStamp());
        assertArrayEquals(new double[] {load5_1, load10_1, load15_1}, ArrayUtils.toPrimitiveDoubleArray(stat1.getPerProcessorUsage()), 0.001);
        CpuStat stat2 = stats.get(1);
        assertEquals(t2, stat2.getTimeStamp());
        assertArrayEquals(new double[] {load5_2, load10_2, load15_2}, ArrayUtils.toPrimitiveDoubleArray(stat2.getPerProcessorUsage()), 0.001);
    }

    @Test
    public void testGetLatestMultipleCalls() {
        Cursor cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor1.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Cursor cursor2 = mock(Cursor.class);
        when(cursor2.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor2.next()).thenReturn(result3);

        Storage storage = mock(Storage.class);
        MockQuery firstQuery = new MockQuery();
        MockQuery secondQuery = new MockQuery();
        when(storage.createQuery()).thenReturn(firstQuery).thenReturn(secondQuery);

        when(storage.findAll(isA(Query.class))).thenReturn(cursor1);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, converter, ref);
        getter.getLatest();
        getter.getLatest();

        verify(storage, times(2)).findAll(isA(Query.class));

        assertTrue(secondQuery.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID));
        assertTrue(secondQuery.hasWhereClause(Key.TIMESTAMP, Criteria.GREATER_THAN, t2));
    }

    @After
    public void tearDown() {
        ref = null;
        converter = null;
        result1 = null;
        result2 = null;
        result3 = null;
    }
}
