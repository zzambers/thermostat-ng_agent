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

package com.redhat.thermostat.storage.dao;

import java.util.Collection;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Countable;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.HostInfo;

@Service
public interface HostInfoDAO extends Countable {

    static Key<String> hostNameKey = new Key<>("hostname");
    static Key<String> osNameKey = new Key<>("osName");
    static Key<String> osKernelKey = new Key<>("osKernel");
    static Key<Integer> cpuCountKey = new Key<>("cpuCount");
    static Key<String> cpuModelKey = new Key<>("cpuModel");
    static Key<Long> hostMemoryTotalKey = new Key<>("totalMemory");

    static final Category<HostInfo> hostInfoCategory = new Category<>("host-info", HostInfo.class,
            Key.AGENT_ID, hostNameKey, osNameKey, osKernelKey,
            cpuCountKey, cpuModelKey, hostMemoryTotalKey);

    /**
     * 
     * @param ref The host ref for which to get the HostInfo object for.
     * @return The corresponding HostInfo object. May return null if the user
     *         is not permitted to retrieve this HostInfo.
     */
    @Deprecated
    HostInfo getHostInfo(HostRef ref);

    /**
     *
     * @param agentId The Agent Id for which to get the HostInfo object for.
     * @return The corresponding HostInfo object. May return null if the user
     *         is not permitted to retrieve this HostInfo.
     */
    HostInfo getHostInfo(AgentId agentId);

    void putHostInfo(HostInfo info);

    /**
     * 
     * @return A collection of hosts (HostRefs), which may be empty.
     *
     * @deprecated use {@link com.redhat.thermostat.storage.dao.AgentInfoDAO#getAgentIds()}
     * instead.
     */
    @Deprecated
    Collection<HostRef> getHosts();
    
    /**
     * 
     * @return A collection of alive hosts which may be empty.
     *
     * @deprecated use {@link com.redhat.thermostat.storage.dao.AgentInfoDAO#getAliveAgentIds()}
     * instead.
     */
    @Deprecated
    Collection<HostRef> getAliveHosts();
    
    /**
     * 
     * @return if this host is alive.
     * @deprecated use {@link com.redhat.thermostat.storage.dao.AgentInfoDAO#isAlive(AgentId)}
     * instead.
     */
    @Deprecated
    boolean isAlive(HostRef ref);

}

