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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.model.VmCpuStat;

public class VmCpuStatBuilderTest {

    @Test
    public void testBuilderKnowsNothing() {
        Clock clock = mock(Clock.class);
        ProcessStatusInfoBuilder statusBuilder = mock(ProcessStatusInfoBuilder.class);
        int cpuCount = 0;
        long ticksPerSecond = 0;
        VmCpuStatBuilder builder = new VmCpuStatBuilder(clock, cpuCount, ticksPerSecond, statusBuilder);

        assertFalse(builder.knowsAbout(0));
        assertFalse(builder.knowsAbout(1));
        assertFalse(builder.knowsAbout(Integer.MIN_VALUE));
        assertFalse(builder.knowsAbout(Integer.MAX_VALUE));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderThrowsOnBuildOfUnknownPid() {
        Clock clock = mock(Clock.class);
        int cpuCount = 0;
        long ticksPerSecond = 0;
        ProcessStatusInfoBuilder statusBuilder = mock(ProcessStatusInfoBuilder.class);
        VmCpuStatBuilder builder = new VmCpuStatBuilder(clock, cpuCount, ticksPerSecond, statusBuilder);
        builder.build(0);
    }

    @Test
    public void testSaneBuild() {
        final int PID = 0;

        final int CPU_COUNT = 3;

        final long USER_INITIAL_TICKS = 1;
        final long KERNEL_INITIAL_TICKS = 1;

        final long USER_LATER_TICKS = 10;
        final long KERNEL_LATER_TICKS = 10;

        final long CLOCK1 = 10000;
        final long CLOCK2 = 20000;

        final long TICKS_PER_SECOND = 100;

        final double CPU_LOAD_PERCENT =
                100.0
                * ((USER_LATER_TICKS + KERNEL_LATER_TICKS) - (USER_INITIAL_TICKS + KERNEL_INITIAL_TICKS))
                / TICKS_PER_SECOND
                / ((CLOCK2 - CLOCK1) * 1E-3 /* millis to seconds */)
                / CPU_COUNT;

        final ProcessStatusInfo initialInfo = new ProcessStatusInfo(PID, USER_INITIAL_TICKS, KERNEL_INITIAL_TICKS);
        final ProcessStatusInfo laterInfo = new ProcessStatusInfo(PID, USER_LATER_TICKS, KERNEL_LATER_TICKS);

        Clock clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(CLOCK2);
        when(clock.getMonotonicTimeNanos()).thenReturn((long) (CLOCK1 * 1E6)).thenReturn((long) (CLOCK2 * 1E6));

        ProcessStatusInfoBuilder statusBuilder = mock(ProcessStatusInfoBuilder.class);
        when(statusBuilder.build(any(Integer.class))).thenReturn(initialInfo).thenReturn(laterInfo).thenReturn(null);

        VmCpuStatBuilder builder = new VmCpuStatBuilder(clock, CPU_COUNT, TICKS_PER_SECOND, statusBuilder);

        builder.learnAbout(PID);
        VmCpuStat stat = builder.build(PID);

        assertNotNull(stat);
        assertEquals(PID, stat.getVmId());
        assertEquals(CLOCK2, stat.getTimeStamp());
        assertEquals(CPU_LOAD_PERCENT, stat.getCpuLoad(), 0.0001);
    }

}
