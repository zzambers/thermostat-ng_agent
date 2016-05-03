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

package com.redhat.thermostat.launcher.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.launcher.internal.BundleLoader;

public class BundleLoaderTest {

    @Test
    public void verifyBundlesAreInstalledAndStarted() throws BundleException {
        final String BUNDLE_LOCATION = "bundle-location-1";

        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(new Hashtable<String, String>());
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.installBundle(BUNDLE_LOCATION)).thenReturn(bundle);
        Framework framework = mock(Framework.class);
        when(framework.getBundleContext()).thenReturn(bundleContext);
        List<String> bundleLocations = Arrays.asList(BUNDLE_LOCATION);

        BundleLoader loader = new BundleLoader();
        loader.installAndStartBundles(framework, bundleLocations);

        verify(bundle).start(Bundle.START_TRANSIENT);
    }

    @Bug(id="1227",
         summary="Make sure launcher does not start fragments",
         url="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1227")
    @Test
    public void verifyFragmentsAreInstalledButNotStarted() throws BundleException {
        final String BUNDLE_LOCATION = "bundle-location-1";

        Bundle bundle = mock(Bundle.class);
        Dictionary<String, String> bundleHeaders = new Hashtable<>();
        bundleHeaders.put(Constants.FRAGMENT_HOST, "foo-bar");
        when(bundle.getHeaders()).thenReturn(bundleHeaders);

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.installBundle(BUNDLE_LOCATION)).thenReturn(bundle);
        Framework framework = mock(Framework.class);
        when(framework.getBundleContext()).thenReturn(bundleContext);
        List<String> bundleLocations = Arrays.asList(BUNDLE_LOCATION);

        BundleLoader loader = new BundleLoader();
        loader.installAndStartBundles(framework, bundleLocations);

        verify(bundle, times(0)).start(Bundle.START_TRANSIENT);

    }
}

