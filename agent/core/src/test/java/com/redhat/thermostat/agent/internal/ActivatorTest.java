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

package com.redhat.thermostat.agent.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;

import com.redhat.thermostat.agent.RMIRegistry;
import com.redhat.thermostat.agent.VmBlacklist;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.NativeLibraryResolver;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolImpl;
import com.redhat.thermostat.utils.username.internal.UserNameUtilImpl;

public class ActivatorTest {
    @Test
    public void verifyServiceIsRegistered() throws Exception {

    	CommonPaths paths = mock(CommonPaths.class);
    	when(paths.getSystemNativeLibsRoot()).thenReturn(new File("target"));
        when(paths.getUserAgentAuthConfigFile()).thenReturn(new File("not.exist.does.not.matter"));
    	NativeLibraryResolver.setCommonPaths(paths);

        StubBundleContext context = new StubBundleContext();
        context.registerService(CommonPaths.class.getName(), paths, null);

        //RMIRegistry registry = mock(RMIRegistry.class);
        //MXBeanConnectionPoolImpl pool = new MXBeanConnectionPoolImpl(registry);
        Activator activator = new Activator();

        activator.start(context);

        assertTrue(context.isServiceRegistered(RMIRegistry.class.getName(), RMIRegistryImpl.class));
        assertTrue(context.isServiceRegistered(MXBeanConnectionPool.class.getName(), MXBeanConnectionPoolImpl.class));
        assertTrue(context.isServiceRegistered(UserNameUtil.class.getName(), UserNameUtilImpl.class));
        assertTrue(context.isServiceRegistered(VmBlacklist.class.getName(), VmBlacklistImpl.class));
    }
    
    @Test
    public void verifyPoolShutdown() throws Exception {

        StubBundleContext context = new StubBundleContext();

        MXBeanConnectionPoolImpl pool = mock(MXBeanConnectionPoolImpl.class);
        Activator activator = new Activator();
        activator.setPool(pool);

        activator.stop(context);

        verify(pool).shutdown();
    }
}
