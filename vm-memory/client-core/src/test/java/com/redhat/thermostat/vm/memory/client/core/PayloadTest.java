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

package com.redhat.thermostat.vm.memory.client.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.redhat.thermostat.common.Size;

public class PayloadTest {

    @Test
    public void testClone() {
        
        StatsModel model = new StatsModel();
        model.setName("fluffModel");
        model.setRange(100);
        model.addData(500, 2.0);
        model.addData(501, 2.1);
        
        Payload source = new Payload();
        source.setCapacity(10.0);
        source.setName("fluff");
        source.setCapacityUnit(Size.Unit.GiB);
        source.setMaxCapacity(100.0);
        source.setMaxUsed(5.0);
        source.setUsed(3.0);
        source.setUsedUnit(Size.Unit.MiB);
        source.setModel(model);
        source.setTooltip("fluffTooltip");
        
        Payload cloned = source.clone();
        assertNotSame(cloned, source);
        
        assertEquals(source.getName(), cloned.getName());
        assertEquals(source.getCapacity(), cloned.getCapacity(), 0);
        assertEquals(source.getCapacityUnit(), cloned.getCapacityUnit());
        assertEquals(source.getMaxCapacity(), cloned.getMaxCapacity(), 0);
        assertEquals(source.getMaxUsed(), cloned.getMaxUsed(), 0);
        assertEquals(source.getTooltip(), cloned.getTooltip());
        assertEquals(source.getUsed(), cloned.getUsed(), 0);
        assertEquals(source.getUsedUnit(), cloned.getUsedUnit());
        assertNotSame(source.getModel(), cloned.getModel());

        assertEquals(source.getModel().getName(), cloned.getModel().getName());
    }
}

