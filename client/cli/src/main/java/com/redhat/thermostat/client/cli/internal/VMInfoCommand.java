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

package com.redhat.thermostat.client.cli.internal;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.cli.HostVMArguments;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.DAOException;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class VMInfoCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String STILL_ALIVE = translator.localize(LocaleResources.VM_STOP_TIME_RUNNING).getContents();

    private final BundleContext context;

    public VMInfoCommand() {
        this(FrameworkUtil.getBundle(VMInfoCommand.class).getBundleContext());
    }

    /** For tests only */
    VMInfoCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference vmsDAORef = context.getServiceReference(VmInfoDAO.class.getName());
        if (vmsDAORef == null) {
            throw new CommandException(translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        }
        VmInfoDAO vmsDAO = (VmInfoDAO) context.getService(vmsDAORef);

        HostVMArguments hostVMArgs = new HostVMArguments(ctx.getArguments(), true, false);
        HostRef host = hostVMArgs.getHost();
        VmRef vm = hostVMArgs.getVM();
        try {
            if (vm != null) {
                getAndPrintVMInfo(ctx, vmsDAO, vm);
            } else {
                getAndPrintAllVMInfo(ctx, vmsDAO, host);

            }
        } catch (DAOException ex) {
            ctx.getConsole().getError().println(ex.getMessage());
        } finally {
            context.ungetService(vmsDAORef);
        }
    }

    private void getAndPrintAllVMInfo(CommandContext ctx, VmInfoDAO vmsDAO, HostRef host) {
        Collection<VmRef> vms = vmsDAO.getVMs(host);
        for (VmRef vm : vms) {
            getAndPrintVMInfo(ctx, vmsDAO, vm);
        }
    }

    private void getAndPrintVMInfo(CommandContext ctx, VmInfoDAO vmsDAO, VmRef vm) {

        VmInfo vmInfo = vmsDAO.getVmInfo(vm);

        TableRenderer table = new TableRenderer(2);
        table.printLine(translator.localize(LocaleResources.VM_INFO_VM_ID).getContents(), vmInfo.getVmId());
        table.printLine(translator.localize(LocaleResources.VM_INFO_PROCESS_ID).getContents(), String.valueOf(vmInfo.getVmPid()));
        table.printLine(translator.localize(LocaleResources.VM_INFO_START_TIME).getContents(), new Date(vmInfo.getStartTimeStamp()).toString());
        if (vmInfo.isAlive()) {
            table.printLine(translator.localize(LocaleResources.VM_INFO_STOP_TIME).getContents(), STILL_ALIVE);
        } else {
            table.printLine(translator.localize(LocaleResources.VM_INFO_STOP_TIME).getContents(), new Date(vmInfo.getStopTimeStamp()).toString());
        }
        printUserInfo(vmInfo, table);
        table.printLine(translator.localize(LocaleResources.VM_INFO_MAIN_CLASS).getContents(), vmInfo.getMainClass());
        table.printLine(translator.localize(LocaleResources.VM_INFO_COMMAND_LINE).getContents(), vmInfo.getJavaCommandLine());
        table.printLine(translator.localize(LocaleResources.VM_INFO_JAVA_VERSION).getContents(), vmInfo.getJavaVersion());
        table.printLine(translator.localize(LocaleResources.VM_INFO_VIRTUAL_MACHINE).getContents(), vmInfo.getVmName());
        table.printLine(translator.localize(LocaleResources.VM_INFO_VM_ARGUMENTS).getContents(), vmInfo.getVmArguments());
        
        PrintStream out = ctx.getConsole().getOutput();
        table.render(out);
    }

    private void printUserInfo(VmInfo vmInfo, TableRenderer table) {
        // Check if we have valid user info
        long uid = vmInfo.getUid();
        String user;
        if (uid >= 0) {
            user = String.valueOf(uid);
            String username = vmInfo.getUsername();
            if (username != null) {
                user += "(" + username + ")";
            }
        }
        else {
            user = translator.localize(LocaleResources.VM_INFO_USER_UNKNOWN).getContents();
        }
        table.printLine(translator.localize(LocaleResources.VM_INFO_USER).getContents(), user);
    }

}

