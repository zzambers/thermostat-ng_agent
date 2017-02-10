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

package com.redhat.thermostat.shared.config;

import java.io.File;

import com.redhat.thermostat.shared.config.internal.CommonPathsImpl;


/**
 * Class which enables resolving of native libraries placed in
 * {@link CommonPaths#getSystemNativeLibsRoot()}.
 *
 */
public class NativeLibraryResolver {

    private static CommonPaths paths;

    // Set in Activator.start() to prevent IllegalStateException below.
    public static void setCommonPaths(CommonPaths paths) {
        NativeLibraryResolver.paths = paths;
    }

    /**
     * Gets the absolute path of a native library. The native library must be
     * placed in directory as returned by {@link CommonPathsImpl#getSystemNativeLibsRoot()}.
     * 
     * @param libraryName
     *            The name of the library. Specified in the same fashion as for
     *            {@link System#loadLibrary(String)}.
     * @return The absolute path to the native library, suitable to be loaded
     *         via {@link System#load(String)};
     */
    public static String getAbsoluteLibraryPath(String libraryName) {
        String nativeLibsRoot = null;
        // Allow a property for tests
        nativeLibsRoot = System.getProperty("com.redhat.thermostat.shared.loader.testNativesHome");
        if (nativeLibsRoot != null) {
            return doResolve(nativeLibsRoot, libraryName);
        }
        if (paths == null) {
            throw new IllegalStateException("NativeLibraryResolver does not yet know about CommonPaths.");
        }
        try {
            nativeLibsRoot = paths.getSystemNativeLibsRoot().getPath();
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return doResolve(nativeLibsRoot, libraryName);
    }
    
    private static String doResolve(String root, String libraryName) {
        return new File(root, System.mapLibraryName(libraryName)).getAbsolutePath();
    }
}

