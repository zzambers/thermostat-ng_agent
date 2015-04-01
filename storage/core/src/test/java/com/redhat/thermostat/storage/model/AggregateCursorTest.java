/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.storage.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Cursor;

public class AggregateCursorTest {

    @Test
    public void testCursor() {
        AggregateTest t = new AggregateTest();
        Cursor<AggregateTest> cursor = t.getCursor();
        assertTrue(cursor.hasNext());
        AggregateTest actual = cursor.next();
        assertSame(t, actual);
        assertFalse(cursor.hasNext());
        try {
            cursor.next();
            fail("Cursor should throw a NoSuchElementException!");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /**
     * Setting the batch size for single result lists should be no-op.
     * This just makes sure that nothing bad happens (no exceptions being thrown)
     */
    @Test
    public void testCursorBatchSize() {
        AggregateTest t = new AggregateTest();
        Cursor<AggregateTest> cursor = t.getCursor();
        cursor.setBatchSize(500);
        assertEquals(500, cursor.getBatchSize());
    }
    
    private static class AggregateTest implements AggregateResult {
        
        @SuppressWarnings("unchecked")
        private <T extends Pojo> Cursor<T> getCursor() {
            return (Cursor<T>)new AggregateCursor<>(this);
        }
    }
}
