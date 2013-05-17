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

package com.redhat.thermostat.vm.jmx.common;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

@Entity
public class JmxNotification extends BasePojo implements TimeStampedPojo {

    private long timeStamp;
    private int vmId;
    private String importance;
    private String sourceBackend;
    private String sourceDetails;
    private String contents;

    @Persist
    public int getVmId() {
        return vmId;
    }

    @Persist
    public void setVmId(int vmId) {
        this.vmId = vmId;
    }

    @Persist
    public String getImportance() {
        return importance;
    }

    @Persist
    public void setImportance(String importance) {
        this.importance = importance;
    }

    @Persist
    public String getSourceBackend() {
        return sourceBackend;
    }

    @Persist
    public void setSourceBackend(String sourceBackend) {
        this.sourceBackend = sourceBackend;
    }

    @Persist
    public String getSourceDetails() {
        return sourceDetails;
    }

    @Persist
    public void setSourceDetails(String sourceDetails) {
        this.sourceDetails = sourceDetails;
    }

    @Persist
    public String getContents() {
        return contents;
    }

    @Persist
    public void setContents(String contents) {
        this.contents = contents;
    }

    @Persist
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Persist
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

}
