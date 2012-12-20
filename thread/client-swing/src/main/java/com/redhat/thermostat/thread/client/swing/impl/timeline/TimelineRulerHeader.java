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

package com.redhat.thermostat.thread.client.swing.impl.timeline;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.beans.Transient;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JScrollPane;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.GradientPanel;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.model.LongRange;

@SuppressWarnings("serial")
public class TimelineRulerHeader extends GradientPanel {

    private LongRange range;
    private JScrollPane scrollPane;
    
    public TimelineRulerHeader(LongRange range, JScrollPane scrollPane) {
        
        super(Palette.LIGHT_GRAY.getColor(), Palette.WHITE.getColor());
        
        this.range = range;
        this.scrollPane = scrollPane;
    }
    
    public LongRange getRange() {
        return range;
    }
    
    @Override
    public int getHeight() {
        return 25;
    }
    
    @Override
    @Transient
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = getHeight();
        return dim;
    }
    
    @Override
    public Dimension getSize() {
        return getPreferredSize();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        
        super.paintComponent(g);

        Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);

        int currentValue = scrollPane.getHorizontalScrollBar().getValue();

        Rectangle bounds = g.getClipBounds();
        int totalInc = TimelineUtils.drawMarks(range, graphics, bounds, currentValue,
                                               TimelineUtils.calculateWidth(range),
                                               getHeight(), true);
        
        drawTimelineStrings(graphics, currentValue, bounds, totalInc);
        
        graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
        graphics.drawLine(bounds.x, bounds.height - 1, bounds.width, bounds.height - 1);
        
        graphics.dispose();
    }
    
    private void drawTimelineStrings(Graphics2D graphics, int currentValue, Rectangle bounds, int totalInc) {
        
        Font font = TimelineUtils.FONT;
        
        graphics.setFont(font);
        
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        
        Paint gradient = new GradientPaint(0, 0, Palette.WHITE.getColor(), 0, getHeight(), Palette.GRAY.getColor());
        
        graphics.setColor(Palette.EARL_GRAY.getColor());
        
        long round = range.getMin() % (TimelineUtils.STEP * 10);
        int shift = (int) (round / TimelineUtils.STEP) * totalInc;
        long currentTime = range.getMin() - round;
        
        int lowerBound = bounds.x - (4 * totalInc);
        int x = ((bounds.x - currentValue) - shift);
        
        long increment = 0;
        int height = getHeight();
        for (int i = x; i < bounds.width; i += totalInc) {
            if (increment % 10 == 0 && i >= lowerBound) {
                graphics.setColor(Palette.THERMOSTAT_BLU.getColor());
                graphics.drawLine(i, 0, i, height);
                
                graphics.setPaint(gradient);

                String value = df.format(new Date(currentTime));

                int stringWidth = (int) font.getStringBounds(value, graphics.getFontRenderContext()).getWidth() - 1;
                int stringHeight = (int) font.getStringBounds(value, graphics.getFontRenderContext()).getHeight();
                graphics.fillRect(i + 1, bounds.y + 5, stringWidth + 4, stringHeight + 4);
                
                graphics.setColor(Palette.THERMOSTAT_BLU.getColor());                
                graphics.drawString(value, i + 1, bounds.y + stringHeight + 5);
            }
            currentTime += TimelineUtils.STEP;
            increment++;
        }
    }
}
