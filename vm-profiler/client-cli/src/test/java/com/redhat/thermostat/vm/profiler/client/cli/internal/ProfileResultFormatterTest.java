/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.client.cli.internal;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

public class ProfileResultFormatterTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final String PERCENTAGE = translator.localize(LocaleResources.METHOD_PROFILE_HEADER_PERCENTAGE).getContents();
    private final String TIME = translator.localize(LocaleResources.METHOD_PROFILE_HEADER_TIME).getContents();
    private final String NAME = translator.localize(LocaleResources.METHOD_PROFILE_HEADER_NAME).getContents();

    private ProfileResultFormatter formatter;
    private ByteArrayOutputStream baos;
    private PrintStream out;

    @Before
    public void setUp() {
        formatter = new ProfileResultFormatter();
        baos = new ByteArrayOutputStream();
        out = new PrintStream(baos);
    }

    @Test
    public void testPrintHeader() {
        formatter.addHeader();
        formatter.format(out);

        String output = new String(baos.toByteArray());
        assertTrue(output.contains(PERCENTAGE));
        assertTrue(output.contains(TIME));
        assertTrue(output.contains(NAME));
    }

    @Test
    public void testPrintProfileResult() {
        String methodName = "Class.method()";
        long time = 1;
        double percentage = 100;

        MethodDeclaration decl = new MethodDeclaration("Class.method", Arrays.<String>asList(), "void");
        MethodInfo info = new MethodInfo(decl, time, percentage);

        formatter.addMethodInfo(info);
        formatter.format(out);

        String output = new String(baos.toByteArray());

        assertTrue(output.contains(methodName));
        assertTrue(output.contains(String.valueOf(time)));
        assertTrue(output.contains(String.valueOf(percentage)));
    }

}
