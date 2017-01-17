/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;

public class MongoQueryTest {
    
    private static class TestClass implements Pojo {
        
    }

    private static MongoStorage storage;
    private static Category<TestClass> category;
    private static MongoExpressionParser parser;
    private static Document dbObject;

    @BeforeClass
    public static void setUp() {
        storage = mock(MongoStorage.class);
        category = new Category<>("some-collection", TestClass.class);
        parser = mock(MongoExpressionParser.class);
        dbObject = new Document();
        when(parser.parse(any(Expression.class))).thenReturn(dbObject);
    }

    @AfterClass
    public static void tearDown() {
        storage = null;
        category = null;
        parser = null;
    }

    @Test
    public void testEmptyQuery() {
        MongoQuery<TestClass> query = new MongoQuery<>(storage, category, parser);
        Document mongoQuery = query.getGeneratedQuery();
        assertFalse(query.hasClauses());
        assertTrue(mongoQuery.keySet().isEmpty());
    }

    @Test
    public void testCollectionName() {
        MongoQuery<TestClass> query = new MongoQuery<>(storage, category, parser);
        assertEquals("some-collection", query.getCategory().getName());
    }
    
    @Test
    public void testWhere() {
        MongoQuery<TestClass> query = new MongoQuery<>(storage, category, parser);
        Expression expr = mock(Expression.class);
        query.where(expr);
        Document generatedQuery = query.getGeneratedQuery();
        assertTrue(query.hasClauses());
        assertEquals(dbObject, generatedQuery);
    }
    
}

