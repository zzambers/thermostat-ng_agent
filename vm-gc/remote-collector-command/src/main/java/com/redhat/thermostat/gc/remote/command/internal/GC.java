/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.gc.remote.command.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class GC {

    private static final Logger logger = LoggingUtils.getLogger(GC.class);

    private MXBeanConnectionPool pool;
    private int vmId;

    public GC(MXBeanConnectionPool pool, int vmId) {
        this.pool = pool;
        this.vmId = vmId;
    }

    public void gc() throws GCException {

        Exception exceptionInGc = null;

        try {
            MXBeanConnection connection = pool.acquire(vmId);
            try {
                MemoryMXBean bean = connection.createProxy(ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
                bean.gc();

            } catch (Exception ex) {
                exceptionInGc = ex;
                logger.log(Level.SEVERE, "can't get MXBeanConnection connection", ex);
            } finally {
                pool.release(vmId, connection);
            }
        } catch (Exception ioe) {
            exceptionInGc = ioe;
        }

        if (exceptionInGc != null) {
            throw new GCException("error performing gc", exceptionInGc);
        }
    }
}

