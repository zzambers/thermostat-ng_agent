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

package com.redhat.thermostat.storage.core.experimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.AggregateQuery.AggregateFunction;
import com.redhat.thermostat.storage.core.experimental.AggregateQuery2;
import com.redhat.thermostat.storage.model.AggregateCount;

public class AggregateQuery2Test {

    @Test
    public void testSetAggregateKey() {
        @SuppressWarnings("unchecked")
        Query<AggregateCount> query = mock(Query.class);
        TestAggregateQuery aggregate = new TestAggregateQuery(AggregateFunction.COUNT, query);
        assertNull(aggregate.getAggregateKey());
        try {
            aggregate.setAggregateKey(null);
            fail("Expected NPE.");
        } catch (NullPointerException e) {
            // pass
        }
        @SuppressWarnings("rawtypes")
        Key<?> fooKey = new Key("foo");
        aggregate.setAggregateKey(fooKey);
        assertEquals(fooKey, aggregate.getAggregateKey());
    }
    
    private static class TestAggregateQuery extends AggregateQuery2<AggregateCount> {

        public TestAggregateQuery(
                AggregateFunction function,
                Query<AggregateCount> queryToAggregate) {
            super(function, queryToAggregate);
        }

        @Override
        public Cursor<AggregateCount> execute() {
            throw new AssertionError("not implemented");
        }

        @Override
        public Statement<AggregateCount> getRawDuplicate() {
            throw new AssertionError("not implemented");
        }
        
    }
}
