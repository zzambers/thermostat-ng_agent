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

package com.redhat.thermostat.common.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Test;

public class ClientPreferencesTest {

    @Test
    public void testGetConnectionUrl() {
        Properties prefs = mock(Properties.class);
        when(prefs.getProperty(eq(ClientPreferences.CONNECTION_URL), any(String.class))).thenReturn("mock-value");
        when(prefs.getProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(String.class))).thenReturn("true");

        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        String value = clientPrefs.getConnectionUrl();

        assertEquals("mock-value", value);
        verify(prefs).getProperty(eq(ClientPreferences.CONNECTION_URL), any(String.class));
    }

    @Test
    public void testSetConnectionUrl() {
        Properties prefs = mock(Properties.class);
        when(prefs.getProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), eq("false"))).thenReturn("true");

        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        clientPrefs.setConnectionUrl("test");

        verify(prefs).put(eq(ClientPreferences.CONNECTION_URL), eq("test"));
    }

    @Test
    public void testSetUsername() {
        Properties prefs = mock(Properties.class);
        when(prefs.getProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(String.class))).thenReturn("true");
        
        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        clientPrefs.setUserName("fluff");

        verify(prefs).put(eq(ClientPreferences.USERNAME), eq("fluff"));
    }
    
    @Test
    public void testGetUsername() {
        Properties prefs = mock(Properties.class);
        when(prefs.getProperty(eq(ClientPreferences.CONNECTION_URL), any(String.class))).thenReturn("mock-url");
        when(prefs.getProperty(eq(ClientPreferences.USERNAME), any(String.class))).thenReturn("mock-value");
        
        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        String username = clientPrefs.getUserName();

        assertEquals("", username);

        when(prefs.getProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(String.class))).thenReturn("true");
        username = clientPrefs.getUserName();
        assertEquals("mock-value", username);
        verify(prefs, atLeastOnce()).getProperty(eq(ClientPreferences.USERNAME), any(String.class));
    }
    
    @Test
    public void testGetSaveEntitlements() {
        Properties prefs = mock(Properties.class);
        when(prefs.getProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(String.class))).thenReturn("true");
        
        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        boolean saveEntitlements = clientPrefs.getSaveEntitlements();

        assertEquals(true, saveEntitlements);
        verify(prefs, atLeastOnce()).getProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), any(String.class));
    }
    
    @Test
    public void testSetSaveEntitlements() {
        Properties prefs = mock(Properties.class);

        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        clientPrefs.setSaveEntitlements(false);

        verify(prefs).setProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), eq("false"));
        
        clientPrefs.setSaveEntitlements(true);
        verify(prefs).setProperty(eq(ClientPreferences.SAVE_ENTITLEMENTS), eq("true"));
    }
    
    @Test
    public void testDefaultPreferencesIsHTTP() {
        Properties prefs = mock(Properties.class);
        when(prefs.getProperty(eq("connection-url"), any(String.class))).thenReturn(ClientPreferences.DEFAULT_CONNECTION_URL);
        ClientPreferences clientPrefs = new ClientPreferences(prefs);
        assertEquals("http://127.0.0.1:8999/thermostat/storage", clientPrefs.getConnectionUrl());
    }
}

