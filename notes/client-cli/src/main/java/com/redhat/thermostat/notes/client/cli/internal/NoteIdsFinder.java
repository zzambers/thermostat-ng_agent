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

package com.redhat.thermostat.notes.client.cli.internal;

import com.redhat.thermostat.common.cli.CompletionFinder;
import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.notes.client.cli.locale.LocaleResources;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.shared.locale.Translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteIdsFinder implements CompletionFinder {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private HostNoteDAO hostNoteDao;
    private VmNoteDAO vmNoteDao;

    @Override
    public List<CompletionInfo> findCompletions() {
        if (hostNoteDao == null || vmNoteDao == null) {
            return Collections.emptyList();
        }

        List<Note> notes = new ArrayList<>();
        notes.addAll(hostNoteDao.getAll());
        notes.addAll(vmNoteDao.getAll());

        List<CompletionInfo> result = new ArrayList<>();
        for (Note note : notes) {
            result.add(toCompletionInfo(note));
        }
        return result;
    }

    static CompletionInfo toCompletionInfo(Note note) {
        return new CompletionInfo(note.getId(), trimContent(note.getContent()));
    }

    static String trimContent(String content) {
        if (content.length() < 30) {
            return content;
        }
        return t.localize(LocaleResources.TRIMMED_NOTE_CONTENT, content.substring(0, 27)).getContents();
    }

    public void bindHostNoteDao(HostNoteDAO hostNoteDao) {
        this.hostNoteDao = hostNoteDao;
    }

    public void unbindHostNoteDao(HostNoteDAO hostNoteDao) {
        this.hostNoteDao = null;
    }

    public void bindVmNoteDao(VmNoteDAO vmNoteDao) {
        this.vmNoteDao = vmNoteDao;
    }

    public void unbindVmNoteDao(VmNoteDAO vmNoteDao) {
        this.vmNoteDao = null;
    }

    public void servicesUnavailable() {
        this.hostNoteDao = null;
        this.vmNoteDao = null;
    }

}
