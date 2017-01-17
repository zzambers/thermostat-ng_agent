/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.vm.numa.common.internal;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaDAOImpl extends AbstractDao implements VmNumaDAO {

    private static final Logger logger = LoggingUtils.getLogger(VmNumaDAOImpl.class);

    // ADD vm-numa-stats SET 'agentId' = ?s , \
    //                      'vmId' = ?s , \
    //                      'timeStamp' = ?l , \
    //                      'vm-nodestats' = ?p[
    static final String DESC_ADD_VM_NUMA_STAT = "ADD " + vmNumaStatCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
            "'" + Key.VM_ID.getName() + "' = ?s , " +
            "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
            "'" + vmNodeStats.getName() + "' = ?p[";

    private final Storage storage;

    private final VmTimeIntervalPojoListGetter<VmNumaStat> intervalGetter;
    private final VmBoundaryPojoGetter<VmNumaStat> boundaryGetter;

    VmNumaDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmNumaStatCategory);

        this.intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, vmNumaStatCategory);
        this.boundaryGetter = new VmBoundaryPojoGetter<>(storage, vmNumaStatCategory);
    }

    @Override
    public void putVmNumaStat(final VmNumaStat stat) {
        executeStatement(new AbstractDaoStatement<VmNumaStat>(storage, vmNumaStatCategory, DESC_ADD_VM_NUMA_STAT) {
            @Override
            public PreparedStatement<VmNumaStat> customize(PreparedStatement<VmNumaStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setString(1, stat.getVmId());
                preparedStatement.setLong(2, stat.getTimeStamp());
                preparedStatement.setPojoList(3, stat.getVmNodeStats());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<VmNumaStat> getNumaStats(AgentId agentId, VmId vmId, long since, long to) {
        return intervalGetter.getLatest(agentId, vmId, since, to);
    }

    @Override
    public VmNumaStat getNewest(AgentId agentId, VmId vmId) {
        return boundaryGetter.getOldestStat(vmId, agentId);
    }

    @Override
    public VmNumaStat getOldest(AgentId agentId, VmId vmId) {
        return boundaryGetter.getOldestStat(vmId, agentId);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
