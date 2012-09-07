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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.model.HostInfo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.test.MockQuery;

public class HostInfoDAOTest {

    static class Pair<T,U> {
        final T first;
        final U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
    }

    private static final String HOST_NAME = "a host name";
    private static final String OS_NAME = "some os";
    private static final String OS_KERNEL = "some kernel";
    private static final String CPU_MODEL = "some cpu that runs fast";
    private static final int CPU_NUM = -1;
    private static final long MEMORY_TOTAL = 0xCAFEBABEl;

    @Test
    public void testCategory() {
        assertEquals("host-info", HostInfoDAO.hostInfoCategory.getName());
        Collection<Key<?>> keys = HostInfoDAO.hostInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agent-id", true)));
        assertTrue(keys.contains(new Key<String>("hostname", true)));
        assertTrue(keys.contains(new Key<String>("os_name", false)));
        assertTrue(keys.contains(new Key<String>("os_kernel", false)));
        assertTrue(keys.contains(new Key<String>("cpu_model", false)));
        assertTrue(keys.contains(new Key<Integer>("cpu_num", false)));
        assertTrue(keys.contains(new Key<Long>("memory_total", false)));
        assertEquals(7, keys.size());
    }

    @Test
    public void testGetHostInfo() {

        Chunk chunk = new Chunk(HostInfoDAO.hostInfoCategory, false);
        chunk.put(HostInfoDAO.hostNameKey, HOST_NAME);
        chunk.put(HostInfoDAO.osNameKey, OS_NAME);
        chunk.put(HostInfoDAO.osKernelKey, OS_KERNEL);
        chunk.put(HostInfoDAO.cpuModelKey, CPU_MODEL);
        chunk.put(HostInfoDAO.cpuCountKey, CPU_NUM);
        chunk.put(HostInfoDAO.hostMemoryTotalKey, MEMORY_TOTAL);

        Storage storage = mock(Storage.class);
        when(storage.createQuery()).thenReturn(new MockQuery());
        when(storage.find(any(Query.class))).thenReturn(chunk);

        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);

        HostInfo info = new HostInfoDAOImpl(storage, agentInfoDao)
            .getHostInfo(new HostRef("some uid", HOST_NAME));

