/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ThermostatScrollBar;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.swing.experimental.components.ContentPane;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RangeChangeEvent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RangeChangeListener;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RatioChangeEvent;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.RatioChangeListener;
import com.redhat.thermostat.thread.client.swing.impl.timeline.model.TimelineModel;
import java.awt.BorderLayout;
import java.awt.Rectangle;

/**
 *
 */
public class TimelineViewComponent extends ContentPane {

    private TimelineModel model;
    private ThermostatScrollPane scrollPane;
    private TimelineViewport viewport;

    private ThermostatScrollBar scrollBar;
    private UIDefaults uiDefaults;

    public TimelineViewComponent(UIDefaults uiDefaults) {
        this.uiDefaults = uiDefaults;
    }

    public void initComponents() {

        this.model = new TimelineModel();

        viewport = new TimelineViewport();

        scrollPane = new ThermostatScrollPane(viewport);
        scrollPane.setVerticalScrollBarPolicy(ThermostatScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ThermostatScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        RangeComponentHeader header = new RangeComponentHeader(model, uiDefaults);
        header.initComponents();
        scrollPane.setColumnHeaderView(header);

        add(scrollPane, BorderLayout.CENTER);

        scrollBar = new ThermostatScrollBar(ThermostatScrollBar.HORIZONTAL);
        model.setScrollBarModel(scrollBar.getModel());

        scrollBar.addAdjustmentListener(new TimelineAdjustmentListener(model));

        add(scrollBar, BorderLayout.SOUTH);
        scrollBar.setEnabled(false);
        scrollBar.setVisible(false);

        model.addRatioChangeListener(new RatioChangeListener() {
            @Override
            public void ratioChanged(RatioChangeEvent event) {
                checkEnableScrollbar();
            }
        });
        model.addRangeChangeListener(new RangeChangeListener() {
            @Override
            public void rangeChanged(RangeChangeEvent event) {
                checkEnableScrollbar();
            }
        });
    }

    private void checkEnableScrollbar() {
        Range<Long> range = model.getRange();
        Rectangle bounds = getBounds();
        long length = bounds.x + bounds.width;
        length = Math.round(length / model.getMagnificationRatio());

        long lengthInMs = range.getMax() - range.getMin();
        lengthInMs = Math.round(lengthInMs / model.getMagnificationRatio());

        boolean shouldEnable = (length < lengthInMs);

        scrollBar.setVisible(shouldEnable);
        scrollBar.setEnabled(shouldEnable);
    }

    public void addTimeline(TimelineComponent timeline) {
        viewport.add(timeline);
        checkEnableScrollbar();

        timeline.

        revalidate();
        repaint();
    }

    public TimelineModel getModel() {
        return model;
    }
}

