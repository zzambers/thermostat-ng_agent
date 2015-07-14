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

package com.redhat.thermostat.vm.memory.client.cli.internal;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.client.cli.VMStatPrintDelegate;
import com.redhat.thermostat.common.Size;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.model.VmMemoryStat;

public class VmMemoryStatPrintDelegate implements VMStatPrintDelegate {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private VmMemoryStatDAO memoryStatDAO;
    
    public VmMemoryStatPrintDelegate(VmMemoryStatDAO memoryStatDAO) {
        this.memoryStatDAO = memoryStatDAO;
    }

    @Override
    @Deprecated
    public List<? extends TimeStampedPojo> getLatestStats(VmRef ref, long timestamp) {
        return memoryStatDAO.getLatestVmMemoryStats(ref, timestamp);
    }

    @Override
    public List<? extends TimeStampedPojo> getLatestStats(AgentId agentId, VmId vmId, long timestamp) {
        return memoryStatDAO.getLatestVmMemoryStats(agentId, vmId, timestamp);
    }

    @Override
    public List<String> getHeaders(TimeStampedPojo stat) {
        return getSpacesNames(stat);
    }
    
    @Override
    public List<String> getStatRow(TimeStampedPojo stat) {
        return getMemoryUsage(stat);
    }

    private List<String> getSpacesNames(TimeStampedPojo stat) {
        List<String> spacesNames = new ArrayList<>();
        VmMemoryStat memStat = (VmMemoryStat) stat;
        for (VmMemoryStat.Generation gen : memStat.getGenerations()) {
            for (VmMemoryStat.Space space : gen.getSpaces()) {
                spacesNames.add(translator.localize(LocaleResources.COLUMN_HEADER_MEMORY_PATTERN, space.getName()).getContents());
            }
        }
        return spacesNames;
    }

    private List<String> getMemoryUsage(TimeStampedPojo stat) {
        List<String> memoryUsage = new ArrayList<>();
        for (VmMemoryStat.Generation gen : ((VmMemoryStat) stat).getGenerations()) {
            for (VmMemoryStat.Space space : gen.getSpaces()) {
                memoryUsage.add(Size.bytes(space.getUsed()).toString());
            }
        }
        return memoryUsage;
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP;
    }

}

