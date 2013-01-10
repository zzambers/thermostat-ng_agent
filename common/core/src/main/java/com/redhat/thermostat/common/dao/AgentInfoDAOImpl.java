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
import java.util.List;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.model.AgentInformation;

public class AgentInfoDAOImpl implements AgentInfoDAO {

    private final Storage storage;

    public AgentInfoDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(CATEGORY);
    }

    @Override
    public long getCount() {
        return storage.getCount(CATEGORY);
    }

    @Override
    public List<AgentInformation> getAllAgentInformation() {
        Query query = storage.createQuery().from(CATEGORY);
        Cursor<AgentInformation> agentCursor = storage.findAllPojos(query, AgentInformation.class);

        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            AgentInformation agentInfo = agentCursor.next();
            results.add(agentInfo);
        }
        return results;
    }

    @Override
    public List<AgentInformation> getAliveAgents() {
        Query query = storage.createQuery()
                .from(CATEGORY)
                .where(AgentInfoDAO.ALIVE_KEY, Criteria.EQUALS, true);

        Cursor<AgentInformation> agentCursor = storage.findAllPojos(query, AgentInformation.class);

        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            AgentInformation agentInfo = agentCursor.next();
            results.add(agentInfo);
        }
        return results;
    }

    @Override
    public AgentInformation getAgentInformation(HostRef agentRef) {
        Query query = storage.createQuery()
                .from(CATEGORY)
                .where(Key.AGENT_ID, Criteria.EQUALS, agentRef.getAgentId());

        return storage.findPojo(query, AgentInformation.class);
    }

    @Override
    public void addAgentInformation(AgentInformation agentInfo) {
        Put replace = storage.createReplace(CATEGORY);
        replace.setPojo(agentInfo);
        replace.apply();
    }

    @Override
    public void removeAgentInformation(AgentInformation agentInfo) {
        Remove remove = storage.createRemove().from(CATEGORY).where(Key.AGENT_ID, agentInfo.getAgentId());
        storage.removePojo(remove);
    }

    @Override
    public void updateAgentInformation(AgentInformation agentInfo) {
        Update update = storage.createUpdate().from(CATEGORY).where(Key.AGENT_ID, agentInfo.getAgentId())
                                .set(START_TIME_KEY, agentInfo.getStartTime())
                                .set(STOP_TIME_KEY, agentInfo.getStopTime())
                                .set(ALIVE_KEY, agentInfo.isAlive())
                                .set(CONFIG_LISTEN_ADDRESS, agentInfo.getConfigListenAddress());
        storage.updatePojo(update);
    }

}
