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

package com.redhat.thermostat.agent.ipc.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.client.internal.ClientIPCPropertiesBuilder.ServiceLoaderHelper;
import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesProvider;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

public class ClientIPCPropertiesBuilderTest {
    
    private IPCProperties props;
    private Properties jProps;
    private ServiceLoaderHelper serviceHelper;
    private IPCPropertiesProvider provider;
    private File propFile;

    @Before
    public void setUp() throws Exception {
        props = mock(IPCProperties.class);
        when(props.getType()).thenReturn(IPCType.UNIX_SOCKET);
        
        provider = mock(IPCPropertiesProvider.class);
        when(provider.getType()).thenReturn(IPCType.UNIX_SOCKET);
        jProps = mock(Properties.class);
        propFile = mock(File.class);
        when(provider.create(jProps, propFile)).thenReturn(props);
        
        serviceHelper = mock(ServiceLoaderHelper.class);
    }

    @Test
    public void testInit() throws Exception {
        Iterable<IPCPropertiesProvider> providers = mockIterator(provider);
        when(serviceHelper.getServiceLoader()).thenReturn(providers);
        
        ClientIPCPropertiesBuilder builder = new ClientIPCPropertiesBuilder(serviceHelper);
        IPCProperties result = builder.getPropertiesForType(IPCType.UNIX_SOCKET, jProps, propFile);
        assertEquals(props, result);
    }
    
    @Test
    public void testInitMultipleProviders() throws Exception {
        IPCPropertiesProvider otherProvider = mock(IPCPropertiesProvider.class);
        when(otherProvider.getType()).thenReturn(IPCType.UNKNOWN);
        
        // Unknown provider should be first
        Iterable<IPCPropertiesProvider> providers = mockIterator(otherProvider, provider);
        when(serviceHelper.getServiceLoader()).thenReturn(providers);
        
        ClientIPCPropertiesBuilder builder = new ClientIPCPropertiesBuilder(serviceHelper);
        IPCProperties result = builder.getPropertiesForType(IPCType.UNIX_SOCKET, jProps, propFile);
        assertEquals(props, result);
        
        verify(otherProvider, never()).create(any(Properties.class), any(File.class));
    }
    
    @Test
    public void testInitNoMatchingProvider() throws Exception {
        IPCPropertiesProvider otherProvider = mock(IPCPropertiesProvider.class);
        when(otherProvider.getType()).thenReturn(IPCType.UNKNOWN);
        
        // Only add Unknown provider
        Iterable<IPCPropertiesProvider> providers = mockIterator(otherProvider);
        when(serviceHelper.getServiceLoader()).thenReturn(providers);
        
        ClientIPCPropertiesBuilder builder = new ClientIPCPropertiesBuilder(serviceHelper);
        try {
            builder.getPropertiesForType(IPCType.UNIX_SOCKET, jProps, propFile);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(otherProvider, never()).create(any(Properties.class), any(File.class));
        }
    }
    
    @Test(expected=IOException.class)
    public void testInitNoProviders() throws Exception {
        Iterable<IPCPropertiesProvider> providers = mockIterator();
        when(serviceHelper.getServiceLoader()).thenReturn(providers);
        
        ClientIPCPropertiesBuilder builder = new ClientIPCPropertiesBuilder(serviceHelper);
        builder.getPropertiesForType(IPCType.UNIX_SOCKET, jProps, propFile);
    }

    @SuppressWarnings("unchecked")
    private <T> Iterable<T> mockIterator(T... provider) {
        Iterable<T> providers = mock(Iterable.class);
        Iterator<T> iterator = mock(Iterator.class);
        when(providers.iterator()).thenReturn(iterator);
        if (provider.length > 1) {
            when(iterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
            when(iterator.next()).thenReturn(provider[0]).thenReturn(provider[1]).thenReturn(null);
        } else if (provider.length == 1) {
            when(iterator.hasNext()).thenReturn(true).thenReturn(false);
            when(iterator.next()).thenReturn(provider[0]).thenReturn(null);
        } else {
            when(iterator.hasNext()).thenReturn(false);
            when(iterator.next()).thenReturn(null);
        }
        return providers;
    }
    
}
