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

package com.redhat.thermostat.agent.command.server.internal;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.command.DecodingHelper;
import com.redhat.thermostat.common.command.InvalidMessageException;
import com.redhat.thermostat.common.command.MessageEncoder;
import com.redhat.thermostat.common.command.ParameterDecodingContext;
import com.redhat.thermostat.common.command.ParameterDecodingState;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.StringDecodingContext;
import com.redhat.thermostat.common.command.StringDecodingState;
import com.redhat.thermostat.common.utils.LoggingUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * 
 * See implementation of {@link MessageEncoder} for documentation of the
 * {@link Request} encoding format.
 */
class CommandChannelRequestDecoder extends ByteToMessageDecoder {
    
    private static final Logger logger = LoggingUtils.getLogger(CommandChannelRequestDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        logger.log(Level.FINEST, "Command channel server: decoding Request object");
        StringDecodingContext stringDecCtx = DecodingHelper.decodeString(buf);
        if (stringDecCtx.getState() != StringDecodingState.VALUE_READ) {
            // insufficient data, return
            return;
        }
        String typeAsString = stringDecCtx.getValue();
        if (typeAsString == null) {
            throw new InvalidMessageException("Could not decode message: " + ByteBufUtil.hexDump(buf));
        }
        // clean up resources
        buf.readerIndex(buf.readerIndex() + stringDecCtx.getBytesRead());
        buf.discardReadBytes();
        // Netty javadoc tells us it's safe to downcast to more concrete type.
        InetSocketAddress addr = (InetSocketAddress)ctx.channel().remoteAddress();
        Request request = new Request(RequestType.valueOf(typeAsString), addr);
        ParameterDecodingContext paramCtx = DecodingHelper.decodeParameters(buf);
        if (paramCtx.getState() != ParameterDecodingState.ALL_PARAMETERS_READ) {
            // insufficient data
            return;
        }
        // clean up resources
        buf.readerIndex(buf.readerIndex() + paramCtx.getBytesRead());
        buf.discardReadBytes();
        for (Entry<String, String> kv: paramCtx.getValues().entrySet()) {
            request.setParameter(kv.getKey(), kv.getValue());
        }
        out.add(request);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.FINEST, "Channel active!");
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.log(Level.WARNING, "Exception caught", cause);
        ctx.close();
    }

}

