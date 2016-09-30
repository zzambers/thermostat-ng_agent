/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.service.internal.windows;

import com.redhat.thermostat.service.internal.ProcessUtilitiesBase;
import com.redhat.thermostat.service.process.UNIXSignal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowsProcessUtilities extends ProcessUtilitiesBase  {

    @Override
    protected List<String> buildCommandLine(Integer pid) {
        final List<String> commandLine = new ArrayList<>();
        commandLine.add("tasklist");
        commandLine.add("/FO");
        commandLine.add("csv");
        commandLine.add("/FI");
        commandLine.add("\"PID eq " + String.valueOf(pid) + "\"");
        return commandLine;
    }

    @Override
    protected String processStdout(final BufferedReader out) throws IOException {
        out.readLine(); // skip header line
        final String outStr = out.readLine();
        if (outStr == null)
            return null;
        final String [] output = outStr.split(",");
        String result = output[0];
        if (result.length() >= 2 && result.charAt(0) == '"')
            result = result.replace("\"","");
        result = result.replace(".exe","");
        return result;
    }

    @Override
    public void sendSignal(Integer pid, UNIXSignal signal) {
        // TODO - port to Windows
        exec("kill -s " + signal.signalName() + " " + pid);
    }

}

