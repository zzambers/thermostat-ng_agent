/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.CompletionFinderTabCompleter;
import com.redhat.thermostat.common.cli.TabCompleter;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component(immediate = true)
@Service
public class VmIdCompleterService implements CompleterService {

    public static final CliCommandOption VM_ID_OPTION = new CliCommandOption("v", "vmId", true, "VM ID", false);

    @Reference
    private VmIdsFinder vmIdsFinder;

    @Override
    public Set<String> getCommands() {
        return TabCompletion.ALL_COMMANDS_COMPLETER;
    }

    @Override
    public Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters() {
        return Collections.singletonMap(VM_ID_OPTION, new CompletionFinderTabCompleter(vmIdsFinder));
    }

    @Override
    public Map<String, Map<CliCommandOption, ? extends TabCompleter>> getSubcommandCompleters() {
        return Collections.emptyMap();
    }

    void bindVmIdsFinder(VmIdsFinder vmIdsFinder) {
        this.vmIdsFinder = vmIdsFinder;
    }

    void unbindVmIdsFinder(VmIdsFinder vmIdsFinder) {
        this.vmIdsFinder = null;
    }

}
