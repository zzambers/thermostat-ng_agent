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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.common.cli.CompletionFinderTabCompleter;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.utils.LoggingUtils;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import org.apache.commons.cli.Option;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.redhat.thermostat.launcher.internal.TreeCompleter.createStringNode;

public class TabCompletion {

    private static final String LONG_OPTION_PREFIX = "--";
    private static final String SHORT_OPTION_PREFIX = "-";

    private Logger log;
    private TreeCompleter treeCompleter;
    private Map<String, TreeCompleter.Node> commandMap;

    public TabCompletion() {
        this(LoggingUtils.getLogger(TabCompletion.class),
                new TreeCompleter(),
                new HashMap<String, TreeCompleter.Node>());
    }

    /*
     * Testing only
     */
    TabCompletion(Logger log, TreeCompleter treeCompleter, Map<String, TreeCompleter.Node> commandMap) {
        this.log = log;
        this.treeCompleter = treeCompleter;
        this.commandMap = commandMap;

        treeCompleter.setAlphabeticalCompletions(true);
    }

    public void addCompleterService(CompleterService service) {
        if (commandMap.isEmpty()) {
            return;
        }
        for (String commandName : service.getCommands()) {
            TreeCompleter.Node command = commandMap.get(commandName);
            if (command == null) {
                log.info("Completer service for command \"" + commandName + "\" was attempted to be registered, but " +
                        "this command is not recognized.");
                continue;
            }
            addCompleterServiceImpl(command, service);
        }
    }

    private void addCompleterServiceImpl(TreeCompleter.Node command, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            for (TreeCompleter.Node branch : command.getBranches()) {
                Set<String> completerOptions = getCompleterOptions(entry.getKey());
                if (completerOptions.contains(branch.getTag())) {
                    TreeCompleter.Node node = new TreeCompleter.Node(command.getTag() + " completer", entry.getValue());
                    node.setRestartNode(command);
                    branch.addBranch(node);
                }
            }
        }
    }

    public void removeCompleterService(CompleterService service) {
        if (commandMap.isEmpty()) {
            return;
        }
        for (String commandName : service.getCommands()) {
            TreeCompleter.Node command = commandMap.get(commandName);
            if (command == null) {
                log.info("Completer service for command \"" + commandName + "\" was attempted to be unregistered, but " +
                        "this command is not recognized.");
                continue;
            }
            removeCompleterServiceImpl(command, service);
        }
    }

    private void removeCompleterServiceImpl(TreeCompleter.Node command, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            for (TreeCompleter.Node branch : command.getBranches()) {
                Set<String> completerOptions = getCompleterOptions(entry.getKey());
                if (completerOptions.contains(branch.getTag())) {
                    command.removeByTag(branch.getTag());
                }
            }
        }
    }

    private static Set<String> getCompleterOptions(CliCommandOption option) {
        Set<String> options = new HashSet<>();
        options.add(LONG_OPTION_PREFIX + option.getLongOpt());
        options.add(SHORT_OPTION_PREFIX + option.getOpt());
        return options;
    }

    public void setupTabCompletion(ConsoleReader reader, CommandInfoSource commandInfoSource, BundleContext context, ClientPreferences prefs) {
        List<String> logLevels = new ArrayList<>();

        for (LoggingUtils.LogLevel level : LoggingUtils.LogLevel.values()) {
            logLevels.add(level.getLevel().getName());
        }

        for (CommandInfo info : commandInfoSource.getCommandInfos()) {

            if (info.getEnvironments().contains(Environment.SHELL)) {
                String commandName = info.getName();
                TreeCompleter.Node command = createStringNode(commandName);
                commandMap.put(commandName, command);

                /* FIXME: the Ping command should be provided by a plugin and have a thermostat-plugin.xml of its own,
                * and in thermostat-plugin.xmls we should also somehow be able to define custom tab completions, including
                * for the no-opt arg case (such as this). When this is possible then this hard-coded completion installation
                * should be replaced with the proper custom ping-plugin custom completion setup.
                * http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=2876
                * http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=2877
                */
                if (commandName.equals("ping")) {
                    TreeCompleter.Node agentIds =
                            new TreeCompleter.Node("agentId", new CompletionFinderTabCompleter(new AgentIdsFinder(context)));
                    agentIds.setRestartNode(command);
                    command.addBranch(agentIds);
                    treeCompleter.addBranch(command);
                    continue;
                }

                for (Option option : (Collection<Option>) info.getOptions().getOptions()) {
                    if (option.getLongOpt().equals("logLevel")) {
                        setupCompletion(command, option, new JLineStringsCompleter(logLevels));
                    } else if (option.getLongOpt().equals("vmId")) {
                        setupCompletion(command, option, new CompletionFinderTabCompleter(new VmIdsFinder(context)));
                    } else if (option.getLongOpt().equals("agentId")) {
                        setupCompletion(command, option, new CompletionFinderTabCompleter(new AgentIdsFinder(context)));
                    } else if (option.getLongOpt().equals(Arguments.DB_URL_ARGUMENT)) {
                        setupCompletion(command, option, new DbUrlCompleter(prefs));
                    } else {
                        setupDefaultCompletion(command, option);
                    }

                }

                if (info.needsFileTabCompletions()) {
                    TreeCompleter.Node files = new TreeCompleter.Node("fileName", new JLineFileNameCompleter());
                    files.setRestartNode(command);
                    command.addBranch(files);
                }
                treeCompleter.addBranch(command);
            }
        }

        reader.addCompleter(new JLineCompleterAdapter(treeCompleter));
    }

    private void setupDefaultCompletion(final TreeCompleter.Node command, final Option option) {
        setupDefaultCompletion(command, option.getLongOpt(), LONG_OPTION_PREFIX);
        setupDefaultCompletion(command, option.getOpt(), SHORT_OPTION_PREFIX);
    }

    private void setupDefaultCompletion(final TreeCompleter.Node command, final String option, final String prefix) {
        if (option != null) {
            String optionShortName = prefix + option;
            TreeCompleter.Node defaultNode = createStringNode(optionShortName);
            defaultNode.setRestartNode(command);
            command.addBranch(defaultNode);
        }
    }

    private void setupCompletion(final TreeCompleter.Node command, final Option option, TabCompleter completer) {
        setupCompletion(command, completer, option.getLongOpt(), LONG_OPTION_PREFIX);
        setupCompletion(command, completer, option.getOpt(), SHORT_OPTION_PREFIX);
    }

    private void setupCompletion(final TreeCompleter.Node command, final TabCompleter completer, final String option, final String prefix) {
        if (option != null) {
            final String optionName = prefix + option;
            TreeCompleter.Node nodeOption = setupCompletionNode(command, optionName, completer);
            command.addBranch(nodeOption);
        }
    }

    private TreeCompleter.Node setupCompletionNode(final TreeCompleter.Node command, final String optionName, TabCompleter completer) {
        TreeCompleter.Node option = createStringNode(optionName);
        TreeCompleter.Node choices = new TreeCompleter.Node(optionName, completer);
        option.addBranch(choices);
        option.setRestartNode(command);
        choices.setRestartNode(command);
        return option;
    }

    private static class JLineCompleterAdapter implements Completer {

        private TabCompleter tabCompleter;

        public JLineCompleterAdapter(TabCompleter tabCompleter) {
            this.tabCompleter = tabCompleter;
        }

        @Override
        public int complete(String s, int i, List<CharSequence> list) {
            return tabCompleter.complete(s, i, list);
        }
    }

}
