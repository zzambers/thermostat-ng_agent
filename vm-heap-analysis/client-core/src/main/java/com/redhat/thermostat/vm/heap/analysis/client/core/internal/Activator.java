/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpListViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumperService;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class Activator implements BundleActivator {

    private MultipleServiceTracker tracker;
    private ServiceRegistration reg;

    @Override
    public void start(final BundleContext context) throws Exception {
        Class<?>[] deps = new Class<?>[] {
            ApplicationService.class,
            VmInfoDAO.class,
            VmMemoryStatDAO.class,
            HeapDAO.class,
            HeapViewProvider.class,
            HeapDumpDetailsViewProvider.class,
            HeapHistogramViewProvider.class,
            HeapTreeMapViewProvider.class,
            ObjectDetailsViewProvider.class,
            ObjectRootsViewProvider.class,
            HeapDumpListViewProvider.class,
            ProgressNotifier.class,
        };

        tracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                
                ProgressNotifier notifier = (ProgressNotifier) services.get(ProgressNotifier.class.getName());
                Objects.requireNonNull(notifier);
                
                ApplicationService appSvc = (ApplicationService) services.get(ApplicationService.class.getName());
                Objects.requireNonNull(appSvc);
                VmInfoDAO vmInfoDao = Objects.requireNonNull((VmInfoDAO) services.get(VmInfoDAO.class.getName()));
                VmMemoryStatDAO vmMemoryStatDao = (VmMemoryStatDAO) services.get(VmMemoryStatDAO.class.getName());
                Objects.requireNonNull(vmMemoryStatDao);
                HeapDAO heapDao = (HeapDAO) services.get(HeapDAO.class.getName());
                Objects.requireNonNull(heapDao);
                HeapViewProvider viewProvider = (HeapViewProvider) services.get(HeapViewProvider.class.getName());
                Objects.requireNonNull(viewProvider);
                HeapDumpDetailsViewProvider detailsViewProvider = (HeapDumpDetailsViewProvider) services
                        .get(HeapDumpDetailsViewProvider.class.getName());
                Objects.requireNonNull(detailsViewProvider);
                HeapHistogramViewProvider histogramViewProvider = (HeapHistogramViewProvider) services
                        .get(HeapHistogramViewProvider.class.getName());
                HeapTreeMapViewProvider treeMapViewProvider = (HeapTreeMapViewProvider) services
                        .get(HeapTreeMapViewProvider.class.getName());
                Objects.requireNonNull(histogramViewProvider);
                ObjectDetailsViewProvider objectDetailsViewProvider = (ObjectDetailsViewProvider) services
                        .get(ObjectDetailsViewProvider.class.getName());
                Objects.requireNonNull(objectDetailsViewProvider);
                ObjectRootsViewProvider objectRootsViewProvider = (ObjectRootsViewProvider) services
                        .get(ObjectRootsViewProvider.class.getName());
                Objects.requireNonNull(objectRootsViewProvider);

                HeapDumpListViewProvider heapDumpListViewProvider = (HeapDumpListViewProvider) services
                        .get(HeapDumpListViewProvider.class.getName());
                
                HeapDumperService service = new HeapDumperServiceImpl(appSvc,
                        vmInfoDao, vmMemoryStatDao, heapDao, viewProvider,
                        detailsViewProvider, histogramViewProvider, treeMapViewProvider,
                        objectDetailsViewProvider, objectRootsViewProvider,
                        heapDumpListViewProvider, notifier);
                Dictionary<String, String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, VmRef.class.getName());
                properties.put(InformationService.KEY_SERVICE_ID, HeapDumperService.SERVICE_ID);
                reg = context.registerService(InformationService.class.getName(), service , properties);
            }

            @Override
            public void dependenciesUnavailable() {
                reg.unregister();
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}
