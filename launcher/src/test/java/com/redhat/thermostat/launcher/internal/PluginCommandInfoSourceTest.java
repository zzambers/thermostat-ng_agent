/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;
import com.redhat.thermostat.plugin.validator.PluginConfigurationValidatorException;

public class PluginCommandInfoSourceTest {

    private Path testRoot;
    private Path jarRootDir;
    private Path sysPluginRootDir;
    private Path userPluginRootDir;
    private PluginConfigurationParser parser;
    private PluginConfiguration parserResult;
    private UsageStringBuilder usageBuilder;

    @Before
    public void setUp() throws IOException, PluginConfigurationValidatorException {
        parser = mock(PluginConfigurationParser.class);
        parserResult = mock(PluginConfiguration.class);
        when(parser.parse(isA(File.class))).thenReturn(parserResult);
        usageBuilder = mock(UsageStringBuilder.class);

        testRoot = Files.createTempDirectory("thermostat");
        sysPluginRootDir = testRoot.resolve("plugins");
        Files.createDirectory(sysPluginRootDir);
        userPluginRootDir = testRoot.resolve("plugins-user");
        Files.createDirectory(userPluginRootDir);
        jarRootDir = testRoot.resolve("libs");
        Files.createDirectories(jarRootDir);
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(testRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }

    @Test
    public void verifyParserIsInvokedOnAllConfigurationFiles() throws IOException, PluginConfigurationValidatorException {
        Path[] pluginDirs = new Path[] {
                sysPluginRootDir.resolve("plugin1"),
                sysPluginRootDir.resolve("plugin2"),
        };
        for (Path pluginDir : pluginDirs) {
            Files.createDirectory(pluginDir);
        }

        new PluginCommandInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(), userPluginRootDir.toFile(), parser, usageBuilder);

        ArgumentCaptor<File> configFilesCaptor = ArgumentCaptor.forClass(File.class);
        verify(parser, times(pluginDirs.length)).parse(configFilesCaptor.capture());

        List<File> configurationFiles = configFilesCaptor.getAllValues();
        assertEquals(pluginDirs.length, configurationFiles.size());
        for (int i = 0; i < pluginDirs.length; i++) {
            assertTrue(configurationFiles.contains(pluginDirs[i].resolve("thermostat-plugin.xml").toFile()));
        }
    }

    @Test
    public void verifyMissingConfigurationFileIsHandledCorrectly() throws FileNotFoundException, PluginConfigurationValidatorException {
        when(parser.parse(isA(File.class))).thenThrow(new FileNotFoundException("test"));

        new PluginCommandInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(), userPluginRootDir.toFile(), parser, usageBuilder);
    }

    @Test(expected = CommandInfoNotFoundException.class)
    public void verifyMissingCommandInfo() {
        PluginCommandInfoSource source = new PluginCommandInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(), userPluginRootDir.toFile(), parser, usageBuilder);

        source.getCommandInfo("TEST");
    }

    @Test
    public void verifyCommandInfoObjectsToExtendExistingCommandsAreCreated() throws IOException {
        BundleInformation bundleInfo = new BundleInformation("plugin-bundle", "0.1");

        Path pluginDir = sysPluginRootDir.resolve("plugin1");
        Files.createDirectory(pluginDir);

        CommandExtensions extensions = mock(CommandExtensions.class);
        when(extensions.getCommandName()).thenReturn("command-name");
        when(extensions.getBundles()).thenReturn(Arrays.asList(bundleInfo));

        when(parserResult.getExtendedCommands()).thenReturn(Arrays.asList(extensions));

        PluginCommandInfoSource source = new PluginCommandInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(), userPluginRootDir.toFile(), parser, usageBuilder);

        CommandInfo info = source.getCommandInfo("command-name");
        assertEquals("command-name", info.getName());

        assertTrue(info.getBundles().contains(bundleInfo));
    }

    @Test
    public void verifyCommandInfoObjectsForNewComamndsAreCreated() throws IOException {
        final String NAME = "command-name";
        final String DESCRIPTION = "description of the command";
        final String USAGE = "usage";
        final Options OPTIONS = new Options();
        final Set<Environment> ENVIRONMENTS = EnumSet.of(Environment.SHELL);
        BundleInformation bundleInfo = new BundleInformation("plugin-bundle", "0.1");

        Path pluginDir = sysPluginRootDir.resolve("plugin1");
        Files.createDirectory(pluginDir);

        NewCommand cmd = mock(NewCommand.class);
        when(cmd.getCommandName()).thenReturn(NAME);
        when(cmd.getDescription()).thenReturn(DESCRIPTION);
        when(usageBuilder.getUsage(NAME, OPTIONS)).thenReturn(USAGE);
        when(cmd.getOptions()).thenReturn(OPTIONS);
        when(cmd.getEnvironments()).thenReturn(ENVIRONMENTS);
        when(cmd.getBundles()).thenReturn(Arrays.asList(bundleInfo));

        when(parserResult.getNewCommands()).thenReturn(Arrays.asList(cmd));

        PluginCommandInfoSource source = new PluginCommandInfoSource(jarRootDir.toFile(), sysPluginRootDir.toFile(), userPluginRootDir.toFile(), parser, usageBuilder);

        CommandInfo result = source.getCommandInfo(NAME);

        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(USAGE, result.getUsage());
        assertEquals(OPTIONS, result.getOptions());

        List<BundleInformation> deps = result.getBundles();
        assertEquals(1, deps.size());
        assertTrue(deps.contains(bundleInfo));
    }
}

