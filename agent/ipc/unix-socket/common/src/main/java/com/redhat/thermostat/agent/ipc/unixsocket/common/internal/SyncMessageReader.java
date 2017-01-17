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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SyncMessageReader extends MessageReader {
    
    private final ByteBuffer readBuffer;
    private final ThermostatLocalSocketChannelImpl channel;
    private ByteBuffer fullMessage;
    
    public SyncMessageReader(ThermostatLocalSocketChannelImpl channel) {
        this(channel, new MessageLimits());
    }
    
    public SyncMessageReader(ThermostatLocalSocketChannelImpl channel, MessageLimits limits) {
        super(limits);
        this.readBuffer = ByteBuffer.allocate(limits.getBufferSize());
        this.channel = channel;
        this.fullMessage = null;
    }

    public ByteBuffer readData() throws IOException {
        fullMessage = null;
        boolean moreData = true;
        while (moreData) {
            doBlockingRead(readBuffer, MIN_HEADER_SIZE);
            processData(readBuffer);
            assertState(ReadState.MIN_HEADER_READ);

            int remainingHeaderSize = currentHeader.getHeaderSize() - MIN_HEADER_SIZE;
            doBlockingRead(readBuffer, remainingHeaderSize);
            processData(readBuffer);
            assertState(ReadState.FULL_HEADER_READ);

            int messageSize = currentHeader.getMessageSize();
            moreData = currentHeader.isMoreData();
            doBlockingRead(readBuffer, messageSize);
            processData(readBuffer);
            assertState(ReadState.NEW_MESSAGE);
        }
        
        if (fullMessage == null) {
            throw new IllegalStateException("No message was read");
        }
        return fullMessage;
    }
    
    private void doBlockingRead(ByteBuffer buf, int expected) throws IOException {
        buf.clear();
        // Use a precise limit to read exact number of bytes
        buf.limit(expected);
        
        while (buf.hasRemaining()) {
            int read = channel.read(buf);
            if (read < 0) {
                throw new IOException("Stream closed unexpectedly while reading message");
            }
        }
        buf.flip();
    }

    @Override
    protected void readFullMessage(ByteBuffer fullMessage) {
        this.fullMessage = fullMessage;
    }
    
    private void assertState(ReadState expected) {
        if (state != expected) {
            throw new IllegalStateException("Expected ReadState \'" + expected.name() 
                + "\', but was \'" + state.name() + "\'");
        }
    }

}
