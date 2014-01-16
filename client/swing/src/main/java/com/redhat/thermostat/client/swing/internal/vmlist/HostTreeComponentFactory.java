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

package com.redhat.thermostat.client.swing.internal.vmlist;

import java.util.HashMap;
import java.util.Map;

import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponent;
import com.redhat.thermostat.client.swing.internal.accordion.AccordionComponentFactory;
import com.redhat.thermostat.client.swing.internal.accordion.TitledPane;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorManager;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

public class HostTreeComponentFactory implements AccordionComponentFactory<HostRef, VmRef> {
    
    private Map<VmRef, AccordionComponent> components;
    private Map<HostRef, ReferenceTitle> headers;
    
    private DecoratorManager decoratorManager;
    private ContextActionController contextActionController;
    
    public HostTreeComponentFactory(DecoratorManager decoratorManager,
                                    ContextActionController contextActionController)
    {
        this.decoratorManager = decoratorManager;
        this.contextActionController = contextActionController;
        
        components = new HashMap<>();
        headers = new HashMap<>();
    }

    @Override
    public TitledPane createHeader(HostRef header) {
        ReferenceTitle pane = new ReferenceTitle(header);
        decoratorManager.registerAndSetIcon(pane);
        contextActionController.register(pane, pane);
        headers.put(header, pane);

        return pane;
    }

    @Override
    public AccordionComponent createComponent(HostRef header, VmRef component) {
        ReferenceComponent refComponent = new ReferenceComponent(component);
        decoratorManager.registerAndSetIcon(refComponent);
        contextActionController.register(refComponent, refComponent);
        components.put(component, refComponent);

        return refComponent;
    }
    
    @Override
    public void removeComponent(AccordionComponent accordionComponent,
                                HostRef header, VmRef component)
    {
        ReferenceComponent refComponent = (ReferenceComponent)
                components.remove(component);
        decoratorManager.unregister(refComponent);
        contextActionController.unregister(refComponent, refComponent);
    }
    
    @Override
    public void removeHeader(TitledPane pane, HostRef header) {
        ReferenceTitle title = headers.remove(header);
        decoratorManager.unregister(title);
        contextActionController.unregister(title, title);
    }
    
    public AccordionComponent getAccordionComponent(VmRef vm) {
        return components.get(vm);
    }
    
    public ReferenceTitle getTitledPane(HostRef host) {
        return headers.get(host);
    }
}

