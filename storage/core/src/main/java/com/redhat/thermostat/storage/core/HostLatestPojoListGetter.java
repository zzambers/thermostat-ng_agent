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

package com.redhat.thermostat.storage.core;

import java.util.List;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.model.TimeStampedPojo;

/**
 * @see HostTimeIntervalPojoListGetter
 */
public class HostLatestPojoListGetter<T extends TimeStampedPojo>  {

    public static final String HOST_LATEST_QUERY_FORMAT = "QUERY %s WHERE '"
            + Key.AGENT_ID.getName() + "' = ?s AND '"
            + Key.TIMESTAMP.getName() + "' > ?l SORT '"
            + Key.TIMESTAMP.getName() + "' DSC";

    private static final Logger logger = LoggingUtils.getLogger(HostLatestPojoListGetter.class);

    private final Category<T> cat;
    private final String queryLatest;

    public HostLatestPojoListGetter(Category<T> cat) {
        this.cat = cat;
        this.queryLatest = String.format(HOST_LATEST_QUERY_FORMAT, cat.getName());
    }

    /**
     * @deprecated use {@link #getLatest(AgentId, long)}
     */
    @Deprecated
    public List<T> getLatest(final HostRef hostRef, final long since) {
        return getLatest(new AgentId(hostRef.getAgentId()), since);
    }

    public List<T> getLatest(final AgentId agentId, final long since) {
        return null;
    }

    // package private for testing
    String getQueryLatestDesc() {
        return queryLatest;
    }

    protected Logger getLogger() {
        return logger;
    }
}

