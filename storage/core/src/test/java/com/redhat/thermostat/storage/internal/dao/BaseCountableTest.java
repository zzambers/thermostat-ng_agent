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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.AggregateCount;

public class BaseCountableTest {

    @Test
    public void testGetCountSuccessful() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Category<AggregateCount> mockCategory = (Category<AggregateCount>)mock(Category.class);
        BaseCountable countable = new BaseCountable();
        String strDesc = "QUERY-COUNT vm-info";
        StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(mockCategory, strDesc);
        @SuppressWarnings("unchecked")
        PreparedStatement<AggregateCount> prepared = (PreparedStatement<AggregateCount>)mock(PreparedStatement.class);
        when(storage.prepareStatement(eq(desc))).thenReturn(prepared);
        AggregateCount c = new AggregateCount();
        c.setCount(3);
        Cursor<AggregateCount> cursor = c.getCursor();
        when(prepared.executeQuery()).thenReturn(cursor);
        long count = countable.getCount(desc, storage);
        assertEquals(3, count);
    }
    
    @Test
    public void testGetCountError() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Category<AggregateCount> mockCategory = (Category<AggregateCount>)mock(Category.class);
        BaseCountable countable = new BaseCountable();
        String strDesc = "QUERY-COUNT vm-info";
        StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(mockCategory, strDesc);
        doThrow(DescriptorParsingException.class).when(storage).prepareStatement(eq(desc));
        long count = countable.getCount(desc, storage);
        assertEquals(-1, count);
    }
    
    @Test
    public void testGetCountError2() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Category<AggregateCount> mockCategory = (Category<AggregateCount>)mock(Category.class);
        BaseCountable countable = new BaseCountable();
        String strDesc = "QUERY-COUNT vm-info";
        StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(mockCategory, strDesc);
        @SuppressWarnings("unchecked")
        PreparedStatement<AggregateCount> prepared = (PreparedStatement<AggregateCount>)mock(PreparedStatement.class);
        when(storage.prepareStatement(eq(desc))).thenReturn(prepared);
        doThrow(StatementExecutionException.class).when(prepared).executeQuery();
        long count = countable.getCount(desc, storage);
        assertEquals(-1, count);
    }
}

