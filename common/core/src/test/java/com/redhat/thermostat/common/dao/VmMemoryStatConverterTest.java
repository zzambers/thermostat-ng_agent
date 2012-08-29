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

package com.redhat.thermostat.common.dao;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmMemoryStatConverterTest {

    @Test
    public void testVmMemoryStatToChunk() {
        List<Generation> generations = new ArrayList<Generation>();

        int i = 0;
        for (String genName: new String[] { "new", "old", "perm" }) {
            Generation gen = new Generation();
            gen.name = genName;
            gen.collector = gen.name;
            generations.add(gen);
            List<Space> spaces = new ArrayList<Space>();
            gen.spaces = spaces;
            String[] spaceNames = null;
            if (genName.equals("new")) {
                spaceNames = new String[] { "eden", "s0", "s1" };
            } else if (genName.equals("old")) {
                spaceNames = new String[] { "old" };
            } else {
                spaceNames = new String[] { "perm" };
            }
            for (String spaceName: spaceNames) {
                Space space = new Space();
                space.name = spaceName;
                space.index = 0;
                space.used = i++;
                space.capacity = i++;
                space.maxCapacity = i++;
                spaces.add(space);
            }
        }

        VmMemoryStat stat = new VmMemoryStat(1, 2, generations);

        Chunk chunk = new VmMemoryStatConverter().toChunk(stat);

        assertNotNull(chunk);
        assertEquals((Long) 1l, chunk.get(new Key<Long>("timestamp", false)));
        assertEquals((Integer) 2, chunk.get(new Key<Integer>("vm-id", true)));
        assertEquals("new", chunk.get(new Key<String>("eden.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("eden.collector", false)));
        assertEquals((Long) 0l, chunk.get(new Key<Long>("eden.used", false)));
        assertEquals((Long) 1l, chunk.get(new Key<Long>("eden.capacity", false)));
        assertEquals((Long) 2l, chunk.get(new Key<Long>("eden.max-capacity", false)));
        assertEquals("new", chunk.get(new Key<String>("s0.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("s0.collector", false)));
        assertEquals((Long) 3l, chunk.get(new Key<Long>("s0.used", false)));
        assertEquals((Long) 4l, chunk.get(new Key<Long>("s0.capacity", false)));
        assertEquals((Long) 5l, chunk.get(new Key<Long>("s0.max-capacity", false)));
        assertEquals("new", chunk.get(new Key<String>("s1.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("s1.collector", false)));
        assertEquals((Long) 6l, chunk.get(new Key<Long>("s1.used", false)));
        assertEquals((Long) 7l, chunk.get(new Key<Long>("s1.capacity", false)));
        assertEquals((Long) 8l, chunk.get(new Key<Long>("s1.max-capacity", false)));
        assertEquals("old", chunk.get(new Key<String>("old.gen", false)));
        assertEquals("old", chunk.get(new Key<String>("old.collector", false)));
        assertEquals((Long) 9l, chunk.get(new Key<Long>("old.used", false)));
        assertEquals((Long) 10l, chunk.get(new Key<Long>("old.capacity", false)));
        assertEquals((Long) 11l, chunk.get(new Key<Long>("old.max-capacity", false)));
        assertEquals("perm", chunk.get(new Key<String>("perm.gen", false)));
        assertEquals("perm", chunk.get(new Key<String>("perm.collector", false)));
        assertEquals((Long) 12l, chunk.get(new Key<Long>("perm.used", false)));
        assertEquals((Long) 13l, chunk.get(new Key<Long>("perm.capacity", false)));
        assertEquals((Long) 14l, chunk.get(new Key<Long>("perm.max-capacity", false)));

    }

    @Test
    public void testChunkToVmMemoryStat() {
        final long TIMESTAMP = 1234l;
        final int VM_ID = 4567;

        final long EDEN_USED = 1;
        final long EDEN_CAPACITY = 2;
        final long EDEN_MAX_CAPACITY = 3;
        
        final long S0_USED = 4;
        final long S0_CAPACITY = 5;
        final long S0_MAX_CAPACITY = 6;
        
        final long S1_USED = 7;
        final long S1_CAPACITY = 8;
        final long S1_MAX_CAPACITY = 9;
        
        final long OLD_USED = 10;
        final long OLD_CAPACITY = 11;
        final long OLD_MAX_CAPACITY = 12;
        
        final long PERM_USED = 13;
        final long PERM_CAPACITY = 14;
        final long PERM_MAX_CAPACITY = 15;
        
        Chunk chunk = new Chunk(VmMemoryStatDAO.vmMemoryStatsCategory, false);

        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(Key.VM_ID, VM_ID);

        chunk.put(VmMemoryStatDAO.edenGenKey, "new");
        chunk.put(VmMemoryStatDAO.edenCollectorKey, "new-collector");
        chunk.put(VmMemoryStatDAO.edenUsedKey, EDEN_USED);
        chunk.put(VmMemoryStatDAO.edenCapacityKey, EDEN_CAPACITY);
        chunk.put(VmMemoryStatDAO.edenMaxCapacityKey, EDEN_MAX_CAPACITY);

        chunk.put(VmMemoryStatDAO.s0GenKey, "new");
        chunk.put(VmMemoryStatDAO.s0CollectorKey, "new-collector");
        chunk.put(VmMemoryStatDAO.s0UsedKey, S0_USED);
        chunk.put(VmMemoryStatDAO.s0CapacityKey, S0_CAPACITY);
        chunk.put(VmMemoryStatDAO.s0MaxCapacityKey, S0_MAX_CAPACITY);

        chunk.put(VmMemoryStatDAO.s1GenKey, "new");
        chunk.put(VmMemoryStatDAO.s1CollectorKey, "new-collector");
        chunk.put(VmMemoryStatDAO.s1UsedKey, S1_USED);
        chunk.put(VmMemoryStatDAO.s1CapacityKey, S1_CAPACITY);
        chunk.put(VmMemoryStatDAO.s1MaxCapacityKey, S1_MAX_CAPACITY);

        chunk.put(VmMemoryStatDAO.oldGenKey, "old");
        chunk.put(VmMemoryStatDAO.oldCollectorKey, "old-collector");
        chunk.put(VmMemoryStatDAO.oldUsedKey, OLD_USED);
        chunk.put(VmMemoryStatDAO.oldCapacityKey, OLD_CAPACITY);
        chunk.put(VmMemoryStatDAO.oldMaxCapacityKey, OLD_MAX_CAPACITY);

        chunk.put(VmMemoryStatDAO.permGenKey, "perm");
        chunk.put(VmMemoryStatDAO.permCollectorKey, "perm-collector");
        chunk.put(VmMemoryStatDAO.permUsedKey, PERM_USED);
        chunk.put(VmMemoryStatDAO.permCapacityKey, PERM_CAPACITY);
        chunk.put(VmMemoryStatDAO.permMaxCapacityKey, PERM_MAX_CAPACITY);

        VmMemoryStat stat = new VmMemoryStatConverter().fromChunk(chunk);

        assertNotNull(stat);
        assertEquals(TIMESTAMP, stat.getTimeStamp());
        assertEquals(VM_ID, stat.getVmId());

        assertEquals(3, stat.getGenerations().size());

        Generation newGen = stat.getGeneration("new");
        assertNotNull(newGen);
        assertEquals(3, newGen.spaces.size());
        assertEquals("new-collector", newGen.collector);
        
        Space eden = newGen.getSpace("eden");
        assertNotNull(eden);
        assertEquals(EDEN_USED, eden.used);
        assertEquals(EDEN_CAPACITY, eden.capacity);
        assertEquals(EDEN_MAX_CAPACITY, eden.maxCapacity);
        
        Space s0 = newGen.getSpace("s0");
        assertNotNull(s0);
        assertEquals(S0_USED, s0.used);
        assertEquals(S0_CAPACITY, s0.capacity);
        assertEquals(S0_MAX_CAPACITY, s0.maxCapacity);
        
        Space s1 = newGen.getSpace("s1");
        assertNotNull(s1);
        assertEquals(S1_USED, s1.used);
        assertEquals(S1_CAPACITY, s1.capacity);
        assertEquals(S1_MAX_CAPACITY, s1.maxCapacity);
        
        Generation oldGen = stat.getGeneration("old");
        assertNotNull(oldGen);
        assertEquals(1, oldGen.spaces.size());
        assertEquals("old-collector", oldGen.collector);

        Space old = oldGen.getSpace("old");
        assertNotNull(old);
        assertEquals(OLD_USED, old.used);
        assertEquals(OLD_CAPACITY, old.capacity);
        assertEquals(OLD_MAX_CAPACITY, old.maxCapacity);
        
        Generation permGen = stat.getGeneration("perm");
        assertNotNull(permGen);
        assertEquals(1, permGen.spaces.size());
        assertEquals("perm-collector", permGen.collector);
        
        Space permSpace = permGen.getSpace("perm");
        assertNotNull(permSpace);
        assertEquals(PERM_USED, permSpace.used);
        assertEquals(PERM_CAPACITY, permSpace.capacity);
        assertEquals(PERM_MAX_CAPACITY, permSpace.maxCapacity);
    }
}
