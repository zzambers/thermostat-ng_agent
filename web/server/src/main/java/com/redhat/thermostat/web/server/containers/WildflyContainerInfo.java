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

package com.redhat.thermostat.web.server.containers;

class WildflyContainerInfo extends AbstractContainerInfo implements ServletContainerInfo {

    private static final String JBOSS_WEB_PREFIX = "JBoss Web";
    private static final String UNDERTOW_PREFIX = "Undertow";
    private final ContainerVersion version;
    
    WildflyContainerInfo(String serverInfo) {
        this.version = parseVersion(serverInfo);
    }
    
    @Override
    public ContainerName getName() {
        return ContainerName.WILDFLY;
    }

    @Override
    public ContainerVersion getContainerVersion() {
        return version;
    }
    
    @Override
    protected ContainerVersion parseVersion(String serverInfo) {
        super.check(serverInfo);
        if (serverInfo.startsWith(JBOSS_WEB_PREFIX)) {
            return parseJBossWeb(serverInfo);
        } else if (serverInfo.startsWith(UNDERTOW_PREFIX)) {
            return parseUndertow(serverInfo);
        } else {
            // Don't know how to parse
            return null;
        }
    }

    private ContainerVersion parseUndertow(String serverInfo) {
        // Wildfly 8 uses undertow and looks like: Undertow - x.y.z.suffix
        String version = serverInfo.substring(serverInfo.indexOf("-") + 2);
        return parseVersionCommon(version);
    }

    private ContainerVersion parseJBossWeb(String serverInfo) {
        // JBOSS AS 7 server info is of the form: JBoss Web/x.y.z.suffix
        String version = serverInfo.substring(serverInfo.indexOf("/") + 1);
        return parseVersionCommon(version);
    }
    
    private ContainerVersion parseVersionCommon(String version) {
        String[] versionTokens = version.split("\\.");
        int major = Integer.parseInt(versionTokens[0]);
        int minor = Integer.parseInt(versionTokens[1]);
        int micro = Integer.parseInt(versionTokens[2]);
        String suffix = versionTokens[3];
        return new ContainerVersion(major, minor, micro, suffix);
    }

}
