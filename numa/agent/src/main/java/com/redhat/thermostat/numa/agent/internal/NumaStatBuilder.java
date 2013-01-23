/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.numa.agent.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import com.redhat.thermostat.numa.common.NumaNodeStat;

class NumaStatBuilder {

    private Reader in;

    NumaStatBuilder(Reader in) {
        this.in = in;
    }

    NumaNodeStat build() throws IOException {
        NumaNodeStat numaStat = new NumaNodeStat();
        readNumaData(numaStat);
        return numaStat;
    }

    private void readNumaData(NumaNodeStat numaStat) throws IOException {
        BufferedReader bufIn = new BufferedReader(in);
        String line = bufIn.readLine();
        while (line != null) {
            String[] keyValue = line.split(" ");
            String key = keyValue[0];
            long value = Long.parseLong(keyValue[1]);
            if (key.equals("numa_hit")) {
                numaStat.setNumaHit(value);
            } else if (key.equals("numa_miss")) {
                numaStat.setNumaMiss(value);
            } else if (key.equals("numa_foreign")) {
                numaStat.setNumaForeign(value);
            } else if (key.equals("interleave_hit")) {
                numaStat.setInterleaveHit(value);
            } else if (key.equals("local_node")) {
                numaStat.setLocalNode(value);
            } else if (key.equals("other_node")) {
                numaStat.setOtherNode(value);
            }
            line = bufIn.readLine();
        }
    }
}

