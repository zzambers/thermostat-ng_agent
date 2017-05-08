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

package com.redhat.thermostat.common.portability;

public class PortableMemoryStat {

    private final long timeStamp;
    private final long total;
    private final long free;
    private final long buffers;
    private final long cached;
    private final long swapTotal;
    private final long swapFree;
    private final long commitLimit;

    public PortableMemoryStat(long timeStamp, long total, long free, long buffers, long cached, long swapTotal, long swapFree, long commitLimit) {
        this.timeStamp = timeStamp;
        this.total = total;
        this.free = free;
        this.buffers = buffers;
        this.cached = cached;
        this.swapTotal = swapTotal;
        this.swapFree = swapFree;
        this.commitLimit = commitLimit;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
    public long getTotal() {
        return total;
    }
    public long getFree() {
        return free;
    }
    public long getBuffers() {
        return buffers;
    }
    public long getCached() {
        return cached;
    }
    public long getSwapTotal() {
        return swapTotal;
    }
    public long getSwapFree() {
        return swapFree;
    }
    public long getCommitLimit() {
        return commitLimit;
    }
}
