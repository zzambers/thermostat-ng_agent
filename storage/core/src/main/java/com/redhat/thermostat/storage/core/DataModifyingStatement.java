package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;

/**
 * Marker interface for {@link Statement}s which perform write operations on
 * storage. These statements usually only return success/failure responses or
 * more specific error codes.
 *
 */
public interface DataModifyingStatement<T extends Pojo> extends Statement<T> {

    /**
     * Executes this statement.
     * 
     * @return Zero on success. A non-zero failure code otherwise.
     */
    int execute();
}