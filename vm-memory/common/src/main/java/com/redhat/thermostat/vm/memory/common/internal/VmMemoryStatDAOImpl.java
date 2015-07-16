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

package com.redhat.thermostat.vm.memory.common.internal;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.AbstractDaoStatement;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;

class VmMemoryStatDAOImpl extends AbstractDao implements VmMemoryStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(VmMemoryStatDAOImpl.class);

    // ADD vm-memory-stats SET 'agentId' = ?s , \
    //                         'vmId' = ?s , \
    //                         'timeStamp' = ?s , \
    //                         'generations' = ?p[
    static final String DESC_ADD_VM_MEMORY_STAT = "ADD " + vmMemoryStatsCategory.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + generationsKey.getName() + "' = ?p[";
    
    private final Storage storage;
    private final VmLatestPojoListGetter<VmMemoryStat> latestGetter;
    private final VmTimeIntervalPojoListGetter<VmMemoryStat> intervalGetter;
    private final VmBoundaryPojoGetter<VmMemoryStat> boundaryGetter;

    VmMemoryStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(vmMemoryStatsCategory);
        latestGetter = new VmLatestPojoListGetter<>(storage, vmMemoryStatsCategory);
        intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, vmMemoryStatsCategory);
        boundaryGetter = new VmBoundaryPojoGetter<>(storage, vmMemoryStatsCategory);
    }

    @Override
    public VmMemoryStat getNewestMemoryStat(VmRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    public VmMemoryStat getOldestMemoryStat(VmRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public void putVmMemoryStat(final VmMemoryStat stat) {
        executeStatement(new AbstractDaoStatement<VmMemoryStat>(storage, vmMemoryStatsCategory, DESC_ADD_VM_MEMORY_STAT) {
            @Override
            public PreparedStatement<VmMemoryStat> customize(PreparedStatement<VmMemoryStat> preparedStatement) {
                preparedStatement.setString(0, stat.getAgentId());
                preparedStatement.setString(1, stat.getVmId());
                preparedStatement.setLong(2, stat.getTimeStamp());
                preparedStatement.setPojoList(3, stat.getGenerations());
                return preparedStatement;
            }
        });
    }

    @Override
    public List<VmMemoryStat> getLatestVmMemoryStats(VmRef ref, long since) {
        return latestGetter.getLatest(ref, since);
    }

    @Override
    public List<VmMemoryStat> getLatestVmMemoryStats(AgentId agentId, VmId vmId, long since) {
        return latestGetter.getLatest(agentId, vmId, since);
    }

    @Override
    public List<VmMemoryStat> getVmMemoryStats(VmRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

