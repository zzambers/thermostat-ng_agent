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

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.ui.Palette;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
@SuppressWarnings("serial")
public class ThreadTimelineHeader extends TimelineBaseComponent {

    public ThreadTimelineHeader(TimelineGroupThreadConverter timelinePageModel,
                                SwingTimelineDimensionModel dimensionModel)
    {
        super(timelinePageModel, dimensionModel);
        setBorder(new TimelineBorder(false));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);

        Rectangle bounds = graphics.getClipBounds();
        int x = getRoundedStartingX(bounds);
        int pixelIncrement = dimensionModel.getIncrementInPixels();
        long increment = dimensionModel.getIncrementInMillis();

        int upperBound = bounds.x + bounds.width;

        Paint gradient = new GradientPaint(0, 0, Palette.WHITE.getColor(), 0, getHeight(), Palette.GRAY.getColor());
        int height = getHeight();

        DateFormat df = new SimpleDateFormat("HH:mm:ss");

        long currentTime = timelinePageModel.getDataModel().getPageRange().getMin() - getRound();

        for (int i = x, j = 0; i < upperBound; i += pixelIncrement, j++) {
            if (j % 10 == 0) {
                graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
                graphics.drawLine(i, 0, i, height);

                graphics.setPaint(gradient);

                String value = df.format(new Date(currentTime));

                int stringWidth = (int) getFont().getStringBounds(value, graphics.getFontRenderContext()).getWidth() - 1;
                int stringHeight = (int) getFont().getStringBounds(value, graphics.getFontRenderContext()).getHeight();
                graphics.fillRect(i + 1, bounds.y + 5, stringWidth + 4, stringHeight + 4);

                graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
                graphics.drawString(value, i + 1, bounds.y + stringHeight + 5);
            }
            currentTime += increment;
        }

        graphics.dispose();
    }
}

