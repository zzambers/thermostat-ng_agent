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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.util.concurrent.Semaphore;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.cli.HostVMArguments;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;


public class DumpHeapCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private BundleContext context;
    private final DumpHeapHelper implementation;

    public DumpHeapCommand() {
        this(FrameworkUtil.getBundle(DumpHeapCommand.class).getBundleContext(), new DumpHeapHelper());
    }

    DumpHeapCommand(BundleContext context, DumpHeapHelper impl) {
        this.context = context;
        this.implementation = impl;
    }

    @Override
    public void run(final CommandContext ctx) throws CommandException {
        final HostVMArguments args = new HostVMArguments(ctx.getArguments());

        final CommandException[] ex = new CommandException[1];
        final Semaphore s = new Semaphore(0);
        Runnable successHandler = new Runnable() {
            @Override
            public void run() {
                ctx.getConsole().getOutput().println(translator.localize(LocaleResources.COMMAND_HEAP_DUMP_DONE).getContents());
                s.release();
            }
        };
        Runnable errorHandler = new Runnable() {
            public void run() {
                ex[0] = new CommandException(translator.localize(
                        LocaleResources.HEAP_DUMP_ERROR, args.getHost()
                                .getStringID(), args.getVM().getVmId()));
                s.release();
            }
        };

        ServiceReference vmInfoRef = context.getServiceReference(VmInfoDAO.class.getName());
        requireNonNull(vmInfoRef, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        VmInfoDAO vmInfoDAO = (VmInfoDAO) context.getService(vmInfoRef);
        
        ServiceReference agentInfoRef = context.getServiceReference(AgentInfoDAO.class.getName());
        requireNonNull(agentInfoRef, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        AgentInfoDAO agentInfoDAO = (AgentInfoDAO) context.getService(agentInfoRef);
        
        ServiceReference requestQueueRef = context.getServiceReference(RequestQueue.class.getName());
        requireNonNull(requestQueueRef, translator.localize(LocaleResources.REQUEST_QUEUE_UNAVAILABLE));
        RequestQueue queue = (RequestQueue) context.getService(requestQueueRef);
        
        implementation.execute(vmInfoDAO, agentInfoDAO, args.getVM(), queue, successHandler, errorHandler);
        
        context.ungetService(vmInfoRef);
        context.ungetService(agentInfoRef);
        context.ungetService(requestQueueRef);
        
        try {
            s.acquire();
        } catch (InterruptedException e) {
            // Nothing to do here, just return ASAP.
        }
        
        if (ex[0] != null) {
            throw ex[0];
        }
    }

}

