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

package com.redhat.thermostat.client.ui;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.HostCpuViewProvider;
import com.redhat.thermostat.client.core.views.HostInformationView;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.HostMemoryViewProvider;
import com.redhat.thermostat.client.core.views.HostOverviewViewProvider;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MemoryStatDAO;
import com.redhat.thermostat.common.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.OSGIUtils;

public class HostInformationController {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final HostOverviewController overviewController;
    private final HostCpuController cpuController;
    private final HostMemoryController memoryController;

    private final HostInformationView view;

    public HostInformationController(HostInfoDAO hostInfoDao, NetworkInterfaceInfoDAO networkInfoDao, CpuStatDAO cpuStatDao, MemoryStatDAO memoryStatDao, HostRef ref, HostInformationViewProvider provider) {
        OSGIUtils utils = OSGIUtils.getInstance();
        HostCpuViewProvider hostCpuProvider = utils.getService(HostCpuViewProvider.class);
        HostOverviewViewProvider hostOverviewProvider = utils.getService(HostOverviewViewProvider.class);
        HostMemoryViewProvider hostMemoryProvider = utils.getService(HostMemoryViewProvider.class);
        overviewController = new HostOverviewController(hostInfoDao, networkInfoDao, ref, hostOverviewProvider);
        cpuController = new HostCpuController(hostInfoDao, cpuStatDao, ref, hostCpuProvider);
        memoryController = new HostMemoryController(hostInfoDao, memoryStatDao, ref, hostMemoryProvider);

        view = provider.createView();

        view.addChildView(translator.localize(LocaleResources.HOST_INFO_TAB_OVERVIEW), getOverviewController().getView());
        view.addChildView(translator.localize(LocaleResources.HOST_INFO_TAB_CPU), getCpuController().getView());
        view.addChildView(translator.localize(LocaleResources.HOST_INFO_TAB_MEMORY), getMemoryController().getView());

    }

    public HostOverviewController getOverviewController() {
        return overviewController;
    }

    public HostCpuController getCpuController() {
        return cpuController;
    }

    public HostMemoryController getMemoryController() {
        return memoryController;
    }
    
    public BasicView getView() {
        return view;
    }

}
