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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


/**
 *  This class implements the Squarified algorithm for TreeMaps. Using it, it is 
 *  possible to associate a rectangle to a {@link TreeMapNode} element and its
 *  children.
 *  <p>
 *  @see TreeMapNode
 *  @see TreMapBuilder
 */
public class SquarifiedTreeMap {

    /*
     * The algorithm this implements is described in detail here:
     *
     * https://bitbucket.org/Ammirate/thermostat-treemap/src/tip/TreeMap%20documentation.pdf
     *
     * Which is an improvement on:
     *
     * Mark Bruls, Kees Huizing and Jarke J. van Wijk, "Squarified
     * Treemaps" in Data Visualization 2000: Proceedings of the Joint
     * EUROGRAPHICS and IEEE TCVG Symposium on Visualization in Amsterdam,
     * The Netherlands, May 29–30, 2000. Berlin, Germany: Springer Science
     * & Business Media, 2012
     *
     * The paper itself is also available online at:
     * https://www.win.tue.nl/~vanwijk/stm.pdf
     */
    
    /**
     * List of node to represent as TreeMap.
     */
    private LinkedList<TreeMapNode> elements;

    private double totalRealWeight;

    /**
     * Represent the area in which draw nodes.
     */
    private Rectangle2D.Double container;
    
    private enum DIRECTION {
        LEFT_RIGHT,
        TOP_BOTTOM
    }

    /**
     * Indicates the drawing direction.
     */
    private DIRECTION drawingDir;

    /**
     * The rectangles area available for drawing.
     */
    private Rectangle2D.Double availableRegion;

    private double initialArea;

    /**
     * List of the calculated rectangles.
     */
    private List<TreeMapNode> squarifiedNodes;

    /**
     * List of the current rectangles under processing.
     */
    private List<TreeMapNode> currentRow;

    /**
     * Coordinates on which to draw.
     */
    private double lastX = 0;
    private double lastY = 0;

    public SquarifiedTreeMap(Rectangle2D.Double bounds, List<TreeMapNode> list) {
        this.elements = new LinkedList<>();
        this.elements.addAll(Objects.requireNonNull(list));
        this.totalRealWeight = getRealSum(elements);
        this.container = Objects.requireNonNull(bounds);
        this.squarifiedNodes = new ArrayList<>();
        this.currentRow = new ArrayList<>();
    }

    /**
     * This method prepares for and initiates the process of determining rectangles to represent
     * nodes.
     *
     * @return a list of nodes, each containing their respective rectangle.
     */
    public List<TreeMapNode> squarify() {
        if (elements.isEmpty()) {
            return Collections.emptyList();
        }

        initialArea = container.getWidth() * container.getHeight();
        availableRegion = new Rectangle2D.Double(container.getX(), container.getY(),
                container.getWidth(), container.getHeight());
        lastX = 0;
        lastY = 0;
        updateDirection();

        TreeMapNode.sort(elements);

        List<TreeMapNode> row = new ArrayList<>();
        squarifyHelper(elements, row, 0, getPrincipalSide());
        return getSquarifiedNodes();
    }

    /**
     * Recursively determine the rectangles that represent the set of nodes.
     *
     * @param nodes remaining nodes to be processed.
     * @param row the nodes that have been included in the row currently under construction.
     * @param rowArea the total area allocated to this row.
     * @param side the length of the side against which to calculate the the aspect ratio.
     */
    private void squarifyHelper(LinkedList<TreeMapNode> nodes, List<TreeMapNode> row,
                                double rowArea, double side) {

        if (nodes.isEmpty() && row.isEmpty()) {
            // nothing to do here, just return
            return;
        }
        if (nodes.isEmpty()) {
            // no more nodes to process, just finalize current row
            finalizeRow(row, rowArea);
            return;
        }
        if (row.isEmpty()) {
            // add the first element to the row and process any remaining nodes recursively
            row.add(nodes.getFirst());
            double realWeight = nodes.getFirst().getRealWeight();
            nodes.removeFirst();
            double nodeArea = (realWeight / totalRealWeight) * initialArea;
            squarifyHelper(nodes, row, nodeArea, side);
            return;
        }

        /*
         * Determine if adding another rectangle to the current row improves the overall aspect
         * ratio.  If the current row is not (and therefore cannot be) improved then it is
         * finalized, and the algorithm is run recursively on the remaining nodes that have not yet
         * been placed in a row.
         */
        List<TreeMapNode> expandedRow = new ArrayList<>(row);
        expandedRow.add(nodes.getFirst());
        double realWeight = nodes.getFirst().getRealWeight();
        double nodeArea = (realWeight / totalRealWeight) * initialArea;
        double expandedRowArea = rowArea + nodeArea;

        double actualAspectRatio = maxAspectRatio(row, rowArea, side);
        double expandedAspectRatio = maxAspectRatio(expandedRow, expandedRowArea, side);

        if (!willImprove(actualAspectRatio, expandedAspectRatio)) {
            finalizeRow(row, rowArea);
            squarifyHelper(nodes, new ArrayList<TreeMapNode>(), 0, getPrincipalSide());
        } else {
            nodes.removeFirst();
            squarifyHelper(nodes, expandedRow, expandedRowArea, side);
        }
    }

