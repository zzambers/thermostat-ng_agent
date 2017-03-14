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

package com.redhat.thermostat.thread.dao.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.experimental.statement.Category;
import com.redhat.thermostat.storage.core.experimental.statement.CategoryBuilder;
import com.redhat.thermostat.storage.model.Pojo;

public class ThreadDaoCategoriesTest {

    @Test
    public void testRegister() throws Exception {
        Set<String> set = new HashSet<>();
        ThreadDaoCategories.register(set);

        assertEquals(ThreadDaoCategories.BEANS.size(), set.size());
        for (Class<? extends Pojo> categoryClass : ThreadDaoCategories.BEANS) {
            String name = categoryClass.getAnnotation(Category.class).value();
            assertTrue(set.contains(name));
        }
    }

    @Test
    public void testRegisterInStorage() throws Exception {
        Storage storage = mock(Storage.class);

        List<com.redhat.thermostat.storage.core.Category<?>> categories =
                new ArrayList<>();
        for (Class<? extends Pojo> categoryClass : ThreadDaoCategories.BEANS) {
            categories.add(new CategoryBuilder(categoryClass).build());
        }

        ThreadDaoCategories.register(storage);
        for (com.redhat.thermostat.storage.core.Category<?> category : categories) {
            verify(storage).registerCategory(category);
        }
    }
}
