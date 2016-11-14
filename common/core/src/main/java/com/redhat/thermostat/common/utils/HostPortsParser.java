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

package com.redhat.thermostat.common.utils;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.internal.LocaleResources;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;

/**
 * Parses Host/Port pairs from a raw string.
 * 
 * <pre>
 * IPv4:
 *      127.0.0.1:9999,127.0.0.2:8888
 * </pre>
 * 
 * or
 * 
 * <pre>
 * IPv6:
 *      [1fff:0:a88:85a3::ac1f]:8001,[1fff:0:a88:85a3::ac2f]:8001
 * </pre>
 * 
 * or
 * 
 * <pre>
 * DNS hostname:port pairs:
 *      testhost.example.com:8970,host2.example.com:1234
 * </pre>
 * 
 * Be sure to call <code>{@link #parse()}</code> before getting the list of
 * host/port pairs via <code>{@link #getHostsPorts()}</code>.
 */
public class HostPortsParser {

    private final String rawString;
    private List<HostPortPair> ipPorts;
    private final InvalidConfigurationException formatException; 
    private final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    public HostPortsParser(String parseString) {
        this.rawString = parseString;
        this.formatException = new InvalidConfigurationException(t.localize(LocaleResources.INVALID_IPPORT, rawString));
    }
    
    public void parse() throws InvalidConfigurationException {
        ipPorts = new ArrayList<>();
        for (String ipPortPair: rawString.split(",")) {
            // if we have a '[' in the ip:port pair string we likely have an IPv6
            int idxRparen = ipPortPair.indexOf(']');
            int idxLParen = ipPortPair.indexOf('[');
            if (idxLParen == -1) {
                // IPv4
                if (idxRparen != -1 || ipPortPair.indexOf(':') == -1) {
                   throw formatException; 
                }
                String[] ipPort = ipPortPair.split(":");
                int port = -1;
                try {
                    port = Integer.parseInt(ipPort[1]);
                } catch (NumberFormatException e) {
                    throw formatException;
                }
                ipPorts.add(new HostPortPair(ipPort[0], port));
            } else {
                // IPv6
                if (idxRparen == -1) {
                    throw formatException;
                }
                int port = -1;
                try {
                    port = Integer.parseInt(ipPortPair.substring(idxRparen + 2));
                } catch (NumberFormatException e) {
                    throw formatException;
                }
                ipPorts.add(new HostPortPair(ipPortPair.substring(idxLParen + 1, idxRparen), port, true));
            }
        }
    }
    
    public List<HostPortPair> getHostsPorts() {
        if (ipPorts == null) {
            throw new IllegalStateException("Must call parse() before getting map!");
        }
        return ipPorts;
    }
}

