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

package com.redhat.thermostat.web.endpoint.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class DelegatingClassLoaderTest {

    @Test
    public void verifyClassloaderDelegationFailsForNotExistingClass() throws Exception {
        ClassLoader mockLoader = mock(ClassLoader.class);
        DelegatingClassLoader delegateLoader = new DelegatingClassLoader(mockLoader);
        String notExistingClassName = "com.redhat.thermostat.not.existing.FooClass";
        doThrow(ClassNotFoundException.class).when(mockLoader).loadClass(eq(notExistingClassName));
        try {
            delegateLoader.loadClass(notExistingClassName);
            fail("should not have found class " + notExistingClassName);
        } catch (ClassNotFoundException e) {
            // pass
            verify(mockLoader).loadClass(eq(notExistingClassName));
        }
    }
    
    @Test
    public void verifyClassloaderDelegationWorksForExistingClass() throws Exception {
        ClassLoader mockLoader = mock(ClassLoader.class);
        DelegatingClassLoader webappLoader = new DelegatingClassLoader(mockLoader);
        // This class is not really there, but the fake class loader does not
        // throw a CNFE
        String className = "com.redhat.thermostat.not.existing.FooClass";
        try {
            Class<?> clazz = webappLoader.loadClass(className);
            assertNull(clazz);
        } catch (ClassNotFoundException e) {
            fail("should have been able to load class since mock classloader does not throw CNFE");
        }
        verify(mockLoader).loadClass(eq(className));
    }
    
    @Test
    public void verifySystemClassIsResolvable() throws Exception {
    	ClassLoader mockLoader = mock(ClassLoader.class);
    	DelegatingClassLoader delegateLoader = new DelegatingClassLoader(mockLoader);
    	String className = "javax.security.auth.login.LoginContext";
    	doThrow(ClassNotFoundException.class).when(mockLoader).loadClass(eq(className));
    	try {
    		Class<?> clazz = delegateLoader.loadClass(className);
    		assertNotNull(clazz);
    	} catch (ClassNotFoundException e) {
    		fail("should have been able to load system class " + className);
    	}
    }
    
}
