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

package com.redhat.thermostat.vm.io.common.internal;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmBoundaryPojoGetter;
import com.redhat.thermostat.storage.core.VmLatestPojoListGetter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.VmTimeIntervalPojoListGetter;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoStatDAOImpl implements VmIoStatDAO {

    private static final Logger logger = LoggingUtils.getLogger(VmIoStatDAOImpl.class);

    static final Key<Long> KEY_CHARACTERS_READ = new Key<>("charactersRead");
    static final Key<Long> KEY_CHARACTERS_WRITTEN = new Key<>("charactersWritten");
    static final Key<Long> KEY_READ_SYSCALLS = new Key<>("readSyscalls");
    static final Key<Long> KEY_WRITE_SYSCALLS = new Key<>("writeSyscalls");

    static final Category<VmIoStat> CATEGORY = new Category<>("vm-io-stats", VmIoStat.class,
            Arrays.<Key<?>>asList(Key.AGENT_ID, Key.VM_ID, Key.TIMESTAMP,
                    KEY_CHARACTERS_READ, KEY_CHARACTERS_WRITTEN, KEY_READ_SYSCALLS, KEY_WRITE_SYSCALLS),
            Arrays.<Key<?>>asList(Key.TIMESTAMP));

    static final String DESC_ADD_VM_IO_STAT = "ADD " + CATEGORY.getName() +
            " SET '" + Key.AGENT_ID.getName() + "' = ?s , " +
                 "'" + Key.VM_ID.getName() + "' = ?s , " +
                 "'" + Key.TIMESTAMP.getName() + "' = ?l , " +
                 "'" + KEY_CHARACTERS_READ.getName() + "' = ?l , " +
                 "'" + KEY_CHARACTERS_WRITTEN.getName() + "' = ?l , " +
                 "'" + KEY_READ_SYSCALLS.getName() + "' = ?l , " +
                 "'" + KEY_WRITE_SYSCALLS.getName() + "' = ?l";

    private final Storage storage;
    private final VmLatestPojoListGetter<VmIoStat> latestGetter;
    private final VmTimeIntervalPojoListGetter<VmIoStat> intervalGetter;
    private final VmBoundaryPojoGetter<VmIoStat> boundaryGetter;

    VmIoStatDAOImpl(Storage storage) {
        this.storage = storage;
        storage.registerCategory(CATEGORY);
        this.latestGetter = new VmLatestPojoListGetter<>(storage, CATEGORY);
        this.intervalGetter = new VmTimeIntervalPojoListGetter<>(storage, CATEGORY);
        this.boundaryGetter = new VmBoundaryPojoGetter<>(storage, CATEGORY);
    }

    @Override
    public List<VmIoStat> getLatestVmIoStats(VmRef ref, long since) {
        return latestGetter.getLatest(ref, since);
    }

    @Override
    public List<VmIoStat> getVmIoStats(VmRef ref, long since, long to) {
        return intervalGetter.getLatest(ref, since, to);
    }

    @Override
    public VmIoStat getNewest(VmRef ref) {
        return boundaryGetter.getNewestStat(ref);
    }

    @Override
    public VmIoStat getOldest(VmRef ref) {
        return boundaryGetter.getOldestStat(ref);
    }

    @Override
    public void putVmIoStat(VmIoStat stat) {
        StatementDescriptor<VmIoStat> desc = new StatementDescriptor<>(CATEGORY, DESC_ADD_VM_IO_STAT);
        PreparedStatement<VmIoStat> prepared;
        try {
            prepared = storage.prepareStatement(desc);
            prepared.setString(0, stat.getAgentId());
            prepared.setString(1, stat.getVmId());
            prepared.setLong(2, stat.getTimeStamp());
            prepared.setLong(3, stat.getCharactersRead());
            prepared.setLong(4, stat.getCharactersWritten());
            prepared.setLong(5, stat.getReadSyscalls());
            prepared.setLong(6, stat.getWriteSyscalls());
            prepared.execute();
        } catch (DescriptorParsingException e) {
            logger.log(Level.SEVERE, "Preparing stmt '" + desc + "' failed!", e);
        } catch (StatementExecutionException e) {
            logger.log(Level.SEVERE, "Executing stmt '" + desc + "' failed!", e);
        }
    }
}

