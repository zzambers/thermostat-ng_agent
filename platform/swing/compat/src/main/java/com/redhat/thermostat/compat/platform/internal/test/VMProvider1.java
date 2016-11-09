/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.compat.platform.internal.test;

import com.redhat.thermostat.compat.platform.DynamicHostPlugin;
import com.redhat.thermostat.compat.platform.DynamicVMPlugin;
import com.redhat.thermostat.compat.platform.EmbeddedPlatformController;
import com.redhat.thermostat.compat.platform.EmbeddedPlatformSwingView;
import com.redhat.thermostat.platform.annotations.ExtensionPoint;
import com.redhat.thermostat.platform.annotations.PlatformService;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.swing.components.ContentPane;
import com.redhat.thermostat.shared.locale.LocalizedString;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import javax.swing.JTabbedPane;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Component
@Service(MVCProvider.class)
@PlatformService(service = DynamicVMPlugin.class)
public class VMProvider1 extends DynamicVMPlugin {
    @Override
    protected EmbeddedPlatformController createController() {
        return new EmbeddedPlatformController() {
            @Override
            public LocalizedString getName() {
                return new LocalizedString("VMProvider1");
            }

            @Override
            public void stop() {
                System.err.println("/////////////////// VMProvider1.Controller stop");
            }

            @Override
            public void start() {
                System.err.println("/////////////////// VMProvider1.Controller start");
            }
        };
    }

    @Override
    protected EmbeddedPlatformSwingView createView() {
        return new EmbeddedPlatformSwingView() {
            private List<VMPluginInterface1> extensions;

            JTabbedPane pane;

            @Override
            protected void init() {
                extensions = new ArrayList<>();
                pane = new JTabbedPane();
                contentPane.add(pane);
                contentPane.setName("VMProvider1");
            }

            @ExtensionPoint(VMPluginInterface1.class)
            public void hook(VMPluginInterface1 extension) {
                System.err.println("/////////////////// VMProvider1 adding: " + extension.getClass());
                extensions.add(extension);
            }

            @Override
            protected void addImpl(ContentPane contentPane, Object constraints, int index) {
                pane.addTab(contentPane.getName(), contentPane);
            }

            @Override
            public void start() {
                super.start();

                System.err.println("/////////////////// VMProvider1 start");

                for (VMPluginInterface1 extension : extensions) {
                    add(extension.getContent());
                }
            }

            @Override
            public void stop() {
                super.stop();

                System.err.println("/////////////////// VMProvider1 stop");

                for (VMPluginInterface1 extension : extensions) {
                    remove(extension.getContent());
                }
            }
        };
    }
}
