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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.AsyncMessageReader;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.AsyncMessageWriter;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.MessageListener;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.ClientHandler.MessageCreator;

public class ClientHandlerTest {
    
    private AcceptedLocalSocketChannelImpl client;
    private ThermostatIPCCallbacks callbacks;
    private SelectionKey key;
    private Selector selector;
    private AsyncMessageReader reader;
    private AsyncMessageWriter writer;
    private ExecutorService execService;
    private MessageCreator messageCreator;
    private MessageImpl message;
    private ClientHandler handler;
    
    @Before
    public void setup() throws Exception {
        client = mock(AcceptedLocalSocketChannelImpl.class);
        
        key = mock(SelectionKey.class);
        selector = mock(Selector.class);
        when(key.selector()).thenReturn(selector);
        when(client.getSelectionKey()).thenReturn(key);
        
        callbacks = mock(ThermostatIPCCallbacks.class);
        reader = mock(AsyncMessageReader.class);
        writer = mock(AsyncMessageWriter.class);
        execService = mock(ExecutorService.class);
        messageCreator = mock(MessageCreator.class);
        message = mock(MessageImpl.class);
        when(messageCreator.createMessage(any(ByteBuffer.class), any(MessageListener.class))).thenReturn(message);
        handler = new ClientHandler(client, execService, callbacks, reader, writer, messageCreator);
    }
    
    @Test
    public void testRead() throws Exception {
        handler.handleRead();
        verify(reader).readData();
        
        // Should not close connection
        verify(client, never()).close();
    }

    @Test
    public void testReadException() throws Exception {
        doThrow(new IOException()).when(reader).readData();
        
        try {
            handler.handleRead();
            fail("Expected IOException");
        } catch (IOException e) {
            // Should close channel
            verify(reader).readData();
            verify(client).close();
        }
    }
    
    @Test
    public void testWrite() throws Exception {
        final int ops = 8000;
        when(key.interestOps()).thenReturn(ops);
        handler.handleWrite();
        verify(writer).writeData();
        
        // Check write removed from interest set
        verify(key).interestOps(ops & ~SelectionKey.OP_WRITE);
        
        // Should not close connection
        verify(client, never()).close();
    }
    
    @Test
    public void testWriteMoreMessages() throws Exception {
        when(writer.hasMoreMessages()).thenReturn(true);
        handler.handleWrite();
        verify(writer).writeData();
        
        // Check write not removed from interest set
        verify(key, never()).interestOps(anyInt());
        
        // Should not close connection
        verify(client, never()).close();
    }

    @Test
    public void testWriteException() throws Exception {
        doThrow(new IOException()).when(writer).writeData();
        
        try {
            handler.handleWrite();
            fail("Expected IOException");
        } catch (IOException e) {
            // Should close channel
            verify(writer).writeData();
            verify(client).close();
        }
    }
    
    @Test
    public void testMessageRead() throws Exception {
        ByteBuffer buf = mock(ByteBuffer.class);
        handler.messageRead(buf);
        verify(messageCreator).createMessage(buf, handler);
        
        // Check callback notified
        ArgumentCaptor<Runnable> runCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(execService).submit(runCaptor.capture());
        Runnable runnable = runCaptor.getValue();
        runnable.run();
        verify(callbacks).messageReceived(message);
    }
    
    @Test
    public void testWriteMessage() throws Exception {
        final int ops = 8000;
        when(key.interestOps()).thenReturn(ops);
        ByteBuffer buf = mock(ByteBuffer.class);
        handler.writeMessage(buf);
        
        // Check write added to interest set
        verify(key).interestOps(ops | SelectionKey.OP_WRITE);
        
        verify(writer).enqueueForWriting(buf);
        verify(selector).wakeup();
    }
    
}
