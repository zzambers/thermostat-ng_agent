/*
 * Copyright 2013 Red Hat, Inc.
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


package com.redhat.thermostat.numa.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;

public class NumaDAOTest {

    @Test
    public void testCategory() {
        Category<?> cat = NumaDAO.numaStatCategory;
        assertEquals("numa-stat", cat.getName());
        assertEquals(NumaStat.class, cat.getDataClass());
        assertEquals(9, cat.getKeys().size());

        assertEquals("agentId", cat.getKey("agentId").getName());
        assertTrue(cat.getKey("agentId").isPartialCategoryKey());
        assertEquals("timeStamp", cat.getKey("timeStamp").getName());
        assertFalse(cat.getKey("timeStamp").isPartialCategoryKey());

        assertEquals("node", cat.getKey("node").getName());
        assertTrue(cat.getKey("node").isPartialCategoryKey());

        assertEquals("numaHit", cat.getKey("numaHit").getName());
        assertFalse(cat.getKey("numaHit").isPartialCategoryKey());
        assertEquals("numaMiss", cat.getKey("numaMiss").getName());
        assertFalse(cat.getKey("numaMiss").isPartialCategoryKey());
        assertEquals("numaForeign", cat.getKey("numaForeign").getName());
        assertFalse(cat.getKey("numaForeign").isPartialCategoryKey());
        assertEquals("interleaveHit", cat.getKey("interleaveHit").getName());
        assertFalse(cat.getKey("interleaveHit").isPartialCategoryKey());
        assertEquals("localNode", cat.getKey("localNode").getName());
        assertFalse(cat.getKey("localNode").isPartialCategoryKey());
        assertEquals("otherNode", cat.getKey("otherNode").getName());
        assertFalse(cat.getKey("otherNode").isPartialCategoryKey());
    }
}
