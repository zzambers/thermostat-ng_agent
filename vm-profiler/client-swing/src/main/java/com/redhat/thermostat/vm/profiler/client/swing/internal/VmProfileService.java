/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.common.AllPassFilter;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;

public class VmProfileService implements InformationService<VmRef> {

    private ApplicationService service;
    private AgentInfoDAO agentInfoDao;
    private ProfileDAO dao;
    private RequestQueue queue;

    public VmProfileService(ApplicationService service, AgentInfoDAO agentInfoDao, ProfileDAO dao, RequestQueue queue) {
        this.service = service;
        this.agentInfoDao = agentInfoDao;
        this.dao = dao;
        this.queue = queue;
    }

    @Override
    public int getOrderValue() {
        return ORDER_CPU_GROUP + 20;
    }

    @Override
    public Filter<VmRef> getFilter() {
        // we can't profile dead VMs, but we can still look at old profiling data.
        return new AllPassFilter<>();
    }

    @Override
    public InformationServiceController<VmRef> getInformationServiceController(VmRef ref) {
        return new VmProfileController(service, agentInfoDao, dao, queue, ref);
    }

}
