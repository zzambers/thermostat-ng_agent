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

package com.redhat.thermostat.thread.dao.impl.statement.support;

import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.TimeStampedPojo;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StatementUtilsTest {

    @Category("testCategory")
    private static class SampleBean implements TimeStampedPojo {

        private long timeStamp;
        private int vmId;
        private String name;

        @Indexed
        @Persist
        public int getVmId() {
            return vmId;
        }

        @Indexed
        @Persist
        public void setVmId(int vmId) {
            this.vmId = vmId;
        }

        @Persist
        public String getName() {
            return name;
        }

        @Persist
        public void setName(String name) {
            this.name = name;
        }

        @Indexed
        @Persist
        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        @Indexed
        @Persist
        @Override
        public long getTimeStamp() {
            return timeStamp;
        }
    }

    @Category("testCategory2")
    private static class InvalidSampleBean implements TimeStampedPojo {

        private long timeStamp;
        private int vmId;
        private String name;

        public int getVmId() {
            return vmId;
        }

        @Indexed
        @Persist
        public void setVmId(int vmId) {
            this.vmId = vmId;
        }

        @Persist
        public String getName() {
            return name;
        }

        @Persist
        public void setName(String name) {
            this.name = name;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        @Indexed
        @Persist
        @Override
        public long getTimeStamp() {
            return timeStamp;
        }
    }

    @Test
    public void testAnnotationSupport() {
        List<FieldDescriptor> descriptors =
                StatementUtils.createDescriptors(SampleBean.class);
        Map<String, FieldDescriptor> map =
                StatementUtils.createDescriptorMap(descriptors);

        assertTrue(map.containsKey("timeStamp"));

        descriptors =  StatementUtils.createDescriptors(InvalidSampleBean.class);
        map = StatementUtils.createDescriptorMap(descriptors);

        assertFalse(map.containsKey("timeStamp"));
    }
}
