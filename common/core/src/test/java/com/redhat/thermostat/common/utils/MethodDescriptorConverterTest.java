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

package com.redhat.thermostat.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MethodDescriptorConverterTest {

    @Test
    public void verifyMethodDescriptors() {
        check("void ???()", "()V");
        check("int ???()", "()I");
        check("int[] ???()", "()[I");
        check("long ???()", "()J");
        check("double ???()", "()D");
        check("java.lang.Object ???()", "()Ljava/lang/Object;");
        check("java.lang.Object[] ???()", "()[Ljava/lang/Object;");

        check("void ???(java.lang.Object[])", "([Ljava/lang/Object;)V");

        check("java.lang.Object ???(int, double, java.lang.Thread)", "(IDLjava/lang/Thread;)Ljava/lang/Object;");
        check("java.lang.Object ???(java.lang.Object, java.lang.String[], java.lang.Thread)", "(Ljava.lang.Object;[Ljava.lang.String;Ljava/lang/Thread;)Ljava/lang/Object;");
        check("java.lang.Object[] ???(int[], double, java.lang.Thread[])", "([ID[Ljava/lang/Thread;)[Ljava/lang/Object;");
    }

    private static void check(String expected, String input) {
        String result = MethodDescriptorConverter.toJavaType(input);
        assertEquals(expected, result);
    }

    @Test
    public void verifyMethodNameAndDescriptors() {
        check("int[] foo()", "foo", "()[I");
        check("java.lang.Object[] foo()", "foo", "()[Ljava/lang/Object;");

        check("java.lang.Object foo(int, double, java.lang.Thread)", "foo", "(IDLjava/lang/Thread;)Ljava/lang/Object;");
        check("java.lang.Object[] foo(int[], double, java.lang.Thread[])", "foo", "([ID[Ljava/lang/Thread;)[Ljava/lang/Object;");
    }

    private static void check(String expected, String methodName, String descriptor) {
        String result = MethodDescriptorConverter.toJavaType(methodName, descriptor);
        assertEquals(expected, result);
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyDescriptorsWithoutParenthesisThrowsExceptions() {
        MethodDescriptorConverter.toJavaType("foo");
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyDescriptorWithBadClassCausesException() {
        MethodDescriptorConverter.toJavaType("foo(Ljava/lang/Object)"); // no ';' matching 'L'
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyUnrecognizedDescriptorCausesException() {
        MethodDescriptorConverter.toJavaType("foo(X)");
    }

}
