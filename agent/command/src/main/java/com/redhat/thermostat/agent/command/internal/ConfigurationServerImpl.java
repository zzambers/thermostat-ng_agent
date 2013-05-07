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

package com.redhat.thermostat.agent.command.internal;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;

import com.redhat.thermostat.agent.command.ConfigurationServer;
import com.redhat.thermostat.common.utils.LoggingUtils;

class ConfigurationServerImpl implements ConfigurationServer {

    private final ConfigurationServerContext ctx;
    private static final Logger logger = LoggingUtils.getLogger(ConfigurationServerImpl.class);

    ConfigurationServerImpl(ConfigurationServerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void startListening(String hostname, int port) {
        ServerBootstrap bootstrap = (ServerBootstrap) ctx.getBootstrap();

        InetSocketAddress addr = new InetSocketAddress(hostname, port);
        
        logger.log(Level.FINE, "Starting command channel server on " + addr.toString());
        try {
            // Bind and start to accept incoming connections.
            bootstrap.bind(addr);
        } catch (ChannelException e) {
            logger.log(Level.SEVERE, "Failed to bind command channel server!", e);
            // rethrow, in order to stop agent from continuing
            throw e;
        }
        logger.log(Level.FINEST, "Bound command channel server to " + addr.toString());
    }

    @Override
    public void stopListening() {
        logger.log(Level.FINE, "Stopping command channel server");
        ctx.getChannelGroup().close().awaitUninterruptibly();
        ctx.getChannelGroup().clear();
        ctx.getBootstrap().releaseExternalResources();
    }
}

