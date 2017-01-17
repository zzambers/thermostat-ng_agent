/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.storage.query;

/**
 * A {@link BinaryExpression} that represents a boolean formula
 * with two expressions joined by a logical operator.
 * @param <S> - the type of {@link Expression} corresponding to the left operand
 * @param <T> - the type of {@link Expression} corresponding to the right operand
 */
public final class BinaryLogicalExpression<S extends Expression, T extends Expression>
        extends BinaryExpression<S, T, BinaryLogicalOperator>
        implements LogicalExpression {

    /**
     * Constructs a {@link BinaryLogicalExpression} whose operands are
     * expressions joined by a {@link BinaryLogicalOperator}.
     * <p>
     * This constructor exists mainly for JSON serialization, use methods in
     * {@link ExpressionFactory} instead of this constructor.
     * @param leftOperand - left operand for this expression
     * @param operator - the operator for this expression
     * @param rightOperand - right operand for this expression
     */
    public BinaryLogicalExpression(S leftOperand, BinaryLogicalOperator operator, T rightOperand) {
        super(leftOperand, operator, rightOperand);
    }
    
}

