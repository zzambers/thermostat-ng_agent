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

package com.redhat.thermostat.client.heap.cli;

import java.util.Collection;

import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.Translate;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.HostVMArguments;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.heap.HeapDump;

public class DumpHeapCommand extends SimpleCommand {

    private static final String NAME = "dump-heap";
    private static final String DESCRIPTION = Translate.localize(LocaleResources.COMMAND_DUMP_HEAP_DESCRIPTION);
    private static final String USAGE = DESCRIPTION;

    private final HeapDumperCommand implementation;

    public DumpHeapCommand() {
        this(new HeapDumperCommand());
    }

    DumpHeapCommand(HeapDumperCommand impl) {
        this.implementation = impl;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        return HostVMArguments.getArgumentSpecs();
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        HostVMArguments args = new HostVMArguments(ctx.getArguments());

        HeapDump hd = implementation.execute(args.getVM());
        ctx.getConsole().getOutput().print(Translate.localize(LocaleResources.COMMAND_HEAP_DUMP_DONE));
        ctx.getConsole().getOutput().print("\n");
    }

}
