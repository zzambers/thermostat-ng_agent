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

package com.redhat.thermostat.client.ui;

import java.awt.Component;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.client.AsyncUiFacade;
import com.redhat.thermostat.common.VmMemoryStat;
import com.redhat.thermostat.common.VmMemoryStat.Generation;
import com.redhat.thermostat.common.VmMemoryStat.Space;
import com.redhat.thermostat.common.dao.VmMemoryStatConverter;
import com.redhat.thermostat.common.dao.VmRef;

public class VmMemoryController implements AsyncUiFacade {

    private final VmRef vmRef;
    private final VmMemoryView view;
    private final DBCollection vmMemoryStatsCollection;

    private final Timer timer = new Timer();

    public VmMemoryController(VmRef vmRef, DB db) {
        this.vmRef = vmRef;

        vmMemoryStatsCollection = db.getCollection("vm-memory-stats");

        view = createView();
    }

    @Override
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                VmMemoryStat info = getLatestMemoryStat();
                List<Generation> generations = info.getGenerations();
                for (Generation generation: generations) {
                    List<Space> spaces = generation.spaces;
                    for (Space space: spaces) {
                        view.setMemoryRegionSize(space.name, space.used, space.capacity, space.maxCapacity);
                    }
                }

            }

        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    private VmMemoryStat getLatestMemoryStat() {
        BasicDBObject query = new BasicDBObject();
        query.put("agent-id", vmRef.getAgent().getAgentId());
        query.put("vm-id", Integer.valueOf(vmRef.getId()));
        // TODO ensure timestamp is an indexed column
        BasicDBObject sortByTimeStamp = new BasicDBObject("timestamp", -1);
        DBCursor cursor;
        cursor = vmMemoryStatsCollection.find(query).sort(sortByTimeStamp).limit(1);
        if (cursor.hasNext()) {
            DBObject current = cursor.next();
            return new VmMemoryStatConverter().createVmMemoryStatFromDBObject(current);
        }

        return null;
    }

    @Override
    public void stop() {
        timer.cancel();
    }

    protected VmMemoryView createView() {
        return new VmMemoryPanel();
    }

    public Component getComponent() {
        return view.getUiComponent();
    }

}
