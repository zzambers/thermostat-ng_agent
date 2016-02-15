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

package com.redhat.thermostat.vm.classstat.agent.internal;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;

/**
 * A helper class to provide type-safe access to commonly used jvmstat monitors
 * <p>
 * Implementation details: For local vms, jvmstat uses a ByteBuffer
 * corresponding to mmap()ed hsperfdata file. The hsperfdata file is updated
 * asynchronously by the vm that created the file. The polling that jvmstat api
 * provides is merely an abstraction over this (possibly always up-to-date)
 * ByteBuffer. So the data this class extracts is as current as possible, and
 * does not correspond to when the jvmstat update events fired.
 */
public class VmClassStatDataExtractor {

    /*
     * Note, there may be a performance issue to consider here. We have a lot of
     * string constants. When we start adding some of the more heavyweight
     * features, and running into CPU issues this may need to be reconsidered in
     * order to avoid the String pool overhead. See also:
     * http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#intern()
     */

    // For the definitions of the values, see VmClassStat.

    private final VmUpdate update;

    public VmClassStatDataExtractor(VmUpdate update) {
        this.update = update;
    }

    public Long getLoadedClasses() throws VmUpdateException {
        return update.getPerformanceCounterLong("java.cls.loadedClasses")
                + update.getPerformanceCounterLong("java.cls.sharedLoadedClasses");
    }

    public Long getLoadedBytes() throws VmUpdateException {
        return update.getPerformanceCounterLong("sun.cls.loadedBytes")
                + update.getPerformanceCounterLong("sun.cls.sharedLoadedBytes");
    }

    public Long getUnloadedClasses() throws VmUpdateException {
        return update.getPerformanceCounterLong("java.cls.unloadedClasses")
                + update.getPerformanceCounterLong("java.cls.sharedUnloadedClasses");
    }

    public Long getUnloadedBytes() throws VmUpdateException {
        return update.getPerformanceCounterLong("sun.cls.unloadedBytes")
                + update.getPerformanceCounterLong("sun.cls.sharedUnloadedBytes");
    }

    public Long getClassLoadTime() throws VmUpdateException {
        return update.getPerformanceCounterLong("sun.cls.time")
                / update.getPerformanceCounterLong("sun.os.hrt.frequency");
    }

}
