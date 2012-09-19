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


package com.redhat.thermostat.common.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractQuery implements Query {

    public static class Sort {
        private Key<?> key;
        private SortDirection direction;
        public Sort() {
            this(null, null);
        }

        public Sort(Key<?> key, SortDirection direction) {
            this.key = key;
            this.direction = direction;
        }
        public Key<?> getKey() {
            return key;
        }
        public void setKey(Key<?> key) {
            this.key = key;
        }
        public SortDirection getDirection() {
            return direction;
        }
        public void setDirection(SortDirection direction) {
            this.direction = direction;
        }

        public int hashCode() {
            return Objects.hash(key, direction);
        }
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Sort)) {
                return false;
            }
            Sort other = (Sort) obj;
            return Objects.equals(key, other.key) && Objects.equals(direction, other.direction);
        }
    }

    private Category category;
    private List<Sort> sorts;
    private int limit = -1;

    public AbstractQuery() {
        sorts = new ArrayList<>();
    }

    @Override
    public Query from(Category category) {
        this.category = category;
        return this;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public <T> Query sort(Key<T> key, SortDirection direction) {
        sorts.add(new Sort(key, direction));
        return this;
    }

    public List<Sort> getSorts() {
        return sorts;
    }

    public void setSorts(List<Sort> sorts) {
        this.sorts = sorts;
    }

    @Override
    public Query limit(int limit) {
        this.limit  = limit;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
