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

package com.redhat.thermostat.killvm.command.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.killvm.common.VMKilledListener;

public class ShellVMKilledListenerTest {

    private final ShellVMKilledListener listener = new ShellVMKilledListener();
    private final PrintStream printStream = mock(PrintStream.class);
    private final Request request = mock(Request.class);

    @Before
    public void setup() {
        listener.setErr(printStream);
        listener.setOut(printStream);
    }

    @Test
    public void testOkayResponse() {
        Response resp = new Response(Response.ResponseType.OK);

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(printStream).println(new String("VM with id null killed successfully."));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);
    }

    @Test
    public void testErrorResponse() {
        Response resp = new Response(Response.ResponseType.ERROR);

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(printStream).println(new String("Kill request error for VM ID null"));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);
    }

    @Test
    public void testNotOkayResponse() {
        Response resp = new Response(Response.ResponseType.NOK);

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(printStream).println(new String("Kill request acknowledged and refused."));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);
    }

    @Test
    public void testNoOpResponse() {
        Response resp = new Response(Response.ResponseType.NOOP);

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(printStream).println(new String("Kill request acknowledged and no action taken."));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);
    }

    @Test
    public void testAuthFailedResponse() {

        Response resp = new Response(Response.ResponseType.AUTH_FAILED);

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(printStream).println(new String("Unauthorized kill request ignored."));

        listener.fireComplete(request, resp);

        assertTrue(complete[0]);
    }
}
