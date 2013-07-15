/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.storage.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

public class VmLatestPojoListGetter<T extends TimeStampedPojo> {
    
    private static final Logger logger = LoggingUtils.getLogger(VmLatestPojoListGetter.class);

    private final Storage storage;
    private final Category<T> cat;
    private final String queryLatest;

    public VmLatestPojoListGetter(Storage storage, Category<T> cat) {
        this.storage = storage;
        this.cat = cat;
        this.queryLatest = "QUERY " + cat.getName() + " WHERE "
                + Key.AGENT_ID.getName() + " = ?s AND "
                + Key.VM_ID.getName() + " = ?i AND " 
                + Key.TIMESTAMP.getName() + " > ?l SORT "
                + Key.TIMESTAMP.getName() + " DSC";
    }

    public List<T> getLatest(VmRef vmRef, long since) {
        PreparedStatement<T> query = buildQuery(vmRef, since);
        if (query == null) {
            return Collections.emptyList();
        }
        return getLatest(query);
    }

    private List<T> getLatest(PreparedStatement<T> query) {
        Cursor<T> cursor;
        try {
            cursor = query.executeQuery();
        } catch (StatementExecutionException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Executing query '" + query + "' failed!", e);
            return Collections.emptyList();
        }
        
        List<T> result = new ArrayList<>();
        while (cursor.hasNext()) {
            T pojo = cursor.next();
            result.add(pojo);
        }
        return result;
    }

    protected PreparedStatement<T> buildQuery(VmRef vmRef, long since) {
        StatementDescriptor<T> desc = new StatementDescriptor<>(cat, queryLatest);
        PreparedStatement<T> stmt = null;
        try {
            stmt = storage.prepareStatement(desc);
            stmt.setString(0, vmRef.getAgent().getAgentId());
            stmt.setInt(1, vmRef.getId());
            stmt.setLong(2, since);
        } catch (DescriptorParsingException e) {
            // should not happen, but if it *does* happen, at least log it
            logger.log(Level.SEVERE, "Preparing query '" + desc + "' failed!", e);
        }
        return stmt;
    }

}

