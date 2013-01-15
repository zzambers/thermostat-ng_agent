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

package com.redhat.thermostat.web.common;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.model.Pojo;

public class WebQueryTest {

    private static class TestObj implements Pojo {
        
    }

    @Test
    public void test() {
        Key<String> key1 = new Key<>("testkey", true);
        Category<TestObj> category = new Category<>("test", TestObj.class, key1);
        Map<Category,Integer> categoryIdMap = new HashMap<>();
        categoryIdMap.put(category, 42);
        WebQuery query = new WebQuery(42);
        query.where(key1, Criteria.EQUALS, "fluff");

        List<Qualifier<?>> qualifiers = query.getQualifiers();
        assertEquals(1, qualifiers.size());
        Qualifier<?> qualifier = qualifiers.get(0);
        assertEquals(key1, qualifier.getKey());
        assertEquals(Criteria.EQUALS, qualifier.getCriteria());
        assertEquals("fluff", qualifier.getValue());

        assertEquals(42, query.getCategoryId());
    }
}
