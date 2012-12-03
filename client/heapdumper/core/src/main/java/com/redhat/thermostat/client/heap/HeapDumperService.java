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

package com.redhat.thermostat.client.heap;

import com.redhat.thermostat.client.core.VmFilter;
import com.redhat.thermostat.client.core.VmInformationService;
import com.redhat.thermostat.client.core.controllers.VmInformationServiceController;
import com.redhat.thermostat.client.osgi.service.AlwaysMatchFilter;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class HeapDumperService implements VmInformationService {
    
    private static final int PRIORITY = PRIORITY_MEMORY_GROUP + 60;
    private ApplicationService appService;
    private AgentInfoDAO agentInfoDao;
    private VmMemoryStatDAO vmMemoryStatDao;
    private HeapDAO heapDao;

    private VmFilter filter = new AlwaysMatchFilter();
    private HeapDumpDetailsViewProvider detailsViewProvider;
    private HeapHistogramViewProvider histogramViewProvider;
    private HeapViewProvider viewProvider;
    private ObjectDetailsViewProvider objectDetailsViewProvider;
    private ObjectRootsViewProvider objectRootsViewProvider;

    public HeapDumperService(ApplicationService appService, AgentInfoDAO agentInfoDao, VmMemoryStatDAO vmMemoryStatDao, HeapDAO heapDao, HeapDumpDetailsViewProvider detailsViewProvider, HeapViewProvider viewProvider, HeapHistogramViewProvider histogramViewProvider, ObjectDetailsViewProvider objectDetailsViewProvider, ObjectRootsViewProvider objectRootsViewProvider) {
        this.agentInfoDao = agentInfoDao;
        this.vmMemoryStatDao = vmMemoryStatDao;
        this.heapDao = heapDao;
        this.appService = appService;
        this.viewProvider = viewProvider;
        this.detailsViewProvider = detailsViewProvider;
        this.histogramViewProvider = histogramViewProvider;
        this.objectDetailsViewProvider = objectDetailsViewProvider;
        this.objectRootsViewProvider = objectRootsViewProvider;
    }

    @Override
    public VmInformationServiceController getInformationServiceController(VmRef ref) {
        return new HeapDumpController(agentInfoDao, vmMemoryStatDao, heapDao, ref, appService, viewProvider, detailsViewProvider, histogramViewProvider, objectDetailsViewProvider, objectRootsViewProvider);
    }

    @Override
    public VmFilter getFilter() {
        return filter;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
