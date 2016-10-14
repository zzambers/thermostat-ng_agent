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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNote;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class UpdateNoteSubcommand extends NotesSubcommand {

    static final String SUBCOMMAND_NAME = "update";

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        assertExpectedAgentAndVmArgsProvided(args);

        String noteId = getNoteId(args);
        String noteContent = getNoteContent(args);

        if (args.hasArgument(VmArgument.ARGUMENT_NAME)) {
            VmNoteDAO vmNoteDao = services.getRequiredService(VmNoteDAO.class);
            VmInfoDAO vmInfoDao = services.getRequiredService(VmInfoDAO.class);

            VmId vmId = VmArgument.required(args).getVmId();
            checkVmExists(vmId);
            AgentId agentId = new AgentId(vmInfoDao.getVmInfo(vmId).getAgentId());
            checkAgentExists(agentId);

            VmNote note = vmNoteDao.getById(getVmRefFromVmId(vmId), noteId);
            if (note == null) {
                throw new CommandException(translator.localize(LocaleResources.NO_SUCH_VM_NOTE, vmId.get(), noteId));
            }
            note.setContent(noteContent);
            note.setTimeStamp(System.currentTimeMillis());
            vmNoteDao.update(note);
        } else {
            HostNoteDAO hostNoteDao = services.getRequiredService(HostNoteDAO.class);

            AgentId agentId = AgentArgument.required(args).getAgentId();
            checkAgentExists(agentId);

            HostNote note = hostNoteDao.getById(getHostRefFromAgentId(agentId), noteId);
            if (note == null) {
                throw new CommandException(translator.localize(LocaleResources.NO_SUCH_AGENT_NOTE, agentId.get(), noteId));
            }
            note.setContent(noteContent);
            note.setTimeStamp(System.currentTimeMillis());
            hostNoteDao.update(note);
        }
    }

}
