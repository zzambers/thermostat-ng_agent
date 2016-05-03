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

package com.redhat.thermostat.client.filter.internal.vm.swing;

import com.redhat.thermostat.client.ui.ToggleableReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VMStartTimeLabelDecorator implements ToggleableReferenceFieldLabelDecorator {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private VmInfoDAO dao;
    private boolean enabled = false;
    private final Map<VmRef, Date> referenceTimestampMap = new HashMap<>();

    private final ActionNotifier<StatusEvent> notifier = new ActionNotifier<>(this);

    public VMStartTimeLabelDecorator(VmInfoDAO dao) {
        this.dao = dao;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            notifier.fireAction(StatusEvent.STATUS_CHANGED);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void addStatusEventListener(ActionListener<StatusEvent> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeStatusEventListener(ActionListener<StatusEvent> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public String getLabel(String originalLabel, Ref reference) {
        if (!isEnabled()) {
            return originalLabel;
        }

        if (!(reference instanceof VmRef)) {
            return originalLabel;
        }

        VmRef ref = (VmRef) reference;

        if (!referenceTimestampMap.containsKey(ref)) {
            long timestamp = dao.getVmInfo(ref).getStartTimeStamp();
            referenceTimestampMap.put(ref, new Date(timestamp));
        }

        String timestamp = Clock.DEFAULT_DATE_FORMAT.format(referenceTimestampMap.get(ref));
        return t.localize(LocaleResources.STARTTIME_LABEL_DECORATOR, originalLabel, timestamp).getContents();
    }

    @Override
    public int getOrderValue() {
        return ORDER_CODE_GROUP;
    }
}
