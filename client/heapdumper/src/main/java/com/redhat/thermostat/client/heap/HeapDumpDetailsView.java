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

package com.redhat.thermostat.client.heap;

import java.util.Collection;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.BasicView;
import com.redhat.thermostat.common.heap.ObjectHistogram;
import com.sun.tools.hat.internal.model.JavaHeapObject;

public abstract class HeapDumpDetailsView extends BasicView {

    public enum HeapDumpDetailsAction {
        SEARCH,
        GET_OBJECT_DETAIL,
    }
    
    protected final ActionNotifier<HeapDumpDetailsAction> heapDumpDetailsNotifier;
    protected HeapDumpDetailsView() {
        heapDumpDetailsNotifier = new ActionNotifier<HeapDumpDetailsAction>(this);
    }
    
    public void addDumpDetailsListener(ActionListener<HeapDumpDetailsAction> listener) {
        heapDumpDetailsNotifier.addActionListener(listener);
    }
    
    public void removeDumperListener(ActionListener<HeapDumpDetailsAction> listener) {
        heapDumpDetailsNotifier.removeActionListener(listener);
    }

    public static class HeapObjectUI {
        public final String objectId;
        public final String text;

        public HeapObjectUI(String objectId, String text) {
            this.objectId = objectId;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }


    public static interface ObjectReferenceCallback {
        /** get a list of objects that refers to this object */
        Collection<HeapObjectUI> getReferrers(HeapObjectUI obj);
        /** get a list of objects that this object refers to */
        Collection<HeapObjectUI> getReferences(HeapObjectUI obj);
    }

    public abstract void addObjectReferenceCallback(ObjectReferenceCallback callback);
    public abstract void removeObjectReferenceCallback(ObjectReferenceCallback callback);

    public abstract void setHeapHistogram(ObjectHistogram histogram);

    public abstract String getSearchText();

    public abstract void setMatchingObjects(Collection<HeapObjectUI> objects);

    public abstract HeapObjectUI getSelectedMatchingObject();

    public abstract void setObjectDetails(JavaHeapObject object);
}
