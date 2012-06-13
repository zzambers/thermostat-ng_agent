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

package com.redhat.thermostat.client.heap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.osgi.service.Filter;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;

/**
 * Implements the {@link VMContextAction} entry point to provide a kill switch
 * for the currently selected Virtual Machine. 
 */
public class HeapDumpAction implements VMContextAction {

    private static final Logger log = Logger.getLogger(HeapDumpAction.class.getName());

    private final DAOFactory dao;

    public HeapDumpAction(DAOFactory dao) {
        this.dao = dao;
    }

    @Override
    public String getName() {
        return "Heap Dump";
    }

    @Override
    public String getDescription() {
        return "Generate a heap dump";
    }

    @Override
    public void execute(VmRef reference) {
        try {
            File tempFile = Files.createTempFile("thermostat-", "-heapdump").toFile();
            String tempFileName = tempFile.getAbsolutePath();
            tempFile.delete(); // Need to delete before dumping heap, jmap does not override existing file and stop with an error.
            Process proc = Runtime.getRuntime().exec(new String[] {"jmap", "-dump:format=b,file=" + tempFileName, reference.getIdString()});
            try {
                proc.waitFor();
                log.info("Heap dump written to: " + tempFileName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unexpected IO problem while writing heap dump", e);
        }
    }

    @Override
    public Filter getFilter() {
        return new LocalAndAliveFilter();
    }

    private class LocalAndAliveFilter implements Filter {

        @Override
        public boolean matches(Ref ref) {
            // TODO implement local checking too
            if (ref instanceof VmRef) {
                VmRef vm = (VmRef) ref;
                VmInfo vmInfo = dao.getVmInfoDAO().getVmInfo(vm);
                return vmInfo.isAlive();
            } else {
                return false;
            }
        }

    }
}
