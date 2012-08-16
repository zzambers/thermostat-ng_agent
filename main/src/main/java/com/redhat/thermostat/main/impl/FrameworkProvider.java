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

package com.redhat.thermostat.main.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.bundles.OSGiRegistryService;
import com.redhat.thermostat.common.Configuration;
import com.redhat.thermostat.common.ConfigurationException;
import com.redhat.thermostat.launcher.Launcher;

public class FrameworkProvider {

    private static final String DEBUG_PREFIX = "FrameworkProvider: ";
    private static final String PROPS_FILE = "/com/redhat/thermostat/main/impl/bootstrapbundles.properties";
    private static final String BUNDLELIST = "bundles";

    private boolean printOSGiInfo;
    private String thermostatHome;

    public FrameworkProvider(Configuration config) {
        printOSGiInfo = config.getPrintOSGiInfo();
        try {
            this.thermostatHome = config.getThermostatHome();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Cannot initialize frameworks without valid ThermostatHome.", e);
        }
    }

    // This is our ticket into OSGi land. Unfortunately, we to use a bit of reflection here.
    // The launcher and bundleloader are instantiated from within their bundles, ie loaded
    // by the bundle classloader.
    public void startFramework(String[] args) {
        try {
            Framework framework = makeFramework();
            prepareFramework(framework);
            loadBootstrapBundles(framework);
            setLoaderVerbosity(framework);
            setLaunchArgs(framework, args);
        } catch (InterruptedException | BundleException | IOException e) {
            throw new RuntimeException("Could not start framework.", e);
        }
    }

    private String getOSGiPublicPackages() throws FileNotFoundException, IOException {
        File thermostatEtc = new File(thermostatHome, "etc");
        File osgiBundleDefinitions = new File(thermostatEtc, "osgi-export.properties");

        Properties bundles = new Properties();
        bundles.load(new FileInputStream(osgiBundleDefinitions));

        StringBuilder publicPackages = new StringBuilder();
        /*
         * Packages the launcher requires
         */
        //publicPackages.append("com.redhat.thermostat.common.services");
        boolean firstPackage = true;
        for (Object bundle : bundles.keySet()) {
            if (!firstPackage) {
                publicPackages.append(",\n");
            }
            firstPackage = false;
            publicPackages.append(bundle);
            String bundleVersion = (String) bundles.get(bundle);
            if (!bundleVersion.isEmpty()) {
                publicPackages.append("; version=").append(bundleVersion);
            }
        }

        return publicPackages.toString();
    }

    private void prepareFramework(final Framework framework) throws BundleException, IOException {
        framework.init();
        framework.start();
        if (printOSGiInfo) {
            System.out.println(DEBUG_PREFIX + "OSGi framework has started.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    framework.stop();
                    framework.waitForStop(0);
                    if (printOSGiInfo) {
                        System.out.println(DEBUG_PREFIX + "OSGi framework has shut down.");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error shutting down framework.", e);
                }
            }
        });

    }

    private Framework makeFramework() throws FileNotFoundException, IOException {
        File thermostatBundleHome = new File(thermostatHome, "osgi");

        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class,
                getClass().getClassLoader());
        Map<String, String> bundleConfigurations = new HashMap<String, String>();
        String extraPackages = getOSGiPublicPackages();
        bundleConfigurations.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extraPackages);
        bundleConfigurations.put(Constants.FRAMEWORK_STORAGE, thermostatBundleHome.getAbsolutePath());
        Iterator<FrameworkFactory> factories = loader.iterator();
        if (factories.hasNext()) {
            // we just want the first found
            return factories.next().newFramework(bundleConfigurations);
        } else {
            throw new InternalError("ServiceLoader cannot find a FrameworkFactory!");
        }   
    }

    private void loadBootstrapBundles(Framework framework) throws BundleException, InterruptedException {
        Properties bootstrapProps = new Properties();
        // the properties file should be in the same package as this class
        InputStream res = getClass().getResourceAsStream(PROPS_FILE);
        if (res != null) {
            try {
                bootstrapProps.load(res);
            } catch (IOException e) {
                throw new RuntimeException("Could not load bootstrap bundle properties.", e);
            }
        }
        String[] bundles = bootstrapProps.getProperty(BUNDLELIST).split(",");
        List<String> locations = new ArrayList<>();
        for (String bundle : bundles) {
            String trimmed = bundle.trim();
            if (trimmed != null && trimmed.length() > 0) {
                String location = actualLocation(trimmed);
                locations.add(location);
            }
        }
        OSGiRegistryService.preLoadBundles(framework, locations, printOSGiInfo);
    }

    private void setLoaderVerbosity(Framework framework) throws InterruptedException {
        Object loader = getService(framework, OSGiRegistryService.class.getName());
        callVoidReflectedMethod(loader, "setPrintOSGiInfo", printOSGiInfo, Boolean.TYPE);
    }

    private void setLaunchArgs(Framework framework, String[] args) throws InterruptedException {
        Object launcher = getService(framework, Launcher.class.getName());
        callVoidReflectedMethod(launcher, "setArgs", args, String[].class);
    }

    private Object getService(Framework framework, String name) throws InterruptedException {
        Object service = null;
        ServiceTracker tracker = new ServiceTracker(framework.getBundleContext(), name, null);
        tracker.open();
        service = tracker.waitForService(0);
        tracker.close();
        return service;
    }

    private void callVoidReflectedMethod(Object object, String name, Object arguments, Class<?> argsClass) {
        Class<?> clazz = object.getClass();
        try {
            Method m = clazz.getMethod(name, argsClass);
            m.invoke(object, arguments);
        } catch (IllegalAccessException | NoSuchMethodException |
                IllegalArgumentException | InvocationTargetException e) {
            // It's pretty evil to just swallow these exceptions.  But, these can only
            // really come up in Really Bad Code Errors, which testing will catch early.
            // Right?  Right.  Of course it will.
            e.printStackTrace();
        }
    }

    private String actualLocation(String resourceName) {
        return "file:" + thermostatHome + "/libs/" + resourceName.trim();
    }
}
