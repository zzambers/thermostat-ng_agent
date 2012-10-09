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

package com.redhat.thermostat.client.filter.vm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.core.VmFilter;
import com.redhat.thermostat.client.osgi.service.MenuAction;
import com.redhat.thermostat.client.osgi.service.VmDecorator;
import com.redhat.thermostat.common.dao.VmInfoDAO;

public class VMFilterActivator implements BundleActivator {

    private final List<ServiceRegistration> registeredServices = Collections.synchronizedList(new ArrayList<ServiceRegistration>());

    private ServiceTracker vmInfoDaoTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        
        vmInfoDaoTracker = new ServiceTracker(context, VmInfoDAO.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                VmInfoDAO dao = (VmInfoDAO) context.getService(reference);

                LivingVMFilter filter = new LivingVMFilter(dao);
                VMDecorator decorator = new VMDecorator(dao);
                DeadVMDecorator deadDecorator = new DeadVMDecorator(dao);
                
                LivingVMFilterMenuAction menu = new LivingVMFilterMenuAction(filter);

                ServiceRegistration registration = null;
                
                registration = context.registerService(MenuAction.class.getName(), menu, null);
                registeredServices.add(registration);

                registration = context.registerService(VmDecorator.class.getName(), deadDecorator, null);
                registeredServices.add(registration);

                registration = context.registerService(VmDecorator.class.getName(), decorator, null);
                registeredServices.add(registration);

                registration = context.registerService(VmFilter.class.getName(), filter, null);
                registeredServices.add(registration);

                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                Iterator<ServiceRegistration> iterator = registeredServices.iterator();
                while(iterator.hasNext()) {
                    ServiceRegistration registration = iterator.next();
                    registration.unregister();
                    iterator.remove();
                }

                context.ungetService(reference);
                super.removedService(reference, service);
            }
        };
        vmInfoDaoTracker.open();
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        vmInfoDaoTracker.close();
        
        for (ServiceRegistration registration : registeredServices) {
            registration.unregister();
        }
    }
}
