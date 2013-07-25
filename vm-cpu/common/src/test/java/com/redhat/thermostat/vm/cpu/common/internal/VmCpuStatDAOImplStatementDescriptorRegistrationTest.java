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

package com.redhat.thermostat.vm.cpu.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorMetadataFactory;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;
import com.redhat.thermostat.storage.internal.dao.DAOImplStatementDescriptorRegistration;

public class VmCpuStatDAOImplStatementDescriptorRegistrationTest {

    @Test
    public void registersAllQueries() {
        VmCpuStatDAOImplStatementDescriptorRegistration reg = new VmCpuStatDAOImplStatementDescriptorRegistration();
        Set<String> descriptors = reg.getStatementDescriptors();
        assertEquals(1, descriptors.size());
        assertFalse("null descriptor not allowed", descriptors.contains(null));
    }
    
    /*
     * The web storage end-point uses service loader in order to determine the
     * list of trusted/known registrations. This test is to ensure service loading
     * works for this module's regs. E.g. renaming of the impl class without
     * changing META-INF/services/com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration
     */
    @Test
    public void serviceLoaderCanLoadRegistration() {
        Set<String> expectedClassNames = new HashSet<>();
        expectedClassNames.add(DAOImplStatementDescriptorRegistration.class.getName());
        expectedClassNames.add(VmCpuStatDAOImplStatementDescriptorRegistration.class.getName());
        ServiceLoader<StatementDescriptorRegistration> loader = ServiceLoader.load(StatementDescriptorRegistration.class, VmCpuStatDAOImplStatementDescriptorRegistration.class.getClassLoader());
        List<StatementDescriptorRegistration> registrations = new ArrayList<>(1);
        StatementDescriptorRegistration vmCpuStatReg = null;
        for (StatementDescriptorRegistration r: loader) {
            assertTrue(expectedClassNames.contains(r.getClass().getName()));
            if (r.getClass().getName().equals(VmCpuStatDAOImplStatementDescriptorRegistration.class.getName())) {
                vmCpuStatReg = r;
            }
            registrations.add(r);
        }
        // storage-core + this module
        assertEquals(2, registrations.size());
        assertNotNull(vmCpuStatReg);
        assertEquals(1, vmCpuStatReg.getStatementDescriptors().size());
    }
    
    @Test
    public void canGetMetadataForLatestCpuStatQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        PreparedParameter vmIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        String vmId = "vmId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        when(vmIdParam.getValue()).thenReturn(vmId);
        PreparedParameter[] params = new PreparedParameter[] { agentIdParam,
                vmIdParam };
        
        StatementDescriptorMetadataFactory factory = new VmCpuStatDAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(VmCpuStatDAOImplStatementDescriptorRegistration.descriptor, params);
        assertNotNull(data);
        assertEquals(agentId, data.getAgentId());
        assertEquals(vmId, data.getVmId());
    }
    
    @Test
    public void unknownDescriptorThrowsException() {
        StatementDescriptorMetadataFactory factory = new VmCpuStatDAOImplStatementDescriptorRegistration();
        try {
            factory.getDescriptorMetadata("QUERY foo-bar WHERE 'a' = 'b'", null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }
}
