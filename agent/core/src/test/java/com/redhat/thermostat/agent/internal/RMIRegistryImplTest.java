package com.redhat.thermostat.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.internal.RMIRegistryImpl.RegistryWrapper;
import com.redhat.thermostat.agent.internal.RMIRegistryImpl.ServerSocketCreator;

public class RMIRegistryImplTest {
    
    private RegistryWrapper wrapper;
    private Registry reg;
    private RMIRegistryImpl registry;
    private ServerSocketCreator sockCreator;
    
    @Before
    public void setup() throws RemoteException {
        wrapper = mock(RegistryWrapper.class);
        reg = mock(Registry.class);
        when(wrapper.createRegistry(anyInt(), any(RMIClientSocketFactory.class), 
                any(RMIServerSocketFactory.class))).thenReturn(reg);
        sockCreator = mock(ServerSocketCreator.class);
        
        registry = new RMIRegistryImpl(wrapper, sockCreator);
    }

    @Test
    public void testRegistryStart() throws IOException {
        registry.start();
        
        ArgumentCaptor<Integer> portCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RMIClientSocketFactory> csfCaptor = ArgumentCaptor.forClass(RMIClientSocketFactory.class);
        ArgumentCaptor<RMIServerSocketFactory> ssfCaptor = ArgumentCaptor.forClass(RMIServerSocketFactory.class);
        verify(wrapper).createRegistry(portCaptor.capture(), csfCaptor.capture(), ssfCaptor.capture());
        
        // Ensure defaults used for port and client socket factory
        int port = portCaptor.getValue();
        assertEquals(Registry.REGISTRY_PORT, port);
        
        RMIClientSocketFactory csf = csfCaptor.getValue();
        assertEquals(RMISocketFactory.getDefaultSocketFactory(), csf);
        
        // Ensure bound to loopback address
        RMIServerSocketFactory ssf = ssfCaptor.getValue();
        ssf.createServerSocket(port);
        verify(sockCreator).createSocket(port, 0, InetAddress.getLoopbackAddress());
    }

    @Test
    public void testRegistryStop() throws IOException {
        registry.start();
        
        registry.stop();
        
        verify(wrapper).destroyRegistry(reg);
        assertNull(registry.getRegistryImpl());
    }
    
    @Test
    public void testRegistryStopNotStarted() throws IOException {
        registry.stop();
        
        verify(wrapper, never()).destroyRegistry(reg);
    }
    
    @Test
    public void testGetRegistry() throws Exception {
        Registry stub = mock(Registry.class);
        when(wrapper.getRegistry()).thenReturn(stub);
        assertEquals(stub, registry.getRegistry());
    }
    
    @Test
    public void testExportObject() throws Exception {
        Remote obj = mock(Remote.class);
        Remote stub = mock(Remote.class);
        when(wrapper.export(obj, 0)).thenReturn(stub);
        
        registry.start();
        assertEquals(stub, registry.export(obj));
    }
    
    @Test(expected=RemoteException.class)
    public void testExportObjectNotStarted() throws Exception {
        Remote obj = mock(Remote.class);
        registry.export(obj);
    }
    
    @Test
    public void testUnexportObject() throws Exception {
        Remote obj = mock(Remote.class);
        when(wrapper.unexport(obj, true)).thenReturn(true);
        
        registry.start();
        assertEquals(true, registry.unexport(obj));
        verify(wrapper).unexport(obj, true);
    }
    
    @Test(expected=RemoteException.class)
    public void testUnexportObjectNotStarted() throws Exception {
        Remote obj = mock(Remote.class);
        registry.unexport(obj);
    }
}
