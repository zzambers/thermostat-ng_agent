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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.model.HostInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.Query.Criteria;

class HostInfoDAOImpl implements HostInfoDAO {
    private Storage storage;
    private AgentInfoDAO agentInfoDao;


    public HostInfoDAOImpl(Storage storage, AgentInfoDAO agentInfo) {
        this.storage = storage;
        this.agentInfoDao = agentInfo;
    }

    @Override
    public HostInfo getHostInfo(HostRef ref) {
        Query query = storage.createQuery()
                .from(hostInfoCategory)
                .where(Key.AGENT_ID, Criteria.EQUALS, ref.getAgentId());
        HostInfo result = storage.findPojo(query, HostInfo.class);
        return result;
    }

    @Override
    public void putHostInfo(HostInfo info) {
        storage.putPojo(hostInfoCategory, false, info);
    }

    @Override
    public Collection<HostRef> getHosts() {
        Query allHosts = storage.createQuery().from(hostInfoCategory);
        return getHosts(allHosts);
    }

    @Override
    public Collection<HostRef> getAliveHosts() {
        List<HostRef> hosts = new ArrayList<>();
        List<AgentInformation> agentInfos = agentInfoDao.getAliveAgents();
        for (AgentInformation agentInfo : agentInfos) {
            Query filter = storage.createQuery()
                    .from(hostInfoCategory)
                    .where(Key.AGENT_ID, Criteria.EQUALS, agentInfo.getAgentId());

            hosts.addAll(getHosts(filter));
        }

        return hosts;
    }


    private Collection<HostRef> getHosts(Query filter) {
        Collection<HostRef> hosts = new ArrayList<HostRef>();
        
        Cursor hostsCursor = storage.findAll(filter);
        while(hostsCursor.hasNext()) {
            Chunk hostChunk = hostsCursor.next();
            String agentId = hostChunk.get(Key.AGENT_ID);
            String hostName = hostChunk.get(hostNameKey);
            hosts.add(new HostRef(agentId, hostName));
        }
        return hosts;
    }

    @Override
    public long getCount() {
        return storage.getCount(hostInfoCategory);
    }
}
