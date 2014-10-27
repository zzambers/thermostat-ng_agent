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

package com.redhat.thermostat.killvm.common;

import java.net.InetSocketAddress;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;

public class KillVMRequest {
    private static final String RECEIVER = "com.redhat.thermostat.killvm.agent.internal.KillVmReceiver";
    private static final String CMD_CHANNEL_ACTION_NAME = "killvm";

    private RequestQueue queue;

    public KillVMRequest(RequestQueue queue) {
        this.queue = queue;
    }

    public void sendKillVMRequestToAgent(VmRef vmRef, AgentInfoDAO agentInfoDAO, RequestResponseListener listener) {
        String address = agentInfoDAO.getAgentInformation(vmRef.getHostRef()).getConfigListenAddress();

        String [] host = address.split(":");
        InetSocketAddress target = new InetSocketAddress(host[0], Integer.parseInt(host[1]));
        Request murderer = getKillRequest(target);
        murderer.setParameter(Request.ACTION, CMD_CHANNEL_ACTION_NAME);
        murderer.setParameter("vm-pid", String.valueOf(vmRef.getPid()));
        murderer.setReceiver(RECEIVER);
        murderer.addListener(listener);

        queue.putRequest(murderer);
    }

    // for testing
    Request getKillRequest(InetSocketAddress target) {
        return new Request(Request.RequestType.NO_RESPONSE_EXPECTED, target);
    }
}