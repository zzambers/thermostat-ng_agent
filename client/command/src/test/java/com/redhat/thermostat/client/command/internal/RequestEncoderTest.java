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

package com.redhat.thermostat.client.command.internal;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;

public class RequestEncoderTest {

    private static final boolean DEBUG = false;
    
    @Test
    public void canEncodeSimpleRequestWithNoParams() throws Exception {
        RequestEncoder encoder = new RequestEncoder();
        String responseExp = "RESPONSE_EXPECTED";
        ChannelBuffer stringBuf = ChannelBuffers.copiedBuffer(responseExp, Charset.defaultCharset());
        ChannelBuffer buf = ChannelBuffers.buffer(4);
        buf.writeInt(responseExp.getBytes().length);
        ChannelBuffer buf2 = ChannelBuffers.wrappedBuffer(buf, stringBuf);
        buf = ChannelBuffers.buffer(4);
        buf.writeInt(0);
        ChannelBuffer expected = ChannelBuffers.wrappedBuffer(buf2, buf);
        InetSocketAddress addr = new InetSocketAddress("testhost", 12);
        Request item = new Request(RequestType.RESPONSE_EXPECTED, addr);
        ChannelBuffer actual = encoder.encode(item);
        if (DEBUG) {
            printBuffers(actual, expected);
        }
        assertEquals(0, ChannelBuffers.compare(expected, actual));
    }
    
    @Test
    public void canEncodeRequestWithParams() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(1234);

        // Prepare request we'd like to encode
        Request item = new Request(RequestType.RESPONSE_EXPECTED, addr);
        String param1Name = "param1";
        String param1Value = "value1";
        String param2Name = "param2";
        String param2Value = "value2";
        item.setParameter(param1Name, param1Value);
        item.setParameter(param2Name, param2Value);
        RequestEncoder encoder = new RequestEncoder();
        
        // build expected
        String responseExp = "RESPONSE_EXPECTED";
        ChannelBuffer stringBuf = ChannelBuffers.copiedBuffer(responseExp, Charset.defaultCharset());
        ChannelBuffer buf = ChannelBuffers.buffer(4);
        buf.writeInt(responseExp.getBytes().length);
        ChannelBuffer buf2 = ChannelBuffers.wrappedBuffer(buf, stringBuf);
        buf = ChannelBuffers.buffer(4);
        buf.writeInt(2);
        ChannelBuffer request = ChannelBuffers.wrappedBuffer(buf2, buf);
        ChannelBuffer nameLen = ChannelBuffers.buffer(4);
        nameLen.writeInt(param1Name.getBytes().length);
        ChannelBuffer valueLen = ChannelBuffers.buffer(4);
        valueLen.writeInt(param1Value.getBytes().length);
        ChannelBuffer lens = ChannelBuffers.wrappedBuffer(nameLen, valueLen);
        ChannelBuffer nameBuf = ChannelBuffers.copiedBuffer(param1Name, Charset.defaultCharset());
        ChannelBuffer valueBuf = ChannelBuffers.copiedBuffer(param1Value, Charset.defaultCharset());
        ChannelBuffer payload = ChannelBuffers.wrappedBuffer(nameBuf, valueBuf);
        ChannelBuffer param1Buf = ChannelBuffers.wrappedBuffer(lens, payload);
        nameLen = ChannelBuffers.buffer(4);
        nameLen.writeInt(param2Name.getBytes().length);
        valueLen = ChannelBuffers.buffer(4);
        valueLen.writeInt(param2Value.getBytes().length);
        lens = ChannelBuffers.wrappedBuffer(nameLen, valueLen);
        nameBuf = ChannelBuffers.copiedBuffer(param2Name, Charset.defaultCharset());
        valueBuf = ChannelBuffers.copiedBuffer(param2Value, Charset.defaultCharset());
        payload = ChannelBuffers.wrappedBuffer(nameBuf, valueBuf);
        ChannelBuffer param2Buf = ChannelBuffers.wrappedBuffer(lens, payload);
        ChannelBuffer params = ChannelBuffers.wrappedBuffer(param1Buf, param2Buf);
        ChannelBuffer expected = ChannelBuffers.wrappedBuffer(request, params);
        
        // Encode item for actual
        ChannelBuffer actual = encoder.encode(item);
        if (DEBUG) {
            printBuffers(actual, expected);
        }
        assertEquals(0, ChannelBuffers.compare(expected, actual));
    }

    private void printBuffers(ChannelBuffer actual, ChannelBuffer expected) {
        System.out.println("hexdump expected\n-------------------------------------");
        System.out.println(ChannelBuffers.hexDump(expected));
        System.out.println("\nhexdump actual\n-------------------------------------");
        System.out.println(ChannelBuffers.hexDump(actual) + "\n\n");
    }
}

