/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components.experimental;

import junit.framework.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.BeforeClass;
import org.junit.Test;


public class TreeMapComponentTest {

    private TreeMapComponent treeMap;
    private static TreeMapNode tree;
    private static TreeMapNode node1;
    private static TreeMapNode node2;
    private static Dimension dim;

    @BeforeClass
    public static void setUpOnce() {
        tree = new TreeMapNode(1);
        node1 = new TreeMapNode(1);
        node2 = new TreeMapNode(1);
        tree.addChild(node1);
        node1.addChild(node2);
        dim = new Dimension(500, 500);
    }


    @Test
    public final void testTreeMapComponent() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {

                boolean catched = false;

                try {
                    treeMap = new TreeMapComponent(tree, dim);
                    // pass
                 } catch(NullPointerException e) {
                    Assert.fail("Didn't expect exception.");
                 }
                try {
                    treeMap = new TreeMapComponent(null, null);
                } catch(NullPointerException e) {
                    catched = true;
                }
                assertTrue(catched);
                catched = false;

                try {
                    treeMap = new TreeMapComponent(tree, null);
                } catch(NullPointerException e) {
                    catched = true;
                }
                assertTrue(catched);
                catched = false;

                try {
                    treeMap = new TreeMapComponent(null, dim);
                } catch(NullPointerException e) {
                    catched = true;
                }
                assertTrue(catched);
            }
        });
    }

    @Test
    public final void testGetRoot() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap = new TreeMapComponent(tree, dim);
                assertEquals(tree, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testIsZoomInEnabled() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TreeMapComponent treeMap = new TreeMapComponent(tree, dim);

                assertFalse("Should not be able to zoom in on null", treeMap.isZoomInEnabled(null));
                assertFalse("Should not be able to zoom in on root", treeMap.isZoomInEnabled(tree));
                assertTrue("Should be able to zoom in on node 1", treeMap.isZoomInEnabled(node1));
                assertFalse("Should not be able to zoom in on node 2", treeMap.isZoomInEnabled(node2));
            }
        });
    }

    @Test
    public final void testZoomIn() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                TreeMapComponent treeMap = new TreeMapComponent(tree, dim);

                treeMap.zoomIn(node1);
                assertEquals(node1, treeMap.getTreeMapRoot());

                treeMap.zoomIn(node2);
                assertEquals(node1, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testZoomOut() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap = new TreeMapComponent(tree, dim);

                treeMap.zoomOut();
                assertEquals(tree, treeMap.getTreeMapRoot());

                treeMap.zoomIn(node1); //if zoom out root is tree
                treeMap.zoomIn(node2); //no-op, cannot zoom on leaf

                assertEquals(node1, treeMap.getTreeMapRoot());

                treeMap.zoomOut();
                assertEquals(tree, treeMap.getTreeMapRoot());
            }
        });
    }

    @Test
    public final void testZoomFull() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap = new TreeMapComponent(tree, dim);

                treeMap.zoomIn(node2);
                treeMap.zoomFull();
                assertEquals(tree, treeMap.getTreeMapRoot());

            }
        });
    }

    @Test
    public final void testGetZoomCallsStack() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap = new TreeMapComponent(tree, dim);

                // the root is always in the stack
                assertEquals(1, treeMap.getZoomCallsStack().size());

                treeMap.zoomIn(tree);
                // zooming on the same element nothing happen
                assertEquals(1, treeMap.getZoomCallsStack().size());

                treeMap.zoomIn(node1);
                treeMap.zoomIn(node2);
                treeMap.zoomFull();
                assertEquals(tree, treeMap.getTreeMapRoot());
            }
        });
    }


    @Test
    public final void testClearZoomCallsStack() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                treeMap = new TreeMapComponent(tree, dim);

                treeMap.clearZoomCallsStack();
                assertEquals(1, treeMap.getZoomCallsStack().size());

                treeMap.zoomIn(node1);
                treeMap.zoomIn(node2);
                treeMap.clearZoomCallsStack();
                assertEquals(1, treeMap.getZoomCallsStack().size());
            }
        });
    }
    
    @Test
    public final void testObserver() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            boolean zoomedIn = false;
            boolean zoomedOut = false;
            boolean zoomedFull = false;
            
            TreeMapObserver observer = new TreeMapObserver() {
                @Override
                public void notifyZoomOut() {
                    zoomedOut = true;
                }
                
                @Override
                public void notifyZoomIn(TreeMapNode node) {
                    zoomedIn = true;
                }
                
                @Override
                public void notifyZoomFull() {
                    zoomedFull = true;
                }
                
                @Override
                public void notifySelection(TreeMapNode node) {
                }
            };

            @Override
            public void run() {
                TreeMapNode child = new TreeMapNode(1);
                tree.addChild(child);
                TreeMapNode grandchild = new TreeMapNode(1);
                child.addChild(grandchild);

                treeMap = new TreeMapComponent(tree, dim);
                treeMap.register(observer);
                
                treeMap.zoomIn(child);
                assertTrue("Should have zoomed in on child", zoomedIn);
                zoomedIn = false;

                treeMap.zoomIn(grandchild);
                assertFalse("Should not have zoomed in on grandchild", zoomedIn);
                
                treeMap.zoomOut();
                assertTrue("Should have zoomed out", zoomedOut);
                
                treeMap.zoomIn(child);
                treeMap.zoomFull();
                assertTrue("Should have zoomed full", zoomedFull);
            }
        });
    }
}
