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

package com.redhat.thermostat.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.common.Launcher;
import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.main.impl.FrameworkProvider;

public class Thermostat implements Runnable {

    private static Configuration config;

    private BundleContext context;

    public Thermostat(BundleContext context) {
        this.context = context;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void launch()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            FileNotFoundException, IOException, BundleException, InterruptedException {
        Launcher launcher = null;
        ServiceTracker tracker = new ServiceTracker(context, Launcher.class.getName(), null);
        tracker.open();
        launcher = (Launcher) tracker.waitForService(0);
        launcher.run(false);
        tracker.close();
    }

    @Override
    public void run() {
        try {
            launch();
        } catch (IOException | NoSuchMethodException | InterruptedException |
                IllegalAccessException | InvocationTargetException | BundleException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        config = new Configuration();

        List<String> toProcess = new ArrayList<>(Arrays.asList(args));
        Iterator<String> iter = toProcess.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (("--print-osgi-info").equals(arg)) {
                config.setPrintOSGiInfo(true);
                iter.remove();
            }
        }

        FrameworkProvider frameworkProvider = new FrameworkProvider(config);
        frameworkProvider.startFramework(toProcess.toArray(new String[0]));

    }

    

}

