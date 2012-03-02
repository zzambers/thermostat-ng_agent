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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.redhat.thermostat.common.storage.Key;

public class SystemBackendTest {

    @Test
    public void testCategories() {
        assertEquals("host-info", SystemBackend.hostInfoCategory.getName());
        Collection<Key<?>> keys = SystemBackend.hostInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<String>("hostname", true)));
        assertTrue(keys.contains(new Key<String>("os_name", false)));
        assertTrue(keys.contains(new Key<String>("os_kernel", false)));
        assertTrue(keys.contains(new Key<Integer>("cpu_num", false)));
        assertTrue(keys.contains(new Key<String>("cpu_model", false)));
        assertTrue(keys.contains(new Key<Long>("memory_total", false)));
        assertEquals(6, keys.size());


        assertEquals("network-info", SystemBackend.networkInfoCategory.getName());
        keys = SystemBackend.networkInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<String>("iface", true)));
        assertTrue(keys.contains(new Key<String>("ipv4addr", false)));
        assertTrue(keys.contains(new Key<String>("ipv6addr", false)));
        assertEquals(4, keys.size());


        assertEquals("cpu-stats", SystemBackend.cpuStatCategory.getName());
        keys = SystemBackend.cpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<Double>("5load", false)));
        assertTrue(keys.contains(new Key<Double>("10load", false)));
        assertTrue(keys.contains(new Key<Double>("15load", false)));
        assertEquals(4, keys.size());


        assertEquals("memory-stats", SystemBackend.memoryStatCategory.getName());
        keys = SystemBackend.memoryStatCategory.getKeys();
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<Long>("total", false)));
        assertTrue(keys.contains(new Key<Long>("free", false)));
        assertTrue(keys.contains(new Key<Long>("buffers", false)));
        assertTrue(keys.contains(new Key<Long>("cached", false)));
        assertTrue(keys.contains(new Key<Long>("swap-total", false)));
        assertTrue(keys.contains(new Key<Long>("swap-free", false)));
        assertTrue(keys.contains(new Key<Long>("commit-limit", false)));
        assertEquals(8, keys.size());


        assertEquals("vm-cpu-stats", SystemBackend.vmCpuStatCategory.getName());
        keys = SystemBackend.vmCpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<Integer>("vm-id", false)));
        assertTrue(keys.contains(new Key<Integer>("processor-usage", false)));
        assertEquals(2, keys.size());
    }
}