    /**
     * Return the rectangles list.
     * @return a list of rectangles.
     */
    public List<TreeMapNode> getSquarifiedNodes() {
        return squarifiedNodes;
    }

    /**
     * Recalculate the drawing direction.
     */
    private void updateDirection() {
        drawingDir = availableRegion.getWidth() > availableRegion.getHeight() ?
                DIRECTION.TOP_BOTTOM : DIRECTION.LEFT_RIGHT;
    }


    /**
     * Invert the drawing direction.
     */
    private void invertDirection() {
        drawingDir = drawingDir == DIRECTION.LEFT_RIGHT ? 
                DIRECTION.TOP_BOTTOM : DIRECTION.LEFT_RIGHT;
    }
    
    /**
     * For each node in the row, this method creates a rectangle to represent it graphically.
     *
     * @param row the set of nodes that constitute a row.
     * @param rowArea the area allocated to the row.
     */
    private void finalizeRow(List<TreeMapNode> row, double rowArea) {
        if (row == null || row.isEmpty()) {
            return;
        }

        // greedy optimization step: get the best aspect ratio for nodes drawn
        // on the longer and on the smaller side, to evaluate the best.
        double actualAR = maxAspectRatio(row, rowArea, getPrincipalSide());
        double alternativeAR = maxAspectRatio(row, rowArea, getSecondarySide());
      
        if (willImprove(actualAR, alternativeAR)) {
            invertDirection();
        }

        for (TreeMapNode node: row) {
            Rectangle2D.Double r = createRectangle(rowArea, node.getRealWeight() / getRealSum(row));
            node.setRectangle(r);
            
            // recalculate coordinates to draw next rectangle
            updateXY(r);

            // add the node to the current list of rectangles in processing
            currentRow.add(node);
        }
        // recalculate the area in which new rectangles will be drawn and
        // reinitialize the current list of node to represent.
        reduceAvailableArea();
        newRow();
    }
    
    /**
     * Create a rectangle that has a size determined by what fraction of the total row area is
     * allocated to it.
     *
     * @param rowArea the total area allocated to the row.
     * @param fraction the portion of the total area allocated to the rectangle being created.
     * @return the created rectangle.
     */
    private Rectangle2D.Double createRectangle(Double rowArea, Double fraction) {
        double side = getPrincipalSide();
        double w = 0;
        double h = 0;
        
        if (validate(fraction) == 0 || validate(rowArea) == 0 || validate(side) == 0) {
            return new Rectangle2D.Double(lastX, lastY, 0, 0);
        }

        if (drawingDir == DIRECTION.TOP_BOTTOM) {
            // the length of the secondary side (width here) of the rectangle is consistent between
            // rectangles in the row
            w = rowArea / side;

            // as the width is consistent, the length of the principal side (height here) of the
            // rectangle is proportional to the ratio rectangleArea / rowArea = fraction.
            h = fraction * side;
        } else {
            w = fraction * side;
            h = rowArea / side;
        }        
        return new Rectangle2D.Double(lastX, lastY, w, h);
    }
    
