/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.config;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ClientPreferences {

    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String CONNECTION_URL = "connection-url";

    private final Preferences prefs;

    public ClientPreferences() {
        this(Preferences.userRoot().node("thermostat"));
    }

    ClientPreferences(Preferences prefs) {
        this.prefs = prefs;
    }

    public String getConnectionUrl() {
        return prefs.get(CONNECTION_URL, "mongodb://127.0.0.1:27518");
    }

    public String getPassword() {
        return prefs.get(PASSWORD, "");
    }

    public String getUserName() {
        return prefs.get(USERNAME, "");
    }

    public void setConnectionUrl(String url) {
        prefs.put(CONNECTION_URL, url);
    }

    public void setPassword(String password) {
        prefs.put(PASSWORD, password);
    }
    
    public void setUserName(String username) {
        prefs.put(USERNAME, username);
    }
    
    public void flush() throws BackingStoreException {
        prefs.flush();
    }
}
