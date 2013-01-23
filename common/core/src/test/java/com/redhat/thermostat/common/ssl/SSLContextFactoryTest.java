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

package com.redhat.thermostat.common.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ SSLKeystoreConfiguration.class, SSLContext.class,
        KeyManagerFactory.class })
public class SSLContextFactoryTest {

    /*
     * cmdChanServer.keystore is a keystore converted from openssl. It contains
     * key material which was signed by ca.crt. More information as to how to
     * create such a file here (first create server.crt => convert it to java
     * keystore format):
     * http://icedtea.classpath.org/wiki/Thermostat/DevDeployWarInTomcatNotes
     * 
     * Unfortunately, powermock messes up the KeyManagerFactory. We can only
     * verify that proper methods are called.
     */
    @Test
    public void verifySetsUpServerContextWithProperKeyMaterial()
            throws Exception {
        File keystoreFile = new File(this.getClass()
                .getResource("/cmdChanServer.keystore").getFile());

        PowerMockito.mockStatic(SSLKeystoreConfiguration.class);
        when(SSLKeystoreConfiguration.getKeystoreFile()).thenReturn(
                keystoreFile);
        when(SSLKeystoreConfiguration.getKeyStorePassword()).thenReturn(
                "testpassword");

        PowerMockito.mockStatic(SSLContext.class);
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContext.getInstance("TLS")).thenReturn(context);
        KeyManagerFactory factory = PowerMockito.mock(KeyManagerFactory.class);
        PowerMockito.mockStatic(KeyManagerFactory.class);
        when(KeyManagerFactory.getInstance("SunX509")).thenReturn(factory);

        ArgumentCaptor<KeyStore> keystoreArgCaptor = ArgumentCaptor
                .forClass(KeyStore.class);
        ArgumentCaptor<char[]> pwdCaptor = ArgumentCaptor
                .forClass(char[].class);
        SSLContextFactory.getServerContext();
        verify(context).init(any(KeyManager[].class),
                any(TrustManager[].class), any(SecureRandom.class));
        verify(factory).init(keystoreArgCaptor.capture(), pwdCaptor.capture());
        verify(factory).getKeyManagers();

        KeyStore keystore = keystoreArgCaptor.getValue();
        String password = new String(pwdCaptor.getValue());
        assertNotNull(keystore);
        assertNotNull(password);
        assertEquals("testpassword", password);
    }

    @Test
    public void verifySetsUpClientContextWithProperTrustManager()
            throws Exception {
        File keystoreFile = new File(this.getClass()
                .getResource("/cmdChanServer.keystore").getFile());

        PowerMockito.mockStatic(SSLKeystoreConfiguration.class);
        when(SSLKeystoreConfiguration.getKeystoreFile()).thenReturn(
                keystoreFile);
        when(SSLKeystoreConfiguration.getKeyStorePassword()).thenReturn(
                "testpassword");

        PowerMockito.mockStatic(SSLContext.class);
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContext.getInstance("TLS")).thenReturn(context);

        ArgumentCaptor<TrustManager[]> tmsCaptor = ArgumentCaptor
                .forClass(TrustManager[].class);
        SSLContextFactory.getClientContext();
        verify(context).init(any(KeyManager[].class), tmsCaptor.capture(),
                any(SecureRandom.class));
        TrustManager[] tms = tmsCaptor.getValue();
        assertEquals(1, tms.length);
        assertEquals(tms[0].getClass().getName(),
                "com.redhat.thermostat.common.internal.CustomX509TrustManager");
    }
}

