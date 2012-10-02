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

package com.redhat.thermostat.agent.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.cli.StorageCommand;
import com.redhat.thermostat.agent.cli.db.DBConfig;
import com.redhat.thermostat.agent.cli.db.DBStartupConfiguration;
import com.redhat.thermostat.agent.cli.db.MongoProcessRunner;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.tools.ApplicationState;

public class DBServiceTest {
    
    private static final String PORT = "27518";
    private static final String BIND = "127.0.0.1";
    private static final String PROTOCOL = "mongodb";
    private static final String DB = "storage/db";

    private String tmpDir;
    
    @Before
    public void setup() {
        // need to create a dummy config file for the test
        try {
            Random random = new Random();
            
            tmpDir = System.getProperty("java.io.tmpdir") + File.separatorChar +
                     Math.abs(random.nextInt()) + File.separatorChar;
            
            System.setProperty("THERMOSTAT_HOME", tmpDir);
            File base = new File(tmpDir + "storage");
            base.mkdirs();
                        
            File tmpConfigs = new File(base, "db.properties");
            
            new File(base, "run").mkdirs();
            new File(base, "logs").mkdirs();
            new File(base, "db").mkdirs();
            
            Properties props = new Properties();
            
            props.setProperty(DBConfig.BIND.name(), BIND);
            props.setProperty(DBConfig.PORT.name(), PORT);
            props.setProperty(DBConfig.PROTOCOL.name(), PROTOCOL);

            props.store(new FileOutputStream(tmpConfigs), "thermostat test properties");
            
        } catch (IOException e) {
            Assert.fail("cannot setup tests: " + e);
        }
    }
    
    @Test
    public void testConfig() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("quiet", null);
        args.addArgument("start", null);
        args.addArgument("dryRun", null);
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.getArguments()).thenReturn(args);

        StorageCommand service = new StorageCommand() {
            @Override
            MongoProcessRunner createRunner() {
                throw new AssertionError("dry run should never create an actual runner");
            }
        };

        service.run(ctx);
        
        DBStartupConfiguration conf = service.getConfiguration();
        
        Assert.assertEquals(tmpDir + DB, conf.getDBPath().getPath());
        Assert.assertEquals(Integer.parseInt(PORT), conf.getPort());
        Assert.assertEquals(PROTOCOL, conf.getProtocol());
    }
    
    private StorageCommand prepareService(boolean startSuccess) throws IOException,
            InterruptedException, InvalidConfigurationException, ApplicationException
    {
        final MongoProcessRunner runner = mock(MongoProcessRunner.class);
        if (!startSuccess) {
           doThrow(new ApplicationException("mock exception")).when(runner).startService();
        }
        
        // TODO: stop not tested yet, but be sure it's not called from the code
        doThrow(new ApplicationException("mock exception")).when(runner).stopService();
        
        StorageCommand service = new StorageCommand() {
            @Override
            MongoProcessRunner createRunner() {
                return runner;
            }
        };
        
        return service;
    }
    
    @Test
    public void testListeners() throws InterruptedException, IOException, ApplicationException, InvalidConfigurationException, CommandException
    {
        StorageCommand service = prepareService(true);
        
        final CountDownLatch latch = new CountDownLatch(2);
        
        final boolean[] result = new boolean[2];
        service.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                case FAIL:
                    result[0] = false;
                    latch.countDown();
                    latch.countDown();
                    break;
                    
                case SUCCESS:
                    result[0] = true;
                    latch.countDown();
                    break;

                case START:
                    result[1] = true;
                    latch.countDown();
                    break;
                }
            }
        });
        
        service.run(prepareContext());
        latch.await();
        
        Assert.assertTrue(result[0]);
        Assert.assertTrue(result[1]);
    }
    
    @Test
    public void testListenersFail() throws InterruptedException, IOException, ApplicationException, CommandException, InvalidConfigurationException
    {
        StorageCommand service = prepareService(false);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        service.getNotifier().addActionListener(new ActionListener<ApplicationState>() {
            @Override
            public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
                switch (actionEvent.getActionId()) {
                case FAIL:
                    result[0] = true;
                    break;
                    
                case SUCCESS:
                    result[0] = false;
                    break;
                }
                latch.countDown();
            }
        });
        
        service.run(prepareContext());
        latch.await();
        
        Assert.assertTrue(result[0]);
    }

    private CommandContext prepareContext() {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("quiet", "--quiet");
        args.addArgument("start", "--start");
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.getArguments()).thenReturn(args);
        return ctx;
    }

    @Test
    public void testName() {
        StorageCommand dbService = new StorageCommand();
        String name = dbService.getName();
        assertEquals("storage", name);
    }

    @Test
    public void testUsage() {
        StorageCommand dbService = new StorageCommand();
        String usage = dbService.getUsage();
        assertEquals("thermostat storage <--start|--stop>", usage);
    }

    @Test
    public void testOptions() {
        StorageCommand dbService = new StorageCommand();
        Options options = dbService.getOptions();
        assertNotNull(options);
        assertEquals(4, options.getOptions().size());

        assertTrue(options.hasOption("dryRun"));
        Option dry = options.getOption("dryRun");
        assertEquals("d", dry.getOpt());
        assertEquals("run the service in dry run mode", dry.getDescription());
        assertFalse(dry.isRequired());
        assertFalse(dry.hasArg());

        assertTrue(options.hasOption("start"));
        Option start = options.getOption("start");
        assertEquals("start the database", start.getDescription());
        assertFalse(start.isRequired());
        assertFalse(start.hasArg());

        assertTrue(options.hasOption("stop"));
        Option stop = options.getOption("stop");
        assertEquals("stop the database", stop.getDescription());
        assertFalse(stop.isRequired());
        assertFalse(stop.hasArg());

        assertTrue(options.hasOption("quiet"));
        Option quiet = options.getOption("quiet");
        assertEquals("q", quiet.getOpt());
        assertEquals("don't produce any output", quiet.getDescription());
        assertFalse(quiet.isRequired());
        assertFalse(quiet.hasArg());

        OptionGroup startStop = options.getOptionGroup(start);
        assertTrue(startStop.isRequired());
        @SuppressWarnings("unchecked")
        Collection<Option> groupOpts = startStop.getOptions();
        assertEquals(2, groupOpts.size());
        assertTrue(groupOpts.contains(start));
        assertTrue(groupOpts.contains(stop));
    }
}
