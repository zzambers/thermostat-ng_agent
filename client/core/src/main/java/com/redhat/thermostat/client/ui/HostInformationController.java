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

import java.util.Collections;
import java.util.List;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.internal.InformationServiceComparator;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.HostInformationView;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.common.dao.HostRef;

public class HostInformationController {

    private final HostInformationView view;

    public HostInformationController(UiFacadeFactory uiFacadeFactory, HostRef ref, HostInformationViewProvider provider) {
        view = provider.createView();

        List<InformationService<HostRef>> hostInfoServices = uiFacadeFactory.getHostInformationServices();
        Collections.sort(hostInfoServices, new InformationServiceComparator<InformationService<HostRef>>());
        for (InformationService<HostRef> hostInfoService : hostInfoServices) {
            if (hostInfoService.getFilter().matches(ref)) {
                InformationServiceController<HostRef> ctrl = hostInfoService.getInformationServiceController(ref);
                String name = ctrl.getLocalizedName();
                view.addChildView(name, ctrl.getView());
            }
        }
    }

    public BasicView getView() {
        return view;
    }

}
