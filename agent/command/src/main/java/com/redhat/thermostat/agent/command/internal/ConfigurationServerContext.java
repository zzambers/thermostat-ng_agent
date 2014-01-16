/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.common.command.ConfigurationCommandContext;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.common.ssl.SslInitException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;

class ConfigurationServerContext implements ConfigurationCommandContext {

    private static final Logger logger = LoggingUtils.getLogger(ConfigurationServerContext.class);
    
    private final ServerBootstrap bootstrap;
    private final ChannelGroup channels;
    private final BundleContext context;
    private final SSLConfiguration sslConf;

    ConfigurationServerContext(BundleContext context, SSLConfiguration sslConf) {
        bootstrap = createBootstrap();
        channels = createChannelGroup();
        this.context = context;
        this.sslConf = sslConf;
    }

    @Override
    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    public SSLConfiguration getSSLConfiguration() {
        return sslConf;
    }

    private ChannelGroup createChannelGroup() {
        return new DefaultChannelGroup(ConfigurationServerImpl.class.getName());
    }

    private ServerBootstrap createBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(new ServerPipelineFactory());

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.reuseAddress", true);
        bootstrap.setOption("child.connectTimeoutMillis", 100);
        bootstrap.setOption("child.readWriteFair", true);
        
        return bootstrap;
    }

    private class ServerPipelineFactory implements ChannelPipelineFactory {

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = Channels.pipeline();
            if (sslConf.enableForCmdChannel()) {
                SSLEngine engine = null;
                try {
                    SSLContext ctxt = SSLContextFactory.getServerContext(sslConf);
                    engine = ctxt.createSSLEngine();
                    engine.setUseClientMode(false);
                } catch (SslInitException | InvalidConfigurationException e) {
                    logger.log(Level.SEVERE,
                            "Failed to initiate command channel endpoint", e);
                }
                pipeline.addLast("ssl", new SslHandler(engine));
                logger.log(Level.FINE, "Added SSL handler for command channel endpoint");
            }
            pipeline.addLast("decoder", new RequestDecoder());
            pipeline.addLast("encoder", new ResponseEncoder());
            pipeline.addLast("handler", new ServerHandler(new ReceiverRegistry(context), sslConf));
            return pipeline;
        }
        
    }

    ChannelGroup getChannelGroup() {
        return channels;
    }

}

