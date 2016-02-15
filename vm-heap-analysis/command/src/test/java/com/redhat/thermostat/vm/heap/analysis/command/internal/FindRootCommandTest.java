/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.Root;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.Snapshot;

public class FindRootCommandTest {

    private static final String HEAP_ID = "TEST_HEAP_ID";

    private FindRootCommand cmd;

    private JavaHeapObject fooObj;

    private JavaHeapObject barObj;

    private JavaHeapObject bazObj;

    private JavaHeapObject bumObj;

    private JavaHeapObject beeObj;

    private HeapDAO dao;

    @Before
    public void setUp() {
        HeapDump heapDump = setupHeapDump();
        setupDAO(heapDump);

        StubBundleContext context = new StubBundleContext();
        context.registerService(HeapDAO.class, dao, null);

        cmd = new FindRootCommand(context);
    }

    @After
    public void tearDown() {
        fooObj = null;
        barObj = null;
        bazObj = null;
        bumObj = null;
        beeObj = null;
        cmd = null;
    }

    @SuppressWarnings("rawtypes")
    private HeapDump setupHeapDump() {
        fooObj = createMockObject("FooType", "123");
        barObj = createMockObject("BarType", "456");
        bazObj = createMockObject("BazType", "789");
        bumObj = createMockObject("BumType", "987");
        beeObj = createMockObject("BeeType", "654");
        // Setup referrer network.
        Enumeration fooRefs = createReferrerEnum(barObj);
        // The exception is thrown in circle-tests when we visit the foo object again.
        when(fooObj.getReferers()).thenReturn(fooRefs).thenThrow(new RuntimeException());
        Enumeration barRefs = createReferrerEnum(bazObj, bumObj);
        when(barObj.getReferers()).thenReturn(barRefs);
        Enumeration bumRefs = createReferrerEnum(beeObj);
        when(bumObj.getReferers()).thenReturn(bumRefs);
        Enumeration emptyRefs = createReferrerEnum();
        when(bazObj.getReferers()).thenReturn(emptyRefs);
        when(beeObj.getReferers()).thenReturn(emptyRefs);

        // Setup referrer descriptions.
        when(barObj.describeReferenceTo(same(fooObj), any(Snapshot.class))).thenReturn("field foo");
        when(bazObj.describeReferenceTo(same(barObj), any(Snapshot.class))).thenReturn("field bar");
        when(bumObj.describeReferenceTo(same(barObj), any(Snapshot.class))).thenReturn("field bar");
        when(beeObj.describeReferenceTo(same(bumObj), any(Snapshot.class))).thenReturn("field bum");

        // Setup roots.
        Root bazRoot = mock(Root.class);
        when(bazRoot.getDescription()).thenReturn("baz root");
        when(bazObj.getRoot()).thenReturn(bazRoot);
        Root beeRoot = mock(Root.class);
        when(beeRoot.getDescription()).thenReturn("bee root");
        when(beeObj.getRoot()).thenReturn(beeRoot);

        // Setup heap dump.
        HeapDump heapDump = mock(HeapDump.class);
        when(heapDump.findObject("foo")).thenReturn(fooObj);
        return heapDump;
    }

    private void setupDAO(HeapDump heapDump) {

        HeapInfo heapInfo = mock(HeapInfo.class);

        dao = mock(HeapDAO.class);
        when(dao.getHeapInfo(HEAP_ID)).thenReturn(heapInfo);
        when(dao.getHeapDump(heapInfo)).thenReturn(heapDump);

    }

    private JavaHeapObject createMockObject(String className, String id) {
        JavaClass cls = mock(JavaClass.class);
        when(cls.getName()).thenReturn(className);
        JavaHeapObject obj = mock(JavaHeapObject.class);
        when(obj.getIdString()).thenReturn(id);
        when(obj.getClazz()).thenReturn(cls);
        return obj;
    }

    @SuppressWarnings("rawtypes")
    private Enumeration createReferrerEnum(JavaHeapObject... objs) {
        Enumeration refs = mock(Enumeration.class);
        OngoingStubbing<Boolean> hasMoreElements = when(refs.hasMoreElements());
        for (int i = 0; i < objs.length; i++) {
            hasMoreElements = hasMoreElements.thenReturn(true);
        }
        hasMoreElements.thenReturn(false);
        OngoingStubbing<Object> nextElement = when(refs.nextElement());
        for (JavaHeapObject obj : objs) {
            nextElement = nextElement.thenReturn(obj);
        }
        nextElement.thenReturn(null);
        return refs;
    }

    @Test
    public void testStorageRequired() {
        assertTrue(cmd.isStorageRequired());
    }

    @Test
    public void testSimpleAllRootsSearch() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "foo");
        args.addArgument("all", "true");

        cmd.run(factory.createContext(args));

        String expected = "baz root -> BazType@789\n" +
                          "\u2514field bar in BazType@789 -> BarType@456\n" +
                          " \u2514field foo in BarType@456 -> FooType@123\n" +
                          "\n" +
                          "bee root -> BeeType@654\n" +
                          "\u2514field bum in BeeType@654 -> BumType@987\n" +
                          " \u2514field bar in BumType@987 -> BarType@456\n" +
                          "  \u2514field foo in BarType@456 -> FooType@123\n" +
                          "\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test
    public void testSimpleRootSearch() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "foo");

        cmd.run(factory.createContext(args));

        String expected = "baz root -> BazType@789\n" +
                          "\u2514field bar in BazType@789 -> BarType@456\n" +
                          " \u2514field foo in BarType@456 -> FooType@123\n" +
                          "\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test
    public void testSearchWithoutRoot() throws CommandException {
        when(bazObj.getRoot()).thenReturn(null);
        when(beeObj.getRoot()).thenReturn(null);

        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "foo");

        cmd.run(factory.createContext(args));

        String expected = "No root found for: FooType@123\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test
    public void testSearchWithoutRootWithCircle() throws CommandException {
        when(bazObj.getRoot()).thenReturn(null);
        @SuppressWarnings("rawtypes")
        Enumeration bazReferrers = createReferrerEnum(fooObj);
        when(bazObj.getReferers()).thenReturn(bazReferrers);
        when(beeObj.getRoot()).thenReturn(null);
        @SuppressWarnings("rawtypes")
        Enumeration beeReferrers = createReferrerEnum(fooObj);
        when(beeObj.getReferers()).thenReturn(beeReferrers);

        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "foo");

        cmd.run(factory.createContext(args));

        String expected = "No root found for: FooType@123\n";

        assertEquals(expected, factory.getOutput());

    }

    @Test
    public void testHeapNotFound() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", "fluff");
        args.addArgument("objectId", "foo");

        try {
            cmd.run(factory.createContext(args));
            fail();
        } catch (HeapNotFoundException ex) {
           assertEquals("Heap ID not found: fluff", ex.getMessage());
        }
    }

    @Test
    public void testObjectNotFound() throws CommandException {
        TestCommandContextFactory factory = new TestCommandContextFactory();
        SimpleArguments args = new SimpleArguments();
        args.addArgument("heapId", HEAP_ID);
        args.addArgument("objectId", "fluff");

        try {
            cmd.run(factory.createContext(args));
            fail();
        } catch (ObjectNotFoundException ex) {
            assertEquals("Object not found: fluff", ex.getMessage());
         }
    }
}

