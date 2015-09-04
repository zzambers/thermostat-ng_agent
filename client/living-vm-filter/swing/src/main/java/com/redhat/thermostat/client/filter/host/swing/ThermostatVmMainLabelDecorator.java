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

package com.redhat.thermostat.client.filter.host.swing;

import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class ThermostatVmMainLabelDecorator implements ReferenceFieldLabelDecorator {

    // duplicated in the startup scripts
    private static final String MAIN_CLASS =
            "com.redhat.thermostat.main.Thermostat";

    private VmInfoDAO dao;

    public ThermostatVmMainLabelDecorator(VmInfoDAO dao) {
        this.dao = dao;
    }

    @Override
    public String getLabel(String originalLabel, Ref reference) {

        if (!(reference instanceof VmRef) || 
                !reference.getName().equals(MAIN_CLASS)) {
            return originalLabel;
        }
        VmInfo vmInfo = dao.getVmInfo((VmRef) reference);
        if (!vmInfo.getMainClass().equals(MAIN_CLASS)) {
            return originalLabel;
        }
        return createLabel(vmInfo);
    }

    @Override
    public int getOrderValue() {
        return ORDER_FIRST + 1;
    }

    /**
     * This method returns a short version for the VmInfo object command line.
     * @param info the vm info object from which take information.
     * @return the resulting string 
     */
    private String createLabel(VmInfo info) {
        String[]  s = info.getJavaCommandLine().split("\\."); //escaped dot
        String afterDot = s[s.length - 1];

        // heuristic: take the first two non-option arguments

        // FIXME this doesn't work if --boot-delegation appears before the
        // subcommand. --boot-delegation requires a separate value.
        String[] parts = afterDot.split("\\s+");
        int argumentCount = 0;
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.startsWith("--")) {
                continue;
            }
            result.append(part).append(" ");
            argumentCount++;
            if (argumentCount == 2) {
                break;
            }
        }
        return result.toString().trim();
    }
}

