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

package com.redhat.thermostat.client.command.internal;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.ssl.SslHandler;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Listener registered for SSL handshakes.
 * 
 * @see RequestQueueImpl
 *
 */
final class SSLHandshakeFinishedListener implements ChannelFutureListener {
    
    private final Request request;
    private final boolean performHostNameChecking;
    private final SslHandler handler;
    private final RequestQueueImpl queueRunner;
    private final Logger logger = LoggingUtils.getLogger(SSLHandshakeFinishedListener.class);
    
    SSLHandshakeFinishedListener(Request request, boolean performHostNameVerification,
            SslHandler handler, RequestQueueImpl runner) {
        this.request = request;
        this.performHostNameChecking = performHostNameVerification;
        this.handler = handler;
        this.queueRunner = runner;
    }
    
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            logger.log(Level.WARNING,
                    "SSL handshake failed check agent logs for details!",
                    future.getCause());
            queueRunner.fireComplete(request, new Response(ResponseType.ERROR));
        } else {
            if (performHostNameChecking) {
                try {
                    doHostnameVerification();
                } catch (Exception e) {
                    future.setFailure(e);
                    future.removeListener(this);
                    future.getChannel().close();
                    logger.log(Level.SEVERE, "Hostname verification failed!", e);
                    queueRunner.fireComplete(request, new Response(ResponseType.ERROR));
                }
            }
        }
    }
    
    private void doHostnameVerification() throws SSLException {
        SSLSession session = handler.getEngine().getSession();
        // First certificate is the peer. We only need this one for host name
        // verification.
        X509Certificate cert = (X509Certificate)session.getPeerCertificates()[0];
        // We use httpcomponents httpclient's hostname verifier since this one
        // is well tested and supports domain wild-cards in certs and all these
        // goodies.
        BrowserCompatHostnameVerifier hostnameVerifier = new BrowserCompatHostnameVerifier();
        InetSocketAddress addr = request.getTarget();
        // Use getHostString in order to avoid reverse lookup.
        // verify() throws SSLException if we fail to verify
        hostnameVerifier.verify(addr.getHostString(), cert);
    }
}

