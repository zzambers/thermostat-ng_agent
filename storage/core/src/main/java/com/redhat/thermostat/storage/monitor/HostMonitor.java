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

package com.redhat.thermostat.storage.monitor;

import java.util.List;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

/**
 * Monitors selected {@link HostRef} for {@link VmRef} lifecycle changes.
 */
public interface HostMonitor {
    
    public enum Action {
        VM_ADDED,
        VM_REMOVED,
    }
    
    /**
     * Adds this listener to the given {@link HostRef}
     */
    void addHostChangeListener(HostRef host, ActionListener<Action> listener);
    
    /**
     * Removes the listener to the given {@link HostRef}
     */
    void removeHostChangeListener(HostRef host, ActionListener<Action> listener);

    /**
     * Returns all the {@link VmRef} tracked by the given host and matching the
     * given {@link Filter}.
     */
    List<VmRef> getVirtualMachines(HostRef host, Filter<VmRef> matcher);
}

