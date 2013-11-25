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

package com.redhat.thermostat.swing.components.experimental.dial;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import com.redhat.thermostat.client.swing.components.AbstractLayout;

class CenterStackingLayout extends AbstractLayout {

    private float baseMultiplierX;
    private float baseMultiplierY; 
    
    public CenterStackingLayout(float baseMultiplierX, float baseMultiplierY) {
        this.baseMultiplierX = baseMultiplierX;
        this.baseMultiplierY = baseMultiplierY;
    }
    
    public CenterStackingLayout() {
        this(0.5f, 0.5f);
    }
    
    @Override
    protected void doLayout(Container parent) {
        
        if (! (parent instanceof RadialComponentPane)) {
            // can't layout anything else!
            throw new IllegalArgumentException("can only layout on a " +
                                               "RadialComponentPane");
        }
        
        RadialComponentPane pane = (RadialComponentPane) parent;
        List<RadialComponent> components = pane.getRadialComponents();
        
        int totalWidth = pane.getWidth();
        int totalHeight = pane.getHeight();
        
        float centerX = totalWidth  * baseMultiplierX;
        float centerY = totalHeight * baseMultiplierY;
        
        Point2D.Float center = new Point2D.Float(centerX, centerY);
        
        for (RadialComponent component : components) {
            
            float percent = component.getAreaPercentage();
            
            float width = (percent * totalWidth)/100; 
            float height = (percent * totalHeight)/100;
            
            float x = centerX - (width/2);
            float y = centerY - (height/2);
                        
            Rectangle bounds = new Rectangle((int) x, (int) y, (int) width, (int) height);

            component.setBounds(bounds);
            component.setRadialCenter(center);
        }
    }
}
