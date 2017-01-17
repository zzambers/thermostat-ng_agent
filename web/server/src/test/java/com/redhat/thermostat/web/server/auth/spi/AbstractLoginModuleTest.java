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

package com.redhat.thermostat.web.server.auth.spi;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.junit.Test;

public class AbstractLoginModuleTest {

    @Test
    public void canGetUserNameFromCallBack() throws LoginException {
        SimpleCallBackHandler handler = new SimpleCallBackHandler("testuser");
        LoginModuleImpl loginModule = new LoginModuleImpl();
        loginModule.initialize(new Subject(), handler, null, null);
        assertEquals("testuser", loginModule.getUsernameFromCallBack());
    }
    
    @Test
    public void canGetUserPasswordFromCallBack() throws LoginException {
        SimpleCallBackHandler handler = new SimpleCallBackHandler("testuser", "testpassword".toCharArray());
        LoginModuleImpl loginModule = new LoginModuleImpl();
        loginModule.initialize(new Subject(), handler, null, null);
        Object[] creds = loginModule.getUsernamePasswordFromCallBack();
        String user = (String)creds[0];
        char[] pwd = (char[])creds[1];
        assertEquals("testuser", user);
        assertEquals("testpassword", new String(pwd));
    }
    
    private static class LoginModuleImpl extends AbstractLoginModule {

        @Override
        public void initialize(Subject subject,
                CallbackHandler callbackHandler, Map<String, ?> sharedState,
                Map<String, ?> options) {
            this.callBackHandler = callbackHandler;
        }

        @Override
        public boolean login() throws LoginException {
            // don't care
            return false;
        }

        @Override
        public boolean commit() throws LoginException {
            // don't care
            return false;
        }

        @Override
        public boolean abort() throws LoginException {
            // don't care
            return false;
        }

        @Override
        public boolean logout() throws LoginException {
            // don't care
            return false;
        }
        
        @Override
        public String getUsernameFromCallBack() throws LoginException {
            return super.getUsernameFromCallBack();
        }
        
        @Override
        public Object[] getUsernamePasswordFromCallBack() throws LoginException {
            return super.getUsernamePasswordFromCallBack();
        }
        
    }
}

