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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.storage.model.Pojo;

public class QueuedStorage implements Storage {

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 3;

    protected final Storage delegate;
    protected final ExecutorService executor;
    protected final ExecutorService fileExecutor;

    /*
     * NOTE: We intentially use single-thread executor. All updates are put into a queue, from which
     * a single dispatch thread calls the underlying storage. Using multiple dispatch threads
     * could cause out-of-order issues, e.g. a VM death being reported before its VM start, which
     * could confuse the heck out of clients.
     */
    public QueuedStorage(Storage delegate) {
        this(delegate, Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
    }

    /*
     * This is here solely for use by tests.
     */
    QueuedStorage(Storage delegate, ExecutorService executor, ExecutorService fileExecutor) {
        this.delegate = delegate;
        this.executor = executor;
        this.fileExecutor = fileExecutor;
    }

    ExecutorService getExecutor() {
        return executor;
    }

    ExecutorService getFileExecutor() {
        return fileExecutor;
    }

    @Override
    public void purge(final String agentId) {

        executor.execute(new Runnable() {
            
            @Override
            public void run() {
                delegate.purge(agentId);
            }

        });

    }

    @Override
    public void saveFile(final String filename, final InputStream data) {

        fileExecutor.execute(new Runnable() {
            
            @Override
            public void run() {
                delegate.saveFile(filename, data);
            }

        });

    }

    @Override
    public InputStream loadFile(String filename) {
        return delegate.loadFile(filename);
    }

    @Override
    public void registerCategory(final Category<?> category) {
        delegate.registerCategory(category);
    }
    
    @Override
    public <T extends Pojo> PreparedStatement<T> prepareStatement(final StatementDescriptor<T> desc)
            throws DescriptorParsingException {
        return delegate.prepareStatement(desc);
    }

    @Override
    public Connection getConnection() {
        return delegate.getConnection();
    }

    @Override
    public void shutdown() {
        /*
         * First shut down executors. This may trigger some pushes to the
         * storage implementation (a.k.a. delegate). Hence, this should get
         * shut down last as this closes the connection etc.
         */
        try {
            executor.shutdown();
            executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // Fall through. 
        }
        try {
            fileExecutor.shutdown();
            fileExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // Fall through. 
        }
        delegate.shutdown();
    }

}

