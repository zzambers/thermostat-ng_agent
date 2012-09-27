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

package com.redhat.thermostat.agent.heapdumper.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.heap.HistogramLoader;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.redhat.thermostat.common.model.HeapInfo;

public class HeapDumpReceiver implements RequestReceiver {

    private static final Logger log = Logger.getLogger(HeapDumpReceiver.class.getName());

    private final HeapDAO heapDao;

    private final JMXHeapDumper jmxHeapDumper;
    private final JMapHeapDumper jmapHeapDumper;

    private final HistogramLoader histogramLoader;

    public HeapDumpReceiver(HeapDAO heapDao) {
        this(heapDao, new JMXHeapDumper(), new JMapHeapDumper(), new HistogramLoader());
    }

    HeapDumpReceiver(HeapDAO heapDao, JMXHeapDumper jmxHeapDumper, JMapHeapDumper jmapHeapDumper, HistogramLoader histogramLoader) {
        this.heapDao = heapDao;
        this.jmxHeapDumper = jmxHeapDumper;
        this.jmapHeapDumper = jmapHeapDumper;
        this.histogramLoader = histogramLoader;
    }

    @Override
    public Response receive(Request request) {
        String vmId = request.getParameter("vmId");
        try {
            File heapDumpFile = dumpHeap(vmId);
            ObjectHistogram histogram = loadHistogram(heapDumpFile.getAbsolutePath());
            saveHeapDumpInfo(vmId, heapDumpFile, histogram);
            
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unexpected IO problem while writing heap dump", e);
            return new Response(ResponseType.EXCEPTION);
        } catch (HeapDumpException e) {
            log.log(Level.SEVERE, "Unexpected problem while writing heap dump", e);
            return new Response(ResponseType.EXCEPTION);
        }
        return new Response(ResponseType.OK);
    }

    private File dumpHeap(String vmId) throws IOException, HeapDumpException {
        File tempFile = Files.createTempFile("thermostat-", "-heapdump").toFile();
        String tempFileName = tempFile.getAbsolutePath();
        tempFile.delete(); // Need to delete before dumping heap, jmap does not override existing file and stop with an error.
        dumpHeapUsingJMX(vmId, tempFileName);
        return tempFile;
    }

    private void dumpHeapUsingJMX(String vmId, String filename) throws HeapDumpException {

        try {
            jmxHeapDumper.dumpHeap(vmId, filename);
        } catch (HeapDumpException e) {
            log.log(Level.WARNING, "Heap dump using JMX failed, trying jmap", e);
            dumpHeapUsingJMap(vmId, filename);
        }
    }

    private void dumpHeapUsingJMap(String vmId, String filename) throws HeapDumpException {
        jmapHeapDumper.dumpHeap(vmId, filename);
    }

    private ObjectHistogram loadHistogram(String heapDumpFilename) throws IOException {
        return histogramLoader.load(heapDumpFilename);
    }

    private void saveHeapDumpInfo(String vmId, File tempFile, ObjectHistogram histogram) throws IOException {
        HeapInfo heapInfo = new HeapInfo(Integer.parseInt(vmId), System.currentTimeMillis());
        heapDao.putHeapInfo(heapInfo, tempFile, histogram);
    }

}
