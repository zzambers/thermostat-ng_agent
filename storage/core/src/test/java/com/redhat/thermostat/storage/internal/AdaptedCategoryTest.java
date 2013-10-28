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

package com.redhat.thermostat.storage.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.VmInfo;

public class AdaptedCategoryTest {

    @Test
    public void testEquals() {
        AdaptedCategory<AggregateCount, AgentInformation> cat = new AdaptedCategory<>(AgentInfoDAO.CATEGORY, AggregateCount.class);
        assertFalse(cat.equals(AgentInfoDAO.CATEGORY));
        // equals self
        assertEquals(cat, cat);
        // not equal to any other category
        assertFalse(VmInfoDAO.vmInfoCategory.equals(cat));
    }
    
    @Test
    public void testHashCode() {
        AdaptedCategory<AggregateCount, AgentInformation> cat = new AdaptedCategory<>(AgentInfoDAO.CATEGORY, AggregateCount.class);
        assertTrue("Adapted and original must have different hash code",
                cat.hashCode() != AgentInfoDAO.CATEGORY.hashCode());
    }
    
    @Test
    public void getDataClass() {
        AdaptedCategory<AggregateCount, AgentInformation> cat = new AdaptedCategory<>(AgentInfoDAO.CATEGORY, AggregateCount.class);
        assertEquals(AggregateCount.class, cat.getDataClass());
        assertTrue(AggregateResult.class.isAssignableFrom(cat.getDataClass()));
    }
    
    @Test
    public void keysAreImmutable() {
        AdaptedCategory<AggregateCount, AgentInformation> cat = new AdaptedCategory<>(AgentInfoDAO.CATEGORY, AggregateCount.class);
        try {
            cat.getKeys().add(new Key<>("foo"));
            fail("keys need to be immutable");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }
    
    @Test
    public void adaptNonAggregateDataClass() {
        try {
            new AdaptedCategory<>(AgentInfoDAO.CATEGORY, VmInfo.class);
        } catch (IllegalArgumentException e) {
            // pass
            assertTrue(e.getMessage().contains("Can only adapt to aggregate results"));
        }
    }
    
}
