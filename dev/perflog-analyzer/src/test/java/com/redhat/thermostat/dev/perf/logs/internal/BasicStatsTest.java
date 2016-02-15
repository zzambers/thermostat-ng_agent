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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.dev.perf.logs.StatsConfig;

public class BasicStatsTest {
    
    @Test
    public void canGetTotalNumberEmpty() {
        TestBasicStats basic = new TestBasicStats();
        doBasicTests(basic);
    }

    @Test
    public void canSetValues() {
        TestBasicStats basic = new TestBasicStats();
        doBasicTests(basic);
        basic.setConfig(mock(StatsConfig.class));
        basic.setSharedStatementState(mock(SharedStatementState.class));
        List<TestLineStat> testList = new ArrayList<>();
        testList.add(new TestLineStat());
        basic.setStats(testList);
        assertEquals(1, basic.getTotalNumberOfRecords());
        assertNotNull(basic.getAllStats());
        assertNotNull(basic.config);
        assertNotNull(basic.sharedState);
        assertNotNull(basic.stats);
    }
    
    private void doBasicTests(TestBasicStats basic) {
        assertNull(basic.getAllStats());
        assertEquals(0, basic.getTotalNumberOfRecords());
        assertNull(basic.sharedState);
        assertNull(basic.config);
    }

    private static class TestBasicStats extends BasicStats<TestLineStat> {

        @Override
        public void printSummary(PrintStream out) {
            // no-op
        }
        
    }
    
    private static class TestLineStat implements LineStat {

        @Override
        public LogTag getLogTag() {
            // not implemented
            return null;
        }

        @Override
        public Date getTimeStamp() {
            // not implemented
            return null;
        }
        
    }
}
