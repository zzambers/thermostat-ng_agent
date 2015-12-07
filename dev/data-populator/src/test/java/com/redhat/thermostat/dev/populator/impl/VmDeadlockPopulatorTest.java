/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.dev.populator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.dependencies.ProcessedRecords;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.dev.populator.impl.VmDeadlockPopulator.ThreadDaoCountable;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class VmDeadlockPopulatorTest {
    
    private static final String EXPECTED_DESCRIPTION = "" +
            "\"Mallory\" Id=12 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7 owned by \"Alice\" Id=10\n" +
            "\tat sun.misc.Unsafe.park(Native Method)\n" +
            "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
            "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
            "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
            "\t...\n\n" +
            "\tNumber of locked synchronizers = 1\n" +
            "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2" +
            "\n\n\n" +
            "\"Alice\" Id=10 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e owned by \"Bob\" Id=11\n" +
            "\tat sun.misc.Unsafe.park(Native Method)\n" +
            "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n" + 
            "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" + 
            "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
            "\t...\n\n" + 
            "\tNumber of locked synchronizers = 1\n" +
            "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
            "\n\n" +
            "\"Bob\" Id=11 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2 owned by \"Mallory\" Id=12\n" +
            "\tat sun.misc.Unsafe.park(Native Method)\n" +
            "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2\n" +
            "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" + 
            "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" + 
            "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
            "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
            "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" + 
            "\t...\n\n" +
            "\tNumber of locked synchronizers = 1\n" +
            "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n\n\n";
    
    @Test
    public void testFormatDeadlockDesc() {
        Object[] stringFormatArgs = new Object[] {
                "Mallory", 12,
                "Alice", 10,
                "Alice", 10,
                "Bob", 11,
                "Bob", 11,
                "Mallory", 12
        };
        assertEquals(EXPECTED_DESCRIPTION, String.format(VmDeadlockPopulator.DEADLOCK_DESC_FORMAT, stringFormatArgs));
    }
    
    @Test
    public void canGetRandomThreadNames() {
        Object[] args = new VmDeadlockPopulator().getFormatStringArgs(2);
        assertEquals(12, args.length);
        Map<Integer, Integer> tids = new HashMap<>();
        Map<String, Integer> threadNames = new HashMap<>();
        for (int i = 0; i < args.length; i +=2) {
            Object arg1 = args[i];
            Object arg2 = args[i + 1];
            assertEquals(String.class, arg1.getClass());
            assertEquals(Integer.class, arg2.getClass());
            if (tids.containsKey(arg2)) {
                Integer value = tids.get(arg2);
                value += 1;
                tids.put((Integer)arg2, value);
            } else {
                tids.put((Integer)arg2, 1);
            }
            if (threadNames.containsKey(arg1)) {
                Integer value = threadNames.get(arg1);
                value += 1;
                threadNames.put((String)arg1, value);
            } else {
                threadNames.put((String)arg1, 1);
            }
        }
        for (String key : threadNames.keySet()) {
            int value = threadNames.get(key);
            assertEquals(2, value);
        }
        for (Integer key : tids.keySet()) {
            int value = tids.get(key);
            assertEquals(2, value);
        }
    }

    @Test
    public void canHandleAppropriateCollection() {
        VmDeadlockPopulator populator = new VmDeadlockPopulator();
        assertEquals("vm-deadlock-data", populator.getHandledCollection());
    }
    
    @Test
    public void canAddDeadlockInfo() {
        int perVmCount = 123;
        ConfigItem config = new ConfigItem(perVmCount, ConfigItem.UNSET, "vm-deadlock-data");
        String[] agents = new String[] {
                "fooAgent1", "fooBarAgent", "testAgent", "someAgent"
        };
        String[] vms = new String[] {
                "vm1", "vm2", "vm3", "vm4", "vm5"
        };
        int totalCount = perVmCount * agents.length * vms.length;
        ThreadDaoCountable countableDao = mock(ThreadDaoCountable.class);
        when(countableDao.getCount()).thenReturn(0L).thenReturn((long)totalCount);
        VmDeadlockPopulator populator = new VmDeadlockPopulator(countableDao);
        SharedState state = mock(SharedState.class);
        when(state.getProcessedRecordsFor("agentId")).thenReturn(new ProcessedRecords<>(Arrays.asList(agents)));
        when(state.getProcessedRecordsFor("vmId")).thenReturn(new ProcessedRecords<>(Arrays.asList(vms)));
        SharedState retval = populator.addPojos(mock(Storage.class), config, state);
        assertSame(retval, state);
        ArgumentCaptor<VmDeadLockData> deadlockInfoCaptor = ArgumentCaptor.forClass(VmDeadLockData.class);
        verify(countableDao, times(totalCount)).saveDeadLockStatus(deadlockInfoCaptor.capture());
        List<VmDeadLockData> savedValues = deadlockInfoCaptor.getAllValues();
        assertEquals(totalCount, savedValues.size());
        List<VmDeadLockData> perVmIdAgentDeadlockData = getFilteredByVmId(savedValues, "vm3");
        assertEquals(perVmCount * agents.length, perVmIdAgentDeadlockData.size());
        List<VmDeadLockData> perAgentIdDeadlockData = getFilteredByAgentId(savedValues, "fooAgent1");
        verifyVmIdsDifferent(perAgentIdDeadlockData);
    }

    private List<VmDeadLockData> getFilteredByAgentId(List<VmDeadLockData> perVmDeadlockData, String agentId) {
        List<VmDeadLockData> filteredValues = new ArrayList<>();
        for (VmDeadLockData data: perVmDeadlockData) {
            if (data.getAgentId().equals(agentId)) {
                filteredValues.add(data);
            }
        }
        return filteredValues;
    }

    private void verifyVmIdsDifferent(List<VmDeadLockData> perVmDeadlockData) {
        Set<String> vmIds = new HashSet<>();
        for (VmDeadLockData data: perVmDeadlockData) {
            if (vmIds.contains(data.getVmId())) {
                throw new AssertionError("Duplicate VM ID per agent. Duplicate was: " + data.getVmId());
            }
            vmIds.add(data.getAgentId());
        }
    }

    private List<VmDeadLockData> getFilteredByVmId(List<VmDeadLockData> savedValues, String vmId) {
        List<VmDeadLockData> filteredValues = new ArrayList<>();
        for (VmDeadLockData data: savedValues) {
            if (data.getVmId().equals(vmId)) {
                filteredValues.add(data);
            }
        }
        return filteredValues;
    }
}