        assertNotNull(info);
        assertEquals(HOST_NAME, info.getHostname());
        assertEquals(OS_NAME, info.getOsName());
        assertEquals(OS_KERNEL, info.getOsKernel());
        assertEquals(CPU_MODEL, info.getCpuModel());
        assertEquals(CPU_NUM, info.getCpuCount());
        assertEquals(MEMORY_TOTAL, info.getTotalMemory());
    }

    @Test
    public void testGetHostsSingleHost() {

        Storage storage = setupStorageForSingleHost();
        AgentInfoDAO agentInfo = mock(AgentInfoDAO.class);

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfo);
        Collection<HostRef> hosts = hostsDAO.getHosts();

        assertEquals(1, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
    }

    private Storage setupStorageForSingleHost() {

        Chunk hostConfig = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig.put(HostInfoDAO.hostNameKey, "fluffhost1");
        hostConfig.put(Key.AGENT_ID, "123");

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(hostConfig);

        Storage storage = mock(Storage.class);
        when(storage.createQuery()).then(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocation) throws Throwable {
                return new MockQuery();
            }
        });
        when(storage.findAllFromCategory(HostInfoDAO.hostInfoCategory)).thenReturn(cursor);
        when(storage.findAll(any(Query.class))).thenReturn(cursor);
        
        return storage;
    }

    @Test
    public void testGetHosts3Hosts() {

        Storage storage = setupStorageFor3Hosts();
        AgentInfoDAO agentInfo = mock(AgentInfoDAO.class);

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfo);
        Collection<HostRef> hosts = hostsDAO.getHosts();

        assertEquals(3, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        assertTrue(hosts.contains(new HostRef("456", "fluffhost2")));
        assertTrue(hosts.contains(new HostRef("789", "fluffhost3")));
    }

    private Storage setupStorageFor3Hosts() {

        Chunk hostConfig1 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig1.put(HostInfoDAO.hostNameKey, "fluffhost1");
        hostConfig1.put(Key.AGENT_ID, "123");
        Chunk hostConfig2 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig2.put(HostInfoDAO.hostNameKey, "fluffhost2");
        hostConfig2.put(Key.AGENT_ID, "456");
        Chunk hostConfig3 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig3.put(HostInfoDAO.hostNameKey, "fluffhost3");
        hostConfig3.put(Key.AGENT_ID, "789");

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(hostConfig1).thenReturn(hostConfig2).thenReturn(hostConfig3);

        Storage storage = mock(Storage.class);
        when(storage.createQuery()).then(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocation) throws Throwable {
                return new MockQuery();
            }
        });
        when(storage.findAllFromCategory(HostInfoDAO.hostInfoCategory)).thenReturn(cursor);
        when(storage.findAll(any(Query.class))).thenReturn(cursor);
        
        return storage;
    }

    @Test
    public void testPutHostInfo() {
        Storage storage = mock(Storage.class);
        AgentInfoDAO agentInfo = mock(AgentInfoDAO.class);

        HostInfo info = new HostInfo(HOST_NAME, OS_NAME, OS_KERNEL, CPU_MODEL, CPU_NUM, MEMORY_TOTAL);
        HostInfoDAO dao = new HostInfoDAOImpl(storage, agentInfo);
        dao.putHostInfo(info);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).putChunk(arg.capture());
        Chunk chunk = arg.getValue();

        assertEquals(HostInfoDAO.hostInfoCategory, chunk.getCategory());
        assertEquals(HOST_NAME, chunk.get(HostInfoDAO.hostNameKey));
        assertEquals(OS_NAME, chunk.get(HostInfoDAO.osNameKey));
        assertEquals(OS_KERNEL, chunk.get(HostInfoDAO.osKernelKey));
        assertEquals(CPU_MODEL, chunk.get(HostInfoDAO.cpuModelKey));
        assertEquals((Integer) CPU_NUM, chunk.get(HostInfoDAO.cpuCountKey));
        assertEquals((Long) MEMORY_TOTAL, chunk.get(HostInfoDAO.hostMemoryTotalKey));
    }

    @Test
    public void testGetCount() {
        Storage storage = mock(Storage.class);
        when(storage.getCount(any(Category.class))).thenReturn(5L);
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);

        HostInfoDAO dao = new HostInfoDAOImpl(storage, agentInfoDao);
        Long count = dao.getCount();
        assertEquals((Long) 5L, count);
    }
    
    @Test
    public void getAliveHostSingle() {
        Pair<Storage, AgentInfoDAO> setup = setupForSingleAliveHost();
        Storage storage = setup.first;
        AgentInfoDAO agentInfoDao = setup.second;

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfoDao);
        Collection<HostRef> hosts = hostsDAO.getAliveHosts();

        assertEquals(1, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        verify(storage, times(1)).findAll(any(Query.class));
    }
    
    private Pair<Storage, AgentInfoDAO> setupForSingleAliveHost() {
        
        // agents
        
        Chunk agentConfig1 = new Chunk(AgentInfoDAO.CATEGORY, false);
        agentConfig1.put(Key.AGENT_ID, "123");
        agentConfig1.put(AgentInfoDAO.ALIVE_KEY, true);
        
        AgentInformation agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("123");
        agentInfo1.setAlive(true);
        
        // hosts
        
        Chunk hostConfig1 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig1.put(HostInfoDAO.hostNameKey, "fluffhost1");
        hostConfig1.put(Key.AGENT_ID, "123");
        
        Chunk hostConfig2 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig2.put(HostInfoDAO.hostNameKey, "fluffhost2");
        hostConfig2.put(Key.AGENT_ID, "456");
        
        // cursor

        Cursor cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor1.next()).thenReturn(hostConfig1);

        // storage
        
        Storage storage = mock(Storage.class);
        when(storage.createQuery()).then(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocation) throws Throwable {
                return new MockQuery();
            }
        });
        when(storage.findAll(any(Query.class))).thenReturn(cursor1);

        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        when(agentDao.getAliveAgents()).thenReturn(Arrays.asList(agentInfo1));

        return new Pair<>(storage, agentDao);
    }
    
    @Test
    public void getAliveHost3() {
        Pair<Storage, AgentInfoDAO> setup = setupForAliveHost3();
        Storage storage = setup.first;
        AgentInfoDAO agentInfoDao = setup.second;

        HostInfoDAO hostsDAO = new HostInfoDAOImpl(storage, agentInfoDao);
        Collection<HostRef> hosts = hostsDAO.getAliveHosts();

        // cursor 3 from the above storage should not be used
        assertEquals(3, hosts.size());
        assertTrue(hosts.contains(new HostRef("123", "fluffhost1")));
        assertTrue(hosts.contains(new HostRef("456", "fluffhost2")));
        assertTrue(hosts.contains(new HostRef("678", "fluffhost3")));
        verify(storage, times(3)).findAll(any(Query.class));
    }
    
    private Pair<Storage, AgentInfoDAO> setupForAliveHost3() {
        
        // agents
        
        Chunk agentConfig1 = new Chunk(AgentInfoDAO.CATEGORY, false);
        agentConfig1.put(Key.AGENT_ID, "123");
        agentConfig1.put(AgentInfoDAO.ALIVE_KEY, true);

        AgentInformation agentInfo1 = new AgentInformation();
        agentInfo1.setAgentId("123");
        agentInfo1.setAlive(true);

        Chunk agentConfig2 = new Chunk(AgentInfoDAO.CATEGORY, false);
        agentConfig2.put(Key.AGENT_ID, "456");
        agentConfig2.put(AgentInfoDAO.ALIVE_KEY, true);

        AgentInformation agentInfo2 = new AgentInformation();
        agentInfo2.setAgentId("456");
        agentInfo2.setAlive(true);

        Chunk agentConfig3 = new Chunk(AgentInfoDAO.CATEGORY, false);
        agentConfig3.put(Key.AGENT_ID, "678");
        agentConfig3.put(AgentInfoDAO.ALIVE_KEY, true);

        AgentInformation agentInfo3 = new AgentInformation();
        agentInfo3.setAgentId("678");
        agentInfo3.setAlive(true);
        
        // hosts
        
        Chunk hostConfig1 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig1.put(HostInfoDAO.hostNameKey, "fluffhost1");
        hostConfig1.put(Key.AGENT_ID, "123");
        
        Chunk hostConfig2 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig2.put(HostInfoDAO.hostNameKey, "fluffhost2");
        hostConfig2.put(Key.AGENT_ID, "456");
        
        Chunk hostConfig3 = new Chunk(HostInfoDAO.hostInfoCategory, false);
        hostConfig3.put(HostInfoDAO.hostNameKey, "fluffhost3");
        hostConfig3.put(Key.AGENT_ID, "678");
        
        Cursor cursor1 = mock(Cursor.class);
        when(cursor1.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor1.next()).thenReturn(hostConfig1);

        Cursor cursor2 = mock(Cursor.class);
        when(cursor2.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor2.next()).thenReturn(hostConfig2);

        Cursor cursor3 = mock(Cursor.class);
        when(cursor3.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor3.next()).thenReturn(hostConfig3);
        
        // storage
        
        Storage storage = mock(Storage.class);
        when(storage.createQuery()).then(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocation) throws Throwable {
                return new MockQuery();
            }
        });
        when(storage.findAll(any(Query.class))).thenReturn(cursor1).
                                                thenReturn(cursor2).
                                                thenReturn(cursor3);
        
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        when(agentDao.getAliveAgents()).thenReturn(Arrays.asList(agentInfo1, agentInfo2, agentInfo3));

        return new Pair<>(storage, agentDao);
    }
}
