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

package com.redhat.thermostat.bundles.impl;

import com.redhat.thermostat.bundles.OSGiRegistryService;
import com.redhat.thermostat.common.Configuration;
import com.redhat.thermostat.common.ConfigurationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;

public class OSGiRegistry extends OSGiRegistryService {

    private Collection<String> loadedBundles;
    Configuration configuration;
    private String thermostatHome;
    private BundleProperties bundleProperties;
    private BundleLoader loader;

    OSGiRegistry(Configuration configuration) throws ConfigurationException, FileNotFoundException, IOException {
        
        loadedBundles = new HashSet<>(preLoadedBundles);
        thermostatHome = configuration.getThermostatHome();
        bundleProperties = new BundleProperties(thermostatHome);
        this.configuration = configuration;
        loader = new BundleLoader();
    }

    @Override
    public void setPrintOSGiInfo(boolean printOSGiInfo) {
        configuration.setPrintOSGiInfo(printOSGiInfo);
        loader.setPrintOSGiInfo(printOSGiInfo);
    }

    @Override
    public void addBundlesFor(String commandName) throws BundleException, IOException {
        Framework framework = getFramework(this.getClass());
        if (configuration.getPrintOSGiInfo()) {
            System.out.println("Loading additional bundles for: " + commandName);
        }
        List<String> requiredBundles = bundleProperties.getDependencyResourceNamesFor(commandName);
        List<String> bundlesToLoad = new ArrayList<>();
        for (String resource : requiredBundles) {
            if (!loadedBundles.contains(resource)) {
                bundlesToLoad.add(resource);
            }
        }
        List<Bundle> successBundles = loader.installAndStartBundles(framework, bundlesToLoad);
        for (Bundle bundle : successBundles) {
            loadedBundles.add(bundle.getLocation());
        }
    }

    private Framework getFramework(Class<?> cls) {
        return (Framework) FrameworkUtil.getBundle(cls).getBundleContext().getBundle(0);
    }
}
