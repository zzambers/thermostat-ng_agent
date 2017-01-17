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

package com.redhat.thermostat.agent.ipc.tcpsocket.server.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ThermostatSocketChannelImpl;

class AcceptedSocketChannelImpl extends ThermostatSocketChannelImpl {

    private SelectionKey key;
    private SocketHelper helper;
    
    AcceptedSocketChannelImpl(String name, SocketChannel impl, SelectionKey key) {
        this(name, impl, key, new SocketHelper());
    }
    
    AcceptedSocketChannelImpl(String name, SocketChannel impl, SelectionKey key, SocketHelper helper) {
        super(name, impl);
        this.key = key;
        this.helper = helper;
    }
    
    private void unregister() {
        if (key != null) {
            key.cancel();
            key = null;
        }
    }
    
    @Override
    public void close() throws IOException {
        unregister();
        helper.closeSocket(this);
    }
    
    SelectionKey getSelectionKey() {
        return key;
    }

    // For use by SocketHelper
    private void implClose() throws IOException {
        super.close();
    }
    
    // For testing purposes, methods that can't be mocked
    static class SocketHelper {
        void closeSocket(AcceptedSocketChannelImpl channel) throws IOException {
            channel.implClose();
        }
    }

}
