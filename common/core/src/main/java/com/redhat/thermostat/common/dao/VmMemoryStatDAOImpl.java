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

import java.util.List;

import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;

class VmMemoryStatDAOImpl implements VmMemoryStatDAO {

    private final Storage storage;
    private final VmLatestPojoListGetter<VmMemoryStat> getter;

    VmMemoryStatDAOImpl(Storage storage) {
        this.storage = storage;
        getter = new VmLatestPojoListGetter<>(storage, vmMemoryStatsCategory, VmMemoryStat.class);
    }

    @Override
    public VmMemoryStat getLatestMemoryStat(VmRef ref) {
        Query query = storage.createQuery()
                .from(vmMemoryStatsCategory)
                .where(Key.AGENT_ID, Criteria.EQUALS, ref.getAgent().getAgentId())
                .where(Key.VM_ID, Criteria.EQUALS, ref.getId())
                .sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING)
                .limit(1);
        Cursor<VmMemoryStat> cursor = storage.findAllPojos(query, VmMemoryStat.class);
        if (cursor.hasNext()) {
            return cursor.next();
        }
        return null;
    }

    @Override
    public void putVmMemoryStat(VmMemoryStat stat) {
        storage.putPojo(vmMemoryStatsCategory, false, stat);
    }

    @Override
    public List<VmMemoryStat> getLatestVmMemoryStats(VmRef ref, long since) {
        return getter.getLatest(ref, since);
    }

}
