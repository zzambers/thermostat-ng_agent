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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.common.storage.Chunk;

public class VmInfoConverterTest {

    private int vmId;
    private long startTime;
    private long stopTime;
    private String jVersion;
    private String jHome;
    private String mainClass;
    private String commandLine;
    private String vmName;
    private String vmInfo;
    private String vmVersion;
    private String vmArgs;
    private Map<String, String> props;
    private Map<String, String> env;
    private List<String> libs;

    @Before
    public void setUp() {
        vmId = 1;
        startTime = 2;
        stopTime = 3;
        jVersion = "java 1.0";
        jHome = "/path/to/jdk/home";
        mainClass = "Hello.class";
        commandLine = "World";
        vmArgs = "-XX=+FastestJITPossible";
        vmName = "Hotspot";
        vmInfo = "Some info";
        vmVersion = "1.0";
        props = new HashMap<>();
        env = new HashMap<>();
        libs = new ArrayList<>();
    }

    @Test
    public void testVmInfoToChunk() {
        VmInfo info = new VmInfo(vmId, startTime, stopTime,
                jVersion, jHome, mainClass, commandLine,
                vmName, vmInfo, vmVersion, vmArgs,
                props, env, libs);

        Chunk chunk = new VmInfoConverter().toChunk(info);

        assertNotNull(chunk);
        assertEquals((Integer) vmId, chunk.get(VmInfoDAO.vmIdKey));
        assertEquals((Integer) vmId, chunk.get(VmInfoDAO.vmIdKey));
        assertEquals((Long) startTime, chunk.get(VmInfoDAO.startTimeKey));
        assertEquals((Long) stopTime, chunk.get(VmInfoDAO.stopTimeKey));
        assertEquals(jVersion, chunk.get(VmInfoDAO.runtimeVersionKey));
        assertEquals(jHome, chunk.get(VmInfoDAO.javaHomeKey));
        assertEquals(mainClass, chunk.get(VmInfoDAO.mainClassKey));
        assertEquals(commandLine, chunk.get(VmInfoDAO.commandLineKey));
        assertEquals(vmName, chunk.get(VmInfoDAO.vmNameKey));
        assertEquals(vmInfo, chunk.get(VmInfoDAO.vmInfoKey));
        assertEquals(vmVersion, chunk.get(VmInfoDAO.vmVersionKey));
        assertEquals(vmArgs, chunk.get(VmInfoDAO.vmArgumentsKey));

        // FIXME test environment, properties and loaded native libraries later
    }

    @Test
    public void testChunkToVmInfo() {
        Chunk chunk = new Chunk(VmInfoDAO.vmInfoCategory, true);

        chunk.put(VmInfoDAO.vmIdKey, vmId);
        chunk.put(VmInfoDAO.vmPidKey, vmId);
        chunk.put(VmInfoDAO.startTimeKey, startTime);
        chunk.put(VmInfoDAO.stopTimeKey, stopTime);
        chunk.put(VmInfoDAO.runtimeVersionKey, jVersion);
        chunk.put(VmInfoDAO.javaHomeKey, jHome);
        chunk.put(VmInfoDAO.mainClassKey, mainClass);
        chunk.put(VmInfoDAO.commandLineKey, commandLine);
        chunk.put(VmInfoDAO.vmNameKey, vmName);
        chunk.put(VmInfoDAO.vmInfoKey, vmInfo);
        chunk.put(VmInfoDAO.vmVersionKey, vmVersion);
        chunk.put(VmInfoDAO.vmArgumentsKey, vmArgs);
        chunk.put(VmInfoDAO.propertiesKey, props);
        chunk.put(VmInfoDAO.environmentKey, env);
        chunk.put(VmInfoDAO.librariesKey, libs);

        VmInfo info = new VmInfoConverter().fromChunk(chunk);

        assertNotNull(info);
        assertEquals((Integer) vmId, (Integer) info.getVmId());
        assertEquals((Integer) vmId, (Integer) info.getVmPid());
        assertEquals((Long) startTime, (Long) info.getStartTimeStamp());
        assertEquals((Long) stopTime, (Long) info.getStopTimeStamp());
        assertEquals(jVersion, info.getJavaVersion());
        assertEquals(jHome, info.getJavaHome());
        assertEquals(mainClass, info.getMainClass());
        assertEquals(commandLine, info.getJavaCommandLine());
        assertEquals(vmName, info.getVmName());
        assertEquals(vmInfo, info.getVmInfo());
        assertEquals(vmVersion, info.getVmVersion());
        assertEquals(vmArgs, info.getVmArguments());

        // FIXME test environment, properties and loaded native libraries later
    }
}
