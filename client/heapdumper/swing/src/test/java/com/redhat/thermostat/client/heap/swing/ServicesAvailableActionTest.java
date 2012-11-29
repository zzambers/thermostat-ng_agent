/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.heap.swing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.redhat.thermostat.client.core.VmInformationService;
import com.redhat.thermostat.client.heap.HeapDumperService;
import com.redhat.thermostat.client.heap.swing.Activator;
import com.redhat.thermostat.client.heap.swing.Activator.ServicesAvailableAction;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.test.StubBundleContext;

public class ServicesAvailableActionTest {

    @Test
    public void verifyDepsAvailableRegistersVmInformationService() throws Exception {
        StubBundleContext ctx = new StubBundleContext();
        Activator activator = new Activator();
        
        activator.start(ctx);
        
        ServicesAvailableAction action = activator.getServiceAvailableAction();
        ApplicationService appService = mock(ApplicationService.class);
        AgentInfoDAO dao = mock(AgentInfoDAO.class);
        VmMemoryStatDAO vmMemoryDao = mock(VmMemoryStatDAO.class);
        HeapDAO heapDao = mock(HeapDAO.class);
        Map<String, Object> services = new HashMap<>();
        services.put(AgentInfoDAO.class.getName(), dao);
        services.put(ApplicationService.class.getName(), appService);
        services.put(VmMemoryStatDAO.class.getName(), vmMemoryDao);
        services.put(HeapDAO.class.getName(), heapDao);
        
        action.dependenciesAvailable(services);
        
        assertTrue(ctx.isServiceRegistered(VmInformationService.class.getName(), HeapDumperService.class));
        
        // Check no other services are registered
        assertEquals(1, ctx.getAllServices().size());
    }
}
