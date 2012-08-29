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

package com.redhat.thermostat.thread.harvester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;

import static org.junit.Assert.*;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.harvester.management.MXBeanConnection;
import com.redhat.thermostat.thread.harvester.management.MXBeanConnector;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public class HarvesterTest {

    @Test
    public void testStart() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        final MXBeanConnector mockedConnector = mock(MXBeanConnector.class);
        when(mockedConnector.isAttached()).thenReturn(false);
        
        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> arg1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> arg3 = ArgumentCaptor.forClass(TimeUnit.class);
        
        final boolean [] harvestDataCalled = new boolean[1];
        
        when(executor.scheduleAtFixedRate(arg0.capture(), arg1.capture(), arg2.capture(), arg3.capture())).thenReturn(null);
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe") {
            { connector = mockedConnector; }
            @Override
            synchronized void harvestData() {
                harvestDataCalled[0] = true;
            }
        };
        
        harvester.start();
        
        verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        
        assertTrue(arg1.getValue() == 0);
        assertTrue(arg2.getValue() == 1);
        assertEquals(TimeUnit.SECONDS, arg3.getValue());
        
        Runnable action = arg0.getValue();
        assertNotNull(action);
        
        action.run();
        
        assertTrue(harvestDataCalled[0]);
        
        assertTrue(harvester.isConnected());
    }
    
    /**
     *  Mostly the same as testStart, but we call harvester.start() twice
     */
    @Test
    public void testStartOnce() throws Exception {

        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        final MXBeanConnector mockedConnector = mock(MXBeanConnector.class);
        when(mockedConnector.isAttached()).thenReturn(false);
        
        ArgumentCaptor<Runnable> arg0 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> arg1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> arg2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> arg3 = ArgumentCaptor.forClass(TimeUnit.class);
        
        final boolean [] harvestDataCalled = new boolean[1];
        
        when(executor.scheduleAtFixedRate(arg0.capture(), arg1.capture(), arg2.capture(), arg3.capture())).thenReturn(null);
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe") {
            { connector = mockedConnector; }
            @Override
            synchronized void harvestData() {
                harvestDataCalled[0] = true;
            }
        };
        
        harvester.start();
        harvester.start();

        verify(mockedConnector, times(1)).isAttached();
        verify(mockedConnector, times(1)).attach();
        verify(executor, times(1)).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        
        assertTrue(arg1.getValue() == 0);
        assertTrue(arg2.getValue() == 1);
        assertEquals(TimeUnit.SECONDS, arg3.getValue());
        
        Runnable action = arg0.getValue();
        assertNotNull(action);
        
        action.run();
        
        assertTrue(harvestDataCalled[0]);
        
        assertTrue(harvester.isConnected());
    }
    
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testStopAfterStarting() throws Exception {
        
        ScheduledFuture future = mock(ScheduledFuture.class);
        
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        final MXBeanConnector mockedConnector = mock(MXBeanConnector.class);
        
        MXBeanConnection connection = mock(MXBeanConnection.class);
        
        when(mockedConnector.connect()).thenReturn(connection);
        
        when(mockedConnector.isAttached()).thenReturn(true);
        
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe")
        {{ connector = mockedConnector; }};
        
        harvester.start();
        
        assertTrue(harvester.isConnected());
        
        harvester.stop();
        
        verify(future).cancel(false);
        verify(connection).close();
        
        // needs to be 2 times, since is called once in start
        verify(mockedConnector, times(2)).isAttached();
        verify(mockedConnector).close();
        
        assertFalse(harvester.isConnected());
    }
    
    /**
     *  Mostly the same as testStopAfterStarting, but we call harvester.stop()
     *  twice
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testStopTwiceAfterStarting() throws Exception {
        
        ScheduledFuture future = mock(ScheduledFuture.class);
        
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        final MXBeanConnector mockedConnector = mock(MXBeanConnector.class);
        
        MXBeanConnection connection = mock(MXBeanConnection.class);
        
        when(mockedConnector.connect()).thenReturn(connection);
        
        when(mockedConnector.isAttached()).thenReturn(true);
        
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe")
        {{ connector = mockedConnector; }};
        
        harvester.start();
        
        assertTrue(harvester.isConnected());
        
        harvester.stop();
        harvester.stop();

        verify(future, times(1)).cancel(false);
        verify(connection, times(1)).close();
        
        // needs to be 2 times, since is called once in start
        verify(mockedConnector, times(2)).isAttached();
        
        verify(mockedConnector, times(1)).close();
        
        assertFalse(harvester.isConnected());
    }
    
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testStopNotStarted() throws Exception {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ThreadDao dao = mock(ThreadDao.class);
        
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(future);
        
        final MXBeanConnector mockedConnector = mock(MXBeanConnector.class);
        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(mockedConnector.connect()).thenReturn(connection);
        when(mockedConnector.isAttached()).thenReturn(true);
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe")
        {{ connector = mockedConnector; }};
        
        verify(executor, times(0)).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        
        assertFalse(harvester.isConnected());
        
        harvester.stop();

        assertFalse(harvester.isConnected());
        verify(mockedConnector, times(0)).isAttached();
        
        verify(future, times(0)).cancel(false);
    }
    
    @Test
    public void testHarvestData() {
        
        long ids[] = new long [] {
            0, 1
        };
        
        ThreadInfo info1 = mock(ThreadInfo.class);
        when(info1.getThreadName()).thenReturn("fluff1");
        when(info1.getThreadId()).thenReturn(1l);

        ThreadInfo info2 = mock(ThreadInfo.class);
        when(info2.getThreadName()).thenReturn("fluff2");
        when(info2.getThreadId()).thenReturn(2l);

        ThreadInfo[] infos = new ThreadInfo[] {
            info1,
            info2     
        };
                
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ArgumentCaptor<String> vmCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> agentCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ThreadSummary> summaryCapture = ArgumentCaptor.forClass(ThreadSummary.class);

        ThreadDao dao = mock(ThreadDao.class);
        doNothing().when(dao).saveSummary(vmCapture.capture(), agentCapture.capture(), summaryCapture.capture());
        
        ArgumentCaptor<String> vmCapture2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> agentCapture2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ThreadInfoData> threadInfoCapture = ArgumentCaptor.forClass(ThreadInfoData.class);        
        doNothing().when(dao).saveThreadInfo(vmCapture2.capture(), agentCapture2.capture(), threadInfoCapture.capture());

        final ThreadMXBean collectorBean = mock(ThreadMXBean.class);

        when(collectorBean.getThreadCount()).thenReturn(42);
        when(collectorBean.getAllThreadIds()).thenReturn(ids);
        when(collectorBean.getThreadInfo(ids, true, true)).thenReturn(infos);

        final boolean [] getDataCollectorBeanCalled = new boolean[1];
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe") {
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection)
                    throws MalformedObjectNameException {
                getDataCollectorBeanCalled[0] = true;
                return collectorBean;
            }
        };

        harvester.harvestData();
        
        assertTrue(getDataCollectorBeanCalled[0]);
        
        verify(collectorBean).getThreadInfo(ids, true, true);
        
        verify(dao).saveSummary(anyString(), anyString(), any(ThreadSummary.class));
        
        // once for each thread info
        verify(dao, times(2)).saveThreadInfo(anyString(), anyString(), any(ThreadInfoData.class));
        
        assertEquals(42, summaryCapture.getValue().currentLiveThreads());
        assertEquals("42", vmCapture.getValue());
        assertEquals("0xcafe", agentCapture.getValue());
        
        assertEquals(42, summaryCapture.getValue().currentLiveThreads());
        assertEquals("42", vmCapture2.getAllValues().get(0));
        assertEquals("42", vmCapture2.getAllValues().get(1));

        assertEquals("0xcafe", agentCapture2.getAllValues().get(0));
        assertEquals("0xcafe", agentCapture2.getAllValues().get(1));
        
        List<ThreadInfoData> threadInfos = threadInfoCapture.getAllValues();
        assertEquals(2, threadInfos.size());
        
        assertEquals("fluff1", threadInfos.get(0).getName());
        assertEquals("fluff2", threadInfos.get(1).getName());
        
        verify(collectorBean, times(1)).getThreadCpuTime(1l);
        verify(collectorBean, times(1)).getThreadCpuTime(2l);
    }
    
    @Test
    public void testSaveVmCaps() {

        final MXBeanConnector mockedConnector = mock(MXBeanConnector.class);
        when(mockedConnector.isAttached()).thenReturn(true);
        
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        ArgumentCaptor<String> vmCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> agentCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<VMThreadCapabilities> capsCapture = ArgumentCaptor.forClass(VMThreadCapabilities.class);

        ThreadDao dao = mock(ThreadDao.class);
        doNothing().when(dao).saveCapabilities(vmCapture.capture(), agentCapture.capture(), capsCapture.capture());
      
        final ThreadMXBean collectorBean = mock(ThreadMXBean.class);
        when(collectorBean.isThreadCpuTimeSupported()).thenReturn(true);
        when(collectorBean.isThreadContentionMonitoringSupported()).thenReturn(true);

        final boolean [] getDataCollectorBeanCalled = new boolean[1];
        
        Harvester harvester = new Harvester(dao, executor, "42", "0xcafe") {
            { connector = mockedConnector; }
            @Override
            ThreadMXBean getDataCollectorBean(MXBeanConnection connection)
                    throws MalformedObjectNameException {
                getDataCollectorBeanCalled[0] = true;
                return collectorBean;
            }
        };

        harvester.saveVmCaps();
        assertTrue(getDataCollectorBeanCalled[0]);
        
        verify(dao, times(1)).saveCapabilities(anyString(), anyString(), any(VMThreadCapabilities.class));
        assertEquals("42", vmCapture.getValue());
        assertEquals("0xcafe", agentCapture.getValue());

        List<String> features = capsCapture.getValue().getSupportedFeaturesList();
        assertEquals(2, features.size());
        assertTrue(features.contains(ThreadDao.CPU_TIME));
        assertTrue(features.contains(ThreadDao.CONTENTION_MONITOR));
        assertFalse(features.contains(ThreadDao.THREAD_ALLOCATED_MEMORY));
    }    
}
