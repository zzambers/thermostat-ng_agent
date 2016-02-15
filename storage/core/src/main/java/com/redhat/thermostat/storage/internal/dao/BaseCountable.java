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

package com.redhat.thermostat.storage.internal.dao;

import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.dao.AbstractDao;
import com.redhat.thermostat.storage.dao.QueryResult;
import com.redhat.thermostat.storage.dao.SimpleDaoQuery;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.Pojo;

import static com.redhat.thermostat.storage.internal.dao.LoggingUtil.logDescriptorParsingException;
import static com.redhat.thermostat.storage.internal.dao.LoggingUtil.logStatementExecutionException;

class BaseCountable extends AbstractDao {
    
    private static final int ERROR_COUNT_RESULT = -1;
    private static final Logger logger = LoggingUtils.getLogger(BaseCountable.class);

    /**
     * Performs an aggregate count query as described by the given descriptor.
     * 
     * @param storage the storage to use for preparing the descriptor.
     * @param category the query category.
     * @param descriptor the query descriptor.
     * @return -1 if execution failed for some reason, the actual count of the
     *         query results if successful.
     */
    protected long getCount(Storage storage, Category<AggregateCount> category, String descriptor) {
        QueryResult<AggregateCount> result = executeQuery(new SimpleDaoQuery<>(storage, category, descriptor));
        AggregateCount count = result.head();
        if (count == null || result.hasExceptions()) {
            return ERROR_COUNT_RESULT;
        } else {
            return count.getCount();
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

