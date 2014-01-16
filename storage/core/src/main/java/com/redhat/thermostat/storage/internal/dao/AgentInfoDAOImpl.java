/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.storage.internal.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;

public class AgentInfoDAOImpl extends BaseCountable implements AgentInfoDAO {

    private static final Logger logger = LoggingUtils.getLogger(AgentInfoDAOImpl.class);
    static final String QUERY_AGENT_INFO = "QUERY "
            + CATEGORY.getName() + " WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s";
    static final String QUERY_ALL_AGENTS = "QUERY "
            + CATEGORY.getName();
    // We can use AgentInfoDAO.CATEGORY.getName() here since this query
    // only changes the data class. When executed we use the adapted
    // aggregate category.
    static final String AGGREGATE_COUNT_ALL_AGENTS = "QUERY-COUNT "
            + CATEGORY.getName();
    static final String QUERY_ALIVE_AGENTS = "QUERY "
            + CATEGORY.getName() + " WHERE '" 
            + ALIVE_KEY.getName() + "' = ?b";
    
    // ADD agent-config SET
    //                     'agentId' = ?s , \
    //                     'startTime' = ?l , \
    //                     'stopTime' = ?l , \
    //                     'alive' = ?b , \
    //                     'configListenAddress' = ?s
    static final String DESC_ADD_AGENT_INFO = "ADD " + CATEGORY.getName() + " SET " +
            "'" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'" + START_TIME_KEY.getName() + "' = ?l , " +
            "'" + STOP_TIME_KEY.getName() + "' = ?l , " +
            "'" + ALIVE_KEY.getName() + "' = ?b , " +
            "'" + CONFIG_LISTEN_ADDRESS.getName() + "' = ?s";
    // REMOVE agent-config WHERE 'agentId' = ?s
    static final String DESC_REMOVE_AGENT_INFO = "REMOVE " + CATEGORY.getName() +
            " WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
    // UPDATE agent-config SET
    //                       'startTime' = ?l , \
    //                       'stopTime' = ?l , \
    //                       'alive' = ?b , \
    //                       'configListenAddress' = ?s
    //                     WHERE 'agentId' = ?s
    static final String DESC_UPDATE_AGENT_INFO = "UPDATE " + CATEGORY.getName() + " SET " +
            "'" + START_TIME_KEY.getName() + "' = ?l , " +
            "'" + STOP_TIME_KEY.getName() + "' = ?l , " +
            "'" + ALIVE_KEY.getName() + "' = ?b , " +
            "'" + CONFIG_LISTEN_ADDRESS.getName() + "' = ?s " +
            "WHERE '" + Key.AGENT_ID.getName() + "' = ?s";
                             
    
    private final Storage storage;
    private final Category<AggregateCount> aggregateCategory;

    public AgentInfoDAOImpl(Storage storage) {
        this.storage = storage;
        // prepare adapted category and register it.
        CategoryAdapter<AgentInformation, AggregateCount> adapter = new CategoryAdapter<>(CATEGORY);
        this.aggregateCategory = adapter.getAdapted(AggregateCount.class);
        storage.registerCategory(CATEGORY);
        storage.registerCategory(aggregateCategory);
    }

    @Override
    public long getCount() {
        StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(
                aggregateCategory, AGGREGATE_COUNT_ALL_AGENTS);
        long count = getCount(desc, storage);
        return count;
    }

    @Override
    public List<AgentInformation> getAllAgentInformation() {
        Cursor<AgentInformation> agentCursor;
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, QUERY_ALL_AGENTS);
        PreparedStatement<AgentInformation> prepared = null;
        try {
            prepared = storage.prepareStatement(desc);
            agentCursor =  prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            AgentInformation agentInfo = agentCursor.next();
            results.add(agentInfo);
        }
        return results;
    }

    @Override
    public List<AgentInformation> getAliveAgents() {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, QUERY_ALIVE_AGENTS);
        PreparedStatement<AgentInformation> prepared = null;
        Cursor<AgentInformation> agentCursor = null;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setBoolean(0, true);
            agentCursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return Collections.emptyList();
        }
        List<AgentInformation> results = new ArrayList<>();

        while (agentCursor.hasNext()) {
            AgentInformation agentInfo = agentCursor.next();
            results.add(agentInfo);
        }
        return results;
    }

    @Override
    public AgentInformation getAgentInformation(HostRef agentRef) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, QUERY_AGENT_INFO);
        PreparedStatement<AgentInformation> prepared;
        Cursor<AgentInformation> agentCursor;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, agentRef.getAgentId());
            agentCursor = prepared.executeQuery();
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
            return null;
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + desc + "' failed!", e);
            return null;
        }
        
        AgentInformation result = null;
        if (agentCursor.hasNext()) {
            result = agentCursor.next();
        }
        return result;
    }

    @Override
    public void addAgentInformation(AgentInformation agentInfo) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, DESC_ADD_AGENT_INFO);
        PreparedStatement<AgentInformation> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, agentInfo.getAgentId());
            prepared.setLong(1, agentInfo.getStartTime());
            prepared.setLong(2, agentInfo.getStopTime());
            prepared.setBoolean(3, agentInfo.isAlive());
            prepared.setString(4, agentInfo.getConfigListenAddress());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public void removeAgentInformation(AgentInformation agentInfo) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, DESC_REMOVE_AGENT_INFO);
        PreparedStatement<AgentInformation> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, agentInfo.getAgentId());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

    @Override
    public void updateAgentInformation(AgentInformation agentInfo) {
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(CATEGORY, DESC_UPDATE_AGENT_INFO);
        PreparedStatement<AgentInformation> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setLong(0, agentInfo.getStartTime());
            prepared.setLong(1, agentInfo.getStopTime());
            prepared.setBoolean(2, agentInfo.isAlive());
            prepared.setString(3, agentInfo.getConfigListenAddress());
            prepared.setString(4, agentInfo.getAgentId());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }

}

