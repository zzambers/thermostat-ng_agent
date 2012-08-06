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

package com.redhat.thermostat.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * 
 * This class is intended to be used within BundleActivator implementations that require
 * some action be taken only after certain services have appeared.  It is not actually
 * an extension to the ServiceTracker class, but embeds a number of ServiceTracker objects
 * (one per required service) nonetheless.
 *
 */
public class MultipleServiceTracker {

    public interface Action {
        public void doIt(Map<Object, Object> services);
    }

    class InternalServiceTrackerCustomizer implements ServiceTrackerCustomizer {

        private static final String OBJECT_CLASS = "objectClass";
        private ServiceTracker tracker;

        void setTracker(ServiceTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            ensureTracker();
            services.put(getServiceClassName(reference), context.getService(reference));
            if (allServicesReady()) {
                action.doIt(services);
            }
            return tracker.addingService(reference);
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            // We don't actually need to do anything here.
            ensureTracker();
            tracker.modifiedService(reference, service);
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            ensureTracker();
            services.put(getServiceClassName(reference), null);
            tracker.removedService(reference, service);
        }

        private void ensureTracker() {
            if (tracker == null) {
                // This class is used only internally, and is initialized within the constructor.  The trackers that could
                // be generating events cannot be opened except by calling open on the enclosing class, so this should
                // never ever ever ever happen.
                throw new IllegalStateException("Trackers should not be opened before this guy has been set.");
            }
        }

        private Object getServiceClassName(ServiceReference reference) {
            return ((String[]) reference.getProperty(OBJECT_CLASS))[0];
        }
    }

    private Map<Object, Object> services;
    private Collection<ServiceTracker> trackers;
    private Action action;
    private BundleContext context;

    public MultipleServiceTracker(BundleContext context, Class[] classes, Action action) {
        action.getClass();
        context.getClass();
        classes.getClass(); // Harmless call to cause NPE if passed null.
        this.context = context;
        services = new HashMap<>();
        trackers = new ArrayList<>();
        for (Class clazz: classes) {
            InternalServiceTrackerCustomizer tc = new InternalServiceTrackerCustomizer();
            ServiceTracker tracker = new ServiceTracker(context, clazz.getName(), tc);
            tc.setTracker(tracker);
            trackers.add(tracker);
            services.put(clazz.getName(), null);
        }
        this.action = action;
    }

    public void open() {
        for (ServiceTracker tracker : trackers) {
            tracker.open();
        }
    }

    public void close() {
        for (ServiceTracker tracker: trackers) {
            tracker.close();
        }
    }

    private boolean allServicesReady() {
        for (Entry<Object, Object> entry: services.entrySet()) {
            if (entry.getValue() == null) {
                return false;
            }
        }
        return true;
    }
}
