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

package com.redhat.thermostat.storage.core;

import java.io.InputStream;
import java.util.UUID;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * A storage can be used to store, query, update and remove data.
 * Implementations may use memory, a file, some database or even a network
 * server as the backing store.
 */
@Service
public interface Storage {

    void setAgentId(UUID id);

    String getAgentId();

    void registerCategory(Category<?> category);
    
    /**
     * Prepares the given statement for execution.
     * 
     * @param desc
     *            The statement descriptor to prepare.
     * @return A {@link PreparedStatement} if the given statement descriptor was
     *         known and did not fail to parse.
     * @throws DescriptorParsingException
     *             If the descriptor string failed to parse.
     * @throws IllegalDescriptorException
     *             If storage refused to prepare a statement descriptor for
     *             security reasons.
     */
    <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> desc)
            throws DescriptorParsingException;

    /**
     * Returns the Connection object that may be used to manage connections
     * to this Storage. Subsequent calls to this method should return
     * the same Connection.
     * @return the Connection for this Storage
     */
    Connection getConnection();

    Add createAdd(Category<?> category);
    Replace createReplace(Category<?> category);

    /**
     * Drop all data related to the specified agent.
     */
    void purge(String agentId);

    long getCount(Category<?> category);

    void saveFile(String filename, InputStream data);

    InputStream loadFile(String filename);

    Update createUpdate(Category<?> category);
    Remove createRemove();

    void shutdown();

}

