/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.agent.proxy.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.proxy.common.AgentProxyControl;
import com.redhat.thermostat.agent.proxy.server.AgentProxyLoginImpl.LoginContextCreator;

public class AgentProxyLoginImplTest {
    
    private RegistryUtils registryUtils;
    private Registry registry;
    private UnixCredentials creds;
    private LoginContextCreator contextCreator;
    private AgentProxyLoginContext context;

    @Before
    public void setup() throws Exception {
        registry = mock(Registry.class);
        registryUtils = mock(RegistryUtils.class);
        when(registryUtils.getRegistry()).thenReturn(registry);
        creds = new UnixCredentials(9000, 9001, 0);
        contextCreator = mock(LoginContextCreator.class);
        context = mock(AgentProxyLoginContext.class);
        when(contextCreator.createContext(any(Subject.class), same(creds))).thenReturn(context);
    }
    
    @Test
    public void testLoginSuccess() throws Exception {
        ShutdownListener listener = mock(ShutdownListener.class);
        AgentProxyLoginImpl proxyLogin = new AgentProxyLoginImpl(creds, 0, listener, contextCreator, registryUtils);
        AgentProxyControl stub = proxyLogin.login();
        
        ArgumentCaptor<AgentProxyControl> captor = ArgumentCaptor.forClass(AgentProxyControl.class);
        verify(registryUtils).exportObject(captor.capture());
        AgentProxyControl control = captor.getValue();
        
        assertTrue(control instanceof AgentProxyControlWrapper);
        assertFalse(stub instanceof AgentProxyControlWrapper);
    }
    
    @Test
    public void testLoginFailure() throws Exception {
        ShutdownListener listener = mock(ShutdownListener.class);
        
        // Simulate login failure
        LoginException ex = new LoginException("TEST");
        doThrow(ex).when(context).login();
        
        AgentProxyLoginImpl proxyLogin = new AgentProxyLoginImpl(creds, 0, listener, contextCreator, registryUtils);
        
        try {
            proxyLogin.login();
            fail("Expected exception from login");
        } catch (RemoteException e) {
            assertEquals(ex, e.getCause());
        }
        
        verify(registryUtils, never()).exportObject(any(AgentProxyControl.class));
    }

}
