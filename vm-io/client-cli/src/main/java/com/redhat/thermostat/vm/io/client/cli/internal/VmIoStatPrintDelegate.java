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

package com.redhat.thermostat.vm.io.client.cli.internal;

import java.util.Arrays;
import java.util.List;

import com.redhat.thermostat.client.cli.VMStatPrintDelegate;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import com.redhat.thermostat.vm.io.common.Constants;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoStatPrintDelegate implements VMStatPrintDelegate {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String CHARACTERS_READ =
            translator.localize(LocaleResources.COLUMN_HEADER_CHARACTERS_READ).getContents();
    private static final String CHARACTERS_WRITTEN =
            translator.localize(LocaleResources.COLUMN_HEADER_CHARACTERS_WRITTEN).getContents();

    private VmIoStatDAO ioStatDAO;

    public VmIoStatPrintDelegate(VmIoStatDAO ioStatDAO) {
        this.ioStatDAO = ioStatDAO;
    }

    @Deprecated
    @Override
    public List<? extends TimeStampedPojo> getLatestStats(VmRef ref, long timestamp) {
        return ioStatDAO.getLatestVmIoStats(ref, timestamp);
    }

    @Override
    public List<? extends TimeStampedPojo> getLatestStats(AgentId agentId, VmId vmId, long timestamp) {
        return ioStatDAO.getLatestVmIoStats(agentId, vmId, timestamp);
    }

    @Override
    public List<String> getHeaders(TimeStampedPojo stat) {
        return Arrays.asList(CHARACTERS_READ, CHARACTERS_WRITTEN);
    }

    @Override
    public List<String> getStatRow(TimeStampedPojo stat) {
        VmIoStat ioStat = (VmIoStat) stat;
        String charactersRead = String.valueOf(ioStat.getCharactersRead());
        String charactersWritten = String.valueOf(ioStat.getCharactersWritten());
        return Arrays.asList(charactersRead, charactersWritten);
    }

    @Override
    public int getOrderValue() {
        return Constants.ORDER_VALUE;
    }

}
