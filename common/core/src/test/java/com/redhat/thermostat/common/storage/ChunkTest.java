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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChunkTest {

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);
    private static final Key<String> key5 = new Key<>("key5", false);
    private static final Key<Integer> key6 = new Key<>("key6", false);
    private static final Key<Object> key7 = new Key<>("key7", false);

    private static final String value1 = "test1";
    private static final String value2 = "test2";
    private static final String value3 = "test3";
    private static final String value4 = "test4";
    private static final String value5 = "test5";
    private static final int value6 = 12345;
    private static final Object value7 = "test7";

    private static final Category testCategory = new Category("ChunkTest", key1, key2, key3, key4, key5);
    private static final Category testCategory2 = new Category("ChunkTest2", key1, key2, key3, key4, key5);

    @Test
    public void verifyGetCategoryNotNull() {
        Chunk chunk = new Chunk(testCategory, false);
        Category cat = chunk.getCategory();
        assertNotNull(cat);
    }

    @Test
    public void verifyGetCategoryReturnsCorrectCategory() {
        Chunk chunk = new Chunk(testCategory, false);
        Category cat = chunk.getCategory();
        assertEquals(cat, testCategory);
    }

    @Test
    public void verifyGetReplaceReturnsCorrectValue() {
        Chunk chunk = new Chunk(testCategory, false);
        boolean replace = chunk.getReplace();
        assertFalse(replace);
        chunk = new Chunk(testCategory, true);
        replace = chunk.getReplace();
        assertTrue(replace);
    }

    @Test
    public void verifyPutActuallyPuts() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1, value1);
        int pieces = chunk.getKeys().size();
        assertEquals(pieces, 1);
    }

    @Test
    public void verifyPutPutsCorrectly() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1, value1);
        String value = chunk.get(key1);
        assertEquals(value, value1);
    }

    @Test
    public void verifyPutAcceptsVariousTypes() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1, value1);
        chunk.put(key5, value5);
        chunk.put(key6, value6);
        chunk.put(key7, value7);
        int pieces = chunk.getKeys().size();
        assertEquals(pieces, 4);
    }

    @Test
    public void verifyEntriesAreKeptInOrder() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key5, value5);
        chunk.put(key4, value4);
        chunk.put(key3, value3);
        chunk.put(key2, value2);
        chunk.put(key1, value1);

        assertArrayEquals(new Key<?>[]{key5, key4, key3, key2, key1}, chunk.getKeys().toArray());
    }

    @Test
    public void verifyGetNotNull() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1, value1);
        String value = chunk.get(key1);
        assertNotNull(value);
    }

    @Test
    public void verifyGetNullWhenExpected() {
        Chunk chunk = new Chunk(testCategory, false);
        chunk.put(key1, value1);
        String value = chunk.get(key2);
        assertNull(value);
    }


    @Test
    public void testEqualsBasicEquals() {
        Chunk chunk1 = new Chunk(testCategory, false);
        Chunk chunk2 = new Chunk(testCategory, false);
        assertTrue(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsDifferentCategory() {
        Chunk chunk1 = new Chunk(testCategory, false);
        Chunk chunk2 = new Chunk(testCategory2, false);
        assertFalse(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsDifferentReplace() {
        // TODO: Do we want to differentiate chunks by that flag? I think not. Maybe it doesn't even belong in chunk.
        Chunk chunk1 = new Chunk(testCategory, false);
        Chunk chunk2 = new Chunk(testCategory, true);
        assertTrue(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsDifferentType() {
        Chunk chunk1 = new Chunk(testCategory, false);
        assertFalse(chunk1.equals("fluff"));
    }

    @Test
    public void testEqualsNull() {
        Chunk chunk1 = new Chunk(testCategory, false);
        assertFalse(chunk1.equals(null));
    }

    @Test
    public void testEqualsDifferentNumberOfValues() {
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(key1, "value1");
        chunk1.put(key2, "value2");
        Chunk chunk2 = new Chunk(testCategory, true);
        chunk2.put(key1, "value1");
        assertFalse(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsDifferentKeys() {
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(key1, "value1");
        chunk1.put(key2, "value2");
        Chunk chunk2 = new Chunk(testCategory, true);
        chunk2.put(key1, "value1");
        chunk2.put(key3, "value2");
        assertFalse(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsDifferentValues() {
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(key1, "value1");
        chunk1.put(key2, "value2.1");
        Chunk chunk2 = new Chunk(testCategory, true);
        chunk2.put(key1, "value1");
        chunk2.put(key2, "value2.2");
        assertFalse(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsSameValues() {
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(key1, "value1");
        chunk1.put(key2, "value2");
        Chunk chunk2 = new Chunk(testCategory, true);
        chunk2.put(key1, "value1");
        chunk2.put(key2, "value2");
        assertTrue(chunk1.equals(chunk2));
    }

    @Test
    public void testEqualsNullValues() {
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(key1, "value1");
        chunk1.put(key2, null);
        Chunk chunk2 = new Chunk(testCategory, true);
        chunk2.put(key1, "value1");
        chunk2.put(key2, null);
        assertTrue(chunk1.equals(chunk2));
    }
}
