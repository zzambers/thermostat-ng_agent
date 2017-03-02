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

package com.redhat.thermostat.agent.ipc.unixsocket.client.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;

import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.agent.ipc.client.internal.ClientTransport;
import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.ThermostatLocalSocketChannelImpl;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCProperties;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UserPrincipalUtils;

public class UnixSocketTransportImpl implements ClientTransport {
    
    private final UnixSocketIPCProperties socketProps;
    private final SocketHelper sockHelper;
    private final UserPrincipalUtils userUtils;
    
    UnixSocketTransportImpl(IPCProperties props) throws IOException {
        this(props, new SocketHelper(), new UserPrincipalUtils());
    }
    
    UnixSocketTransportImpl(IPCProperties props, SocketHelper sockHelper, 
            UserPrincipalUtils userUtils) throws IOException {
        if (!(props instanceof UnixSocketIPCProperties)) {
            throw new IOException("Unexpected IPC properties for 'socket' type");
        }
        this.socketProps = (UnixSocketIPCProperties) props;
        this.sockHelper = sockHelper;
        this.userUtils = userUtils;
    }
    
    @Override
    public IPCMessageChannel connect(String serverName) throws IOException {
        File socketFile = verifySocketFile(serverName);
        ThermostatLocalSocketChannelImpl sockChannel = sockHelper.openSocketChannel(serverName, socketFile);
        return sockHelper.createMessageChannel(sockChannel);
    }
    
    private File verifySocketFile(String name) throws IOException {
        requireNonNull(name, "Server name cannot be null");
        File socketDir = socketProps.getSocketDirectory();
        if (!socketDir.exists()) {
            throw new IOException("Server address is invalid");
        }
        
        // Get subdirectory for current user
        UserPrincipal currentUser = userUtils.getCurrentUser();
        String username = currentUser.getName();
        File socketFile = socketProps.getSocketFile(name, username);
        if (!socketFile.exists()) {
            throw new IOException("IPC server with name \"" + name + "\" does not exist");
        }
        return socketFile;
    }
    
    // java.lang.Objects is JDK 7+ and we need this to be JDK 6 compat.
    private static void requireNonNull(Object item, String message) {
        if (item == null) {
            throw new NullPointerException(message);
        }
    }
    
    // Helper class for testing
    static class SocketHelper {
        ThermostatLocalSocketChannelImpl openSocketChannel(String name, File socketFile) throws IOException {
            return ThermostatLocalSocketChannelImpl.open(name, socketFile);
        }
        
        UnixSocketMessageChannel createMessageChannel(ThermostatLocalSocketChannelImpl sockChannel) {
            return new UnixSocketMessageChannel(sockChannel);
        }
        
        File getFile(File socketDir, String name) {
            return new File(socketDir, name);
        }
    }

}
