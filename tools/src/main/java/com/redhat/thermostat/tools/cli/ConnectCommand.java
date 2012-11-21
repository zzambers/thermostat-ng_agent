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

package com.redhat.thermostat.tools.cli;

import com.redhat.thermostat.common.DbService;
import com.redhat.thermostat.common.DbServiceFactory;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.launcher.CommonCommandOptions;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.tools.LocaleResources;
import com.redhat.thermostat.utils.keyring.Keyring;

/**
 * Command in order to persistently connect to a database. Available only in
 * shell.
 * 
 * This commands registers a connection service. If this service is available,
 * it can be used to retrieve a DB connection.
 * 
 */
public class ConnectCommand extends SimpleCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String NAME = "connect";
    
    private ClientPreferences prefs;
    private DbServiceFactory dbServiceFactory;

    public ConnectCommand() {
        this(new DbServiceFactory());
    }

    ConnectCommand(DbServiceFactory dbServiceFactory) {
        this.dbServiceFactory = dbServiceFactory;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        DbService service = OSGIUtils.getInstance().getServiceAllowNull(DbService.class);
        if (service != null) {
            // Already connected, bail out
            throw new CommandException(translator.localize(LocaleResources.COMMAND_CONNECT_ALREADY_CONNECTED, service.getConnectionUrl()));
        }
        if (prefs == null) {
            prefs = new ClientPreferences(OSGIUtils.getInstance().getService(Keyring.class));
        }
        String dbUrl = ctx.getArguments().getArgument(CommonCommandOptions.DB_URL_ARG);
        if (dbUrl == null) {
            dbUrl = prefs.getConnectionUrl();
        }
        String username = ctx.getArguments().getArgument(CommonCommandOptions.USERNAME_ARG);
        String password = ctx.getArguments().getArgument(CommonCommandOptions.PASSWORD_ARG);
        try {
            // may throw StorageException if storage url is not supported
            service = dbServiceFactory.createDbService(username, password, dbUrl);
            service.connect();
        } catch (StorageException ex) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_CONNECT_INVALID_STORAGE, dbUrl));
        } catch (ConnectionException ex) {
            String error = ex.getMessage();
            String message = ( error == null ? "" : " " + translator.localize(LocaleResources.COMMAND_CONNECT_ERROR, error) );
            throw new CommandException(translator.localize(LocaleResources.COMMAND_CONNECT_FAILED_TO_CONNECT, dbUrl + message), ex);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public boolean isAvailableOutsideShell() {
        return false;
    }
    
    @Override
    public boolean isStorageRequired() {
        return false;
    }
}
