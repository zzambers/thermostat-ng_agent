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

package com.redhat.thermostat.vm.byteman.agent.internal;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.byteman.agent.internal.typeadapter.BytemanMetricTypeAdapter;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

class BytemanMetricsReceiver implements ThermostatIPCCallbacks {
    
    private static final Logger logger = LoggingUtils.getLogger(BytemanMetricsReceiver.class);
    private final VmBytemanDAO dao;
    private final VmSocketIdentifier socketId;
    
    BytemanMetricsReceiver(VmBytemanDAO dao, VmSocketIdentifier socketId) {
        this.dao = dao;
        this.socketId = socketId;
    }

    @Override
    public byte[] dataReceived(byte[] data) {
        String jsonMetric = new String(data, Charset.forName("UTF-8"));
        logger.fine("Received metrics from byteman for socketId: " + socketId.getName() + ". Metric was: " + jsonMetric);
        List<BytemanMetric> metrics = convertFromJson(jsonMetric);
        for (BytemanMetric metric: metrics) {
            dao.addMetric(metric);
        }
        return null; // No response to send back to the IPC endpoint
    }

    private List<BytemanMetric> convertFromJson(String data) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(BytemanMetric.class, new BytemanMetricTypeAdapter())
                .create();
        BytemanMetric[] metrics = gson.fromJson(data, BytemanMetric[].class);
        List<BytemanMetric> listOfMetrics = new ArrayList<>();
        for (BytemanMetric m: metrics) {
            m.setAgentId(socketId.getAgentId());
            m.setVmId(socketId.getVmId());
            m.setTimeStamp(System.currentTimeMillis());
            listOfMetrics.add(m);
        }
        return listOfMetrics;
    }

}
