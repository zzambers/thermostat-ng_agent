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

package com.redhat.thermostat.vm.gc.common;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;

@Service
public interface VmGcStatDAO {

    static final Key<String> collectorKey = new Key<>("collectorName");
    static final Key<Long> runCountKey = new Key<>("runCount");
    /** time in microseconds */
    static final Key<Long> wallTimeKey = new Key<>("wallTime");

    static final Category<VmGcStat> vmGcStatCategory = new Category<>("vm-gc-stats", VmGcStat.class,
            Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP, collectorKey, runCountKey, wallTimeKey),
            Arrays.<Key<?>>asList(Key.TIMESTAMP));

    @Deprecated
    public List<VmGcStat> getLatestVmGcStats(VmRef ref, long since);

    public List<VmGcStat> getLatestVmGcStats(AgentId agentId, VmId vmId, long since);
    
    /**
     * Find the set of distinct collector names for this JVM. For each JVM there
     * are potentially multiple collectors recorded in storage.
     * 
     * @param ref
     *            The idendifier of the JVM for which to get the collector names
     *            for.
     * @return A set of distinct collector names.
     *
     * @deprecated use {@link #getDistinctCollectorNames(VmId)}
     */
    @Deprecated
    public Set<String> getDistinctCollectorNames(VmRef ref);

    /**
     * Find the set of distinct collector names for this JVM. For each JVM there
     * are potentially multiple collectors recorded in storage.
     *
     * @param vmId
     *            The idendifier of the JVM for which to get the collector names
     *            for.
     * @return A set of distinct collector names.
     */
    public Set<String> getDistinctCollectorNames(VmId vmId);
    
    public void putVmGcStat(VmGcStat stat);
}

