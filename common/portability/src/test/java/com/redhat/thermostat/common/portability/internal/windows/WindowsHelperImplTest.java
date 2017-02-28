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

package com.redhat.thermostat.common.portability.internal.windows;

import com.redhat.thermostat.shared.config.NativeLibraryResolver;
import com.redhat.thermostat.shared.config.OS;

import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * These tests are disabled until we get the DLL path issues sorted out
 * TODO - These tests currently fail on Windows because the helper DLL isn't on the execution path
 */

@Ignore
public class WindowsHelperImplTest {

    @Test
    public void loadNativeLib() {
        Assume.assumeTrue(OS.IS_WINDOWS);
        final WindowsHelperImpl impl = WindowsHelperImpl.INSTANCE;
        assertNotNull(impl);
    }

    @Test
    public void testGetHostInfo() {
        Assume.assumeTrue(OS.IS_WINDOWS);
        final WindowsHelperImpl impl = WindowsHelperImpl.INSTANCE;
        assertNotNull(impl);
        assertContainsData(impl.getHostName());
        assertContainsData(impl.getOSName());
        assertTrue(impl.getOSName().toLowerCase().contains("win"));
        assertContainsData(impl.getOSVersion());
        assertContainsData(impl.getCPUModel());
        assertTrue(impl.getCPUCount() > 0);
        assertTrue(impl.getTotalMemory() > 0);
    }

    @Test
    @Ignore
    public void testGetProcessInfo() {
        Assume.assumeTrue(OS.IS_WINDOWS);
        final WindowsHelperImpl impl = WindowsHelperImpl.INSTANCE;
        assertNotNull(impl);
        int pid = /*TODO: retrieve current process identifier*/0;
        assertContainsData(impl.getUserName(pid));
        assertTrue(impl.getUid(pid) >= 0);
        Map<String,String> envMap = impl.getEnvironment(pid);
        assertNotNull(envMap);
        assertFalse(envMap.isEmpty());
    }

    private static void assertContainsData( final String s ) {
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private static void dumpProcess(int pid) {

        final WindowsHelperImpl helper = WindowsHelperImpl.INSTANCE;

        System.out.println("pid " + pid + " user=" + helper.getUserName(pid));

        final String cwd = helper.getCWD(pid);
        final String exec = helper.getExecutable(pid);
        final String cmd = helper.getCommandLine(pid);

        if (cwd != null) {
            System.out.println("pid " + pid + " cwd = " + cwd);
        } else {
            System.out.println("pid " + pid + " cwd is not available");
        }

        if (exec != null) {
            System.out.println("pid " + pid + " exec = " + exec);
        } else {
            System.out.println("pid " + pid + " exec is not available");
        }

        if (cmd != null) {
            System.out.println("pid " + pid + " cmd = " + cmd);
        } else {
            System.out.println("pid " + pid + " cmd is not available");
        }

        Map<String, String> env = helper.getEnvironment(pid);
        if (env != null && !env.isEmpty()) {
            for (final Map.Entry<String,String> var : env.entrySet()) {
                System.out.println(var.getKey() + " = " + var.getValue());
            }
        }
        else {
            System.out.println("env for pid " + pid + " is not available");
        }
    }
    // manual sanity test
    public static void main( String[] args ) {
        if (System.getenv("THERMOSTAT_HOME") == null) {
            // fix up for CommonPaths
            System.setProperty("THERMOSTAT_HOME", "/");
            System.setProperty("USER_THERMOSTAT_HOME", "/");
        }
        NativeLibraryResolver.setCommonPaths(new CommonPathsImpl());
        final WindowsHelperImpl helper = WindowsHelperImpl.INSTANCE;

        System.out.println("Hostname = " + helper.getHostName());
        System.out.println("OS version = " + helper.getOSVersion());
        System.out.println("CPU = " + helper.getCPUModel());
        System.out.println("OS name = " + helper.getOSName());

        System.out.println("Current process = " + helper.getCurrentProcessPid());

        final int pid = args.length > 0 ? Integer.parseInt(args[0]) : 0;

        dumpProcess(pid);

        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");

        dumpProcess(0);

        System.out.println("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");

        Map<String, String> myenv = System.getenv();
        for (String envName : myenv.keySet()) {
            System.out.format("%s = %s%n",
                    envName,
                    myenv.get(envName));
        }

    }

}
