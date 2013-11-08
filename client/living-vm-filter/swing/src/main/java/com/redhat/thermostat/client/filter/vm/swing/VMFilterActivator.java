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

package com.redhat.thermostat.client.filter.vm.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.filter.host.swing.HostLabelDecorator;
import com.redhat.thermostat.client.swing.ReferenceFieldDecoratorLayout;
import com.redhat.thermostat.client.ui.DecoratorProvider;
import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class VMFilterActivator implements BundleActivator {

    @SuppressWarnings("rawtypes")
    private final List<ServiceRegistration> registeredServices = Collections.synchronizedList(new ArrayList<ServiceRegistration>());

    private MultipleServiceTracker tracker;

    @SuppressWarnings("rawtypes")
    @Override
    public void start(final BundleContext context) throws Exception {
        
        Class<?> [] services =  new Class<?> [] {
                VmInfoDAO.class,
                HostInfoDAO.class,
                NetworkInterfaceInfoDAO.class,
        };
        
        tracker = new MultipleServiceTracker(context, services, new MultipleServiceTracker.Action() {
            
            @Override
            public void dependenciesUnavailable() {
                Iterator<ServiceRegistration> iterator = registeredServices.iterator();
                while(iterator.hasNext()) {
                    ServiceRegistration registration = iterator.next();
                    registration.unregister();
                    iterator.remove();
                }
            }
            
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                ServiceRegistration registration = null;

                VmInfoDAO vmDao = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                HostInfoDAO hostDao = (HostInfoDAO) services.get(HostInfoDAO.class.getName());

                LivingVMDecoratorProvider decorator = new LivingVMDecoratorProvider(vmDao, hostDao);
                DeadVMDecoratorProvider deadDecorator = new DeadVMDecoratorProvider(vmDao);
                
                Dictionary<String, String> decoratorProperties = new Hashtable<>();
                decoratorProperties.put(Constants.GENERIC_SERVICE_CLASSNAME, VmRef.class.getName());
                
                registration = context.registerService(DecoratorProvider.class.getName(),
                                                       deadDecorator, decoratorProperties);
                registeredServices.add(registration);

                registration = context.registerService(DecoratorProvider.class.getName(),
                                                       decorator, decoratorProperties);
                registeredServices.add(registration);
                
                VMLabelDecorator vmLabelDecorator = new VMLabelDecorator(vmDao);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID, ReferenceFieldDecoratorLayout.LABEL_INFO.name());

                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                        vmLabelDecorator, decoratorProperties);
                
                NetworkInterfaceInfoDAO networkDao = (NetworkInterfaceInfoDAO)
                            services.get(NetworkInterfaceInfoDAO.class.getName());
                
                HostLabelDecorator hostLabelDecorator = new HostLabelDecorator(networkDao);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID, ReferenceFieldDecoratorLayout.LABEL_INFO.name());
                
                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                        hostLabelDecorator, decoratorProperties);
                
                registeredServices.add(registration);
            }
        });
        tracker.open();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
        for (ServiceRegistration registration : registeredServices) {
            registration.unregister();
        }
    }
}

