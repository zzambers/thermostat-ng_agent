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

package com.redhat.thermostat.killvm.common;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;

public class KillVMRequestTest {

    private AgentInfoDAO agentDAO;
    private RequestQueue queue;
    private VmRef vm;
    private AgentId agentId;
    private AgentInformation info;

    private KillVMRequest killVMRequest;
    private Request request;
    private RequestResponseListener listener;

    @Before
    public void setup() {
        agentDAO = mock(AgentInfoDAO.class);
        vm = mock(VmRef.class);
        agentId = mock(AgentId.class);

        request = mock(Request.class);

        listener = mock(RequestResponseListener.class);

        when(vm.getPid()).thenReturn(123456);

        info = mock(AgentInformation.class);
        when(info.getRequestQueueAddress()).thenReturn(new InetSocketAddress("0.0.42.42", 42));

        when(agentDAO.getAgentInformation(agentId)).thenReturn(info);

        queue = mock(RequestQueue.class);
    }

    @Test
    public void testSendKillVMRequestToAgent() {
        final boolean [] results = new boolean [3];
        killVMRequest = new KillVMRequest(queue) {
            @Override
            Request getKillRequest(InetSocketAddress target) {
                results[0] = true;
                if (target.getHostString().equals("0.0.42.42")) {
                    results[1] = true;
                }
                if (target.getPort() == 42) {
                    results[2] = true;
                }

                return request;
            }
        };

        killVMRequest.sendKillVMRequestToAgent(agentId, vm.getPid(), agentDAO, listener);
        verify(vm).getPid();
        verify(agentDAO).getAgentInformation(agentId);
        verify(info).getRequestQueueAddress();

        assertTrue(results[0]);
        assertTrue(results[1]);
        assertTrue(results[2]);

        verify(request).setReceiver("com.redhat.thermostat.killvm.agent.internal.KillVmReceiver");
        verify(request).setParameter(Request.ACTION, "killvm");
        verify(request).setParameter("vm-pid", "123456");
        verify(request).addListener(listener);

        verify(queue).putRequest(request);
    }

}
