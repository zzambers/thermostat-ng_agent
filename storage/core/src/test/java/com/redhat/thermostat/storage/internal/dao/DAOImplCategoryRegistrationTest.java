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

package com.redhat.thermostat.storage.internal.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.SchemaInfo;
import com.redhat.thermostat.storage.core.auth.CategoryRegistration;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class DAOImplCategoryRegistrationTest {

    @Test
    public void registersAllCategories() {
        DAOImplCategoryRegistration reg = new DAOImplCategoryRegistration();
        Set<String> categories = reg.getCategoryNames();
        assertEquals(6, categories.size());
        assertFalse("null descriptor not allowed", categories.contains(null));
        assertTrue(categories.contains(HostInfoDAO.hostInfoCategory.getName()));
        assertTrue(categories.contains(VmInfoDAO.vmInfoCategory.getName()));
        assertTrue(categories.contains(AgentInfoDAO.CATEGORY.getName()));
        assertTrue(categories.contains(NetworkInterfaceInfoDAO.networkInfoCategory.getName()));
        assertTrue(categories.contains(BackendInfoDAO.CATEGORY.getName()));
        assertTrue(categories.contains(SchemaInfo.CATEGORY.getName()));
    }
    
    /*
     * The web storage end-point uses service loader in order to determine the
     * list of trusted/known categories. This test is to ensure service loading
     * works for this module's regs. E.g. renaming of the impl class without
     * changing META-INF/com.redhat.thermostat.storage.core.auth.CategoryRegistration
     */
    @Test
    public void serviceLoaderCanLoadRegistration() {
        Set<String> expectedClassNames = new HashSet<>();
        expectedClassNames.add(DAOImplCategoryRegistration.class.getName());
        ServiceLoader<CategoryRegistration> loader = ServiceLoader.load(CategoryRegistration.class, DAOImplCategoryRegistration.class.getClassLoader());
        List<CategoryRegistration> registrations = new ArrayList<>(1);
        CategoryRegistration daoImplReg = null;
        for (CategoryRegistration r: loader) {
            assertTrue(expectedClassNames.contains(r.getClass().getName()));
            if (r.getClass().getName().equals(DAOImplCategoryRegistration.class.getName())) {
                daoImplReg = r;
            }
            registrations.add(r);
        }
        // storage-core + this module
        assertEquals(1, registrations.size());
        assertNotNull(daoImplReg);
        assertEquals(6, daoImplReg.getCategoryNames().size());
    }
}

