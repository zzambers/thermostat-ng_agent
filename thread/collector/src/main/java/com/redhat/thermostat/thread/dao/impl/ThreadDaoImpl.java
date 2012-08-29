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

package com.redhat.thermostat.thread.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.thread.collector.ThreadInfo;
import com.redhat.thermostat.thread.collector.ThreadSummary;
import com.redhat.thermostat.thread.collector.VMThreadCapabilities;
import com.redhat.thermostat.thread.collector.impl.ThreadMXInfo;
import com.redhat.thermostat.thread.collector.impl.ThreadMXSummary;
import com.redhat.thermostat.thread.collector.impl.VMThreadMXCapabilities;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ThreadDaoImpl implements ThreadDao {

    private Storage storage; 
    public ThreadDaoImpl(Storage storage) {
        this.storage = storage;
        storage.createConnectionKey(THREAD_CAPABILITIES);
        storage.createConnectionKey(THREAD_SUMMARY);
        storage.createConnectionKey(THREAD_INFO);
    }

    @Override
    public VMThreadCapabilities loadCapabilities(VmRef vm) {
        
        VMThreadMXCapabilities caps = null;
        
        Chunk query = new Chunk(THREAD_CAPABILITIES, false);
        query.put(Key.VM_ID, vm.getId());
        query.put(Key.AGENT_ID, vm.getAgent().getAgentId());
        
        Chunk found = storage.find(query);
        if (found != null) {
            caps = new VMThreadMXCapabilities();
            if (found.get(CONTENTION_MONITOR_KEY)) caps.addFeature(CONTENTION_MONITOR);
            if (found.get(CPU_TIME_KEY)) caps.addFeature(CPU_TIME);
            if (found.get(THREAD_ALLOCATED_MEMORY_KEY)) caps.addFeature(THREAD_ALLOCATED_MEMORY);
        }
        
        return caps;
    }
    
    @Override
    public void saveCapabilities(String vmId, String agentId, VMThreadCapabilities caps) {
        Chunk chunk = prepareChunk(THREAD_CAPABILITIES, true, vmId, agentId);
        
        chunk.put(CONTENTION_MONITOR_KEY, caps.supportContentionMonitor());
        chunk.put(CPU_TIME_KEY, caps.supportCPUTime());
        chunk.put(THREAD_ALLOCATED_MEMORY_KEY, caps.supportThreadAllocatedMemory());
        
        storage.putChunk(chunk);
    }
    
    @Override
    public void saveSummary(String vmId, String agentId, ThreadSummary summary) {
        Chunk chunk = prepareChunk(THREAD_SUMMARY, false, vmId, agentId);
        
        chunk.put(LIVE_THREADS_KEY, summary.currentLiveThreads());
        chunk.put(DAEMON_THREADS_KEY, summary.currentDaemonThreads());
        chunk.put(Key.TIMESTAMP, summary.getTimeStamp());
        
        storage.putChunk(chunk);
    }
    
    @Override
    public ThreadSummary loadLastestSummary(VmRef ref) {
        ThreadMXSummary summary = null;

        Chunk query = prepareChunk(THREAD_SUMMARY, false, ref);
        Cursor cursor = storage.findAll(query).sort(Key.TIMESTAMP, Cursor.SortDirection.DESCENDING).limit(1);
        if (cursor.hasNext()) {
            Chunk found = cursor.next();
            summary = new ThreadMXSummary();
            summary.setTimestamp(found.get(Key.TIMESTAMP));
            summary.setCurrentLiveThreads(found.get(LIVE_THREADS_KEY));
            summary.setDaemonThreads(found.get(DAEMON_THREADS_KEY));
        }
        
        return summary;
    }
    
    @Override
    public List<ThreadSummary> loadSummary(VmRef ref, long since) {
        
        List<ThreadSummary> result = new ArrayList<>();
        
        Chunk query = prepareChunk(THREAD_SUMMARY, false, ref);
        query.put(Key.WHERE, "this.timestamp > " + since);

        Cursor cursor = storage.findAll(query).sort(Key.TIMESTAMP, Cursor.SortDirection.DESCENDING);
        while (cursor.hasNext()) {
            ThreadMXSummary summary = new ThreadMXSummary();
            
            Chunk found = cursor.next();
            summary.setTimestamp(found.get(Key.TIMESTAMP));
            summary.setCurrentLiveThreads(found.get(LIVE_THREADS_KEY));
            summary.setDaemonThreads(found.get(DAEMON_THREADS_KEY));
            result.add(summary);
        }
        
        return result;
    }
    
    @Override
    public void saveThreadInfo(String vmId, String agentId, ThreadInfo info) {
        Chunk chunk = prepareChunk(THREAD_INFO, false, vmId, agentId);
        
        chunk.put(Key.TIMESTAMP, info.getTimeStamp());

        chunk.put(THREAD_ID_KEY, info.getThreadID());
        chunk.put(THREAD_NAME_KEY, info.getName());
        chunk.put(THREAD_STATE_KEY, info.getState().name());
        
        chunk.put(THREAD_BLOCKED_COUNT_KEY, info.getBlockedCount());
        chunk.put(THREAD_WAIT_COUNT_KEY, info.getWaitedCount());
        chunk.put(THREAD_CPU_TIME_KEY, info.getCpuTime());
        chunk.put(THREAD_USER_TIME_KEY, info.getUserTime());

        storage.putChunk(chunk);
    }

    @Override
    public List<ThreadInfo> loadThreadInfo(VmRef ref, long since) {
        List<ThreadInfo> result = new ArrayList<>();
        
        Chunk query = prepareChunk(THREAD_INFO, false, ref);
        query.put(Key.WHERE, "this.timestamp > " + since);
        
        Cursor cursor = storage.findAll(query).sort(Key.TIMESTAMP, Cursor.SortDirection.DESCENDING);
        while (cursor.hasNext()) {
            ThreadMXInfo info = new ThreadMXInfo();
            
            Chunk found = cursor.next();
            info.setTimeStamp(found.get(Key.TIMESTAMP));
            
            info.setID(found.get(THREAD_ID_KEY));
            info.setName(found.get(THREAD_NAME_KEY));
            info.setState(Thread.State.valueOf(found.get(THREAD_STATE_KEY)));

            info.setBlockedCount(found.get(THREAD_BLOCKED_COUNT_KEY));
            info.setWaitedCount(found.get(THREAD_WAIT_COUNT_KEY));
            info.setCPUTime(found.get(THREAD_CPU_TIME_KEY));
            info.setUserTime(found.get(THREAD_USER_TIME_KEY));

            result.add(info);
        }
        
        return result;
    }
    
    private Chunk prepareChunk(Category category, boolean replace, String vmId, String agentId) {
        Chunk chunk = new Chunk(category, replace);
        chunk.put(Key.AGENT_ID, agentId);
        chunk.put(Key.VM_ID, Integer.valueOf(vmId));
        return chunk;
    }
    
    private Chunk prepareChunk(Category category, boolean replace, VmRef vm) {
        return prepareChunk(category, replace, vm.getIdString(), vm.getAgent().getAgentId());
    }
    
    @Override
    public Storage getStorage() {
        return storage;
    }
}
