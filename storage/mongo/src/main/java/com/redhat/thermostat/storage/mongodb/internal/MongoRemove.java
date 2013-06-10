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


package com.redhat.thermostat.storage.mongodb.internal;

import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.query.Expression;

class MongoRemove implements Remove {

    private Category category;
    private DBObject query;
    private MongoExpressionParser parser;
    
    public MongoRemove() {
        this(new MongoExpressionParser());
    }
    
    MongoRemove(MongoExpressionParser parser) {
        this.parser = parser;
    }

    @Override
    public Remove from(Category category) {
        if (query != null) {
            throw new IllegalStateException();
        }
        this.category = category;
        return this;
    }

    Category getCategory() {
        return category;
    }

    @Override
    public Remove where(Expression expr) {
        query = parser.parse(expr);
        return this;
    }

    DBObject getQuery() {
        return query;
    }

}

