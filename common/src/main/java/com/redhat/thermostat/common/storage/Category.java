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

package com.redhat.thermostat.common.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Category {
    private final String name;
    private final List<Key<?>> keys;
    private boolean locked = false;

    private ConnectionKey connectionKey;

    private static Set<String> categoryNames = new HashSet<String>();

    /**
     * Creates a new Category instance with the specified name.
     *
     * @param name the name of the category
     *
     * @throws IllegalArgumentException if a Category is created with a name that has been used before
     */
    public Category(String name) {
        if (categoryNames.contains(name)) {
            throw new IllegalStateException();
        }
        categoryNames.add(name);
        this.name = name;
        keys = new ArrayList<Key<?>>();
    }

    public String getName() {
        return name;
    }

    public synchronized void lock() {
        locked = true;
    }

    public synchronized void addKey(Key<?> key) {
        if (!locked) {
            keys.add(key);
        } else {
            throw new IllegalStateException("Once locked, a category's keys may not be changed.");
        }
    }

    public synchronized Collection<Key<?>> getKeys() {
        return Collections.unmodifiableCollection(keys);
    }

    public void setConnectionKey(ConnectionKey connKey) {
        connectionKey = connKey;
    }

    public ConnectionKey getConnectionKey() {
        return connectionKey;
    }

    public boolean hasBeenRegistered() {
        return getConnectionKey() != null;
    }
}