    /**
     * Check if a double value is defined as Not a Number and sets it to 0.
     * @param d the value to check.
     * @return the checked value: 0 if the given number is NaN, else the number
     * itself.
     */
    private double validate(double d) {
        if (d == Double.NaN) {
            d = 0;
        }
        return d;
    }

    /**
     * Check in which direction the rectangles have to be drawn.
     * @return the side on which rectangles will be created.
     */
    private double getPrincipalSide() {
        return drawingDir == DIRECTION.LEFT_RIGHT ? 
                availableRegion.getWidth() : availableRegion.getHeight();
    }

    /**
     * 
     * @return the secondary available area's side.
     */
    private double getSecondarySide() {
        return drawingDir == DIRECTION.LEFT_RIGHT ?
                availableRegion.getHeight() : availableRegion.getWidth();
    }

    private double getRealSum(List<TreeMapNode> nodes) {
        double sum = 0;
        for (TreeMapNode node : nodes) {
            sum += node.getRealWeight();
        }
        return sum;
    }

    /**
     * Recalculate the origin to draw next rectangles.
     * @param r the rectangle from which recalculate the origin.
     */
    private void updateXY(Rectangle2D.Double r) {
        if (drawingDir == DIRECTION.LEFT_RIGHT) {
            //lastY doesn't change
            lastX += r.width; 
        } else {
            //lastX doesn't change
            lastY += r.height;
        }
    }

    /**
     * Initialize the origin at the rectangle's origin.
     * @param r the rectangle used as origin source.
     */
    private void initializeXY(Rectangle2D.Double r) {
        lastX = r.x;
        lastY = r.y;
    }

    /**
     * Reduce the size of the available rectangle. Use it after the current 
     * row's closure.
     */
    private void reduceAvailableArea() {
        if (drawingDir == DIRECTION.LEFT_RIGHT) {
            // all rectangles inside the row have the same height
            availableRegion.height -= currentRow.get(0).getRectangle().height;
            availableRegion.y = lastY + currentRow.get(0).getRectangle().height;
            availableRegion.x = currentRow.get(0).getRectangle().x;
        } else {
            // all rectangles inside the row have the same width
            availableRegion.width -= currentRow.get(0).getRectangle().width;
            availableRegion.x = lastX + currentRow.get(0).getRectangle().width;
            availableRegion.y = currentRow.get(0).getRectangle().y;
        }
        updateDirection();
        initializeXY(availableRegion);
    }
    
    /**
     * Close the current row and initialize a new one.
     */
    private void newRow() {
        squarifiedNodes.addAll(currentRow);
        currentRow = new ArrayList<>();
    }

    /**
     * For each node in the row, determine the ratio longer side / shorter side of the rectangle
     * that would represent it.  Return the maximum ratio.
     *
     * @param row the list of nodes in this row.
     * @param rowArea the area allocated to this row.
     * @param side the length of the side against which to calculate the the aspect ratio.
     * @return the maximum ratio calculated for this row.
     */
    private double maxAspectRatio(List<TreeMapNode> row, double rowArea, double side) {
        if (row == null || row.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double realSum = getRealSum(row);
        double maxRatio = 0;

        for (TreeMapNode node : row) {
            double fraction = node.getRealWeight() / realSum;
            double length = rowArea / side;
            double width = fraction * side;
            double currentRatio = Math.max(length / width, width / length);

            if (currentRatio > maxRatio) {
                maxRatio = currentRatio;
            }
        }

        return maxRatio;
    }

    /**
     * This method check which from the values in input, that represent 
     * rectangles' aspect ratio, produces more approximatively a square.
     * It checks if one of the aspect ratio values gives a value nearest to 1 
     * against the other, which means that width and height are similar.
     * @param actualAR the actual aspect ratio
     * @param expandedAR the aspect ratio to evaluate
     * @return false if the actual aspect ratio is better than the new one, 
     * else true.
     */
    private boolean willImprove(double actualAR, double expandedAR) {
        if (actualAR == 0) {
            return true;
        }
        if (expandedAR == 0) {
            return false;
        }
        // check which value is closer to 1, the square's aspect ratio
        double v1 = Math.abs(actualAR - 1);
        double v2 = Math.abs(expandedAR - 1);       
        return v1 > v2;
    }
}
