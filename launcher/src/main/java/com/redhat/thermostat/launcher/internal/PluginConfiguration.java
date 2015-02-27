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

package com.redhat.thermostat.launcher.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.launcher.BundleInformation;


public class PluginConfiguration {

    private final List<CommandExtensions> extensions;
    private final List<NewCommand> newCommands;

    private final PluginID pluginID;
    private final Configurations configurations;

    public PluginConfiguration(List<NewCommand> newCommands, List<CommandExtensions> extensions, PluginID pluginID, Configurations config) {
        this.newCommands = newCommands;
        this.extensions = extensions;
        this.pluginID = pluginID;
        this.configurations = config;
    }

    public List<CommandExtensions> getExtendedCommands() {
        return extensions;
    }

    public List<NewCommand> getNewCommands() {
        return newCommands;
    }

    public PluginID getPluginID() {
        return this.pluginID;
    }

    public Configurations getConfigurations() {
        return this.configurations;
    }

    public boolean hasValidID() {
        return this.pluginID.isValidID();
    }

    public boolean isEmpty() {
        return this.configurations.isEmpty() && !this.pluginID.equals("");
    }

    public static class CommandExtensions {

        private final String commandName;
        private final List<BundleInformation> toLoad;

        public CommandExtensions(String name, List<BundleInformation> toLoad) {
            this.commandName = name;
            this.toLoad = toLoad;
        }

        public String getCommandName() {
            return commandName;
        }

        public List<BundleInformation> getBundles() {
            return toLoad;
        }

        @Override
        public String toString() {
            return "extends " + commandName + " using " + toLoad.toString();
        }
    }

    public static class NewCommand {

        private final String commandName;
        private final String usage;
        private final String summary;
        private final String description;
        private final List<String> positionalArguments;
        private final Options options;
        private final Set<Environment> environment;
        private final List<BundleInformation> bundles;
        private final boolean fileTabCompletionNeeded;

        public NewCommand(String name, String usage, String summary, String description,
                List<String> positionalArguments, Options options,
                Set<Environment> environment,
                List<BundleInformation> bundles,
                boolean fileTabCompletionNeeded) {
            this.commandName = name;
            this.usage = usage;
            this.summary = summary;
            this.description = description;
            this.positionalArguments = positionalArguments;
            this.options = options;
            this.environment = environment;
            this.bundles = bundles;
            this.fileTabCompletionNeeded = fileTabCompletionNeeded;
        }

        public String getCommandName() {
            return commandName;
        }

        /**
         * The usage string may be null if no usage string was explicitly
         * provided. In that case, usage should be "computed" using options and
         * arguments
         */
        public String getUsage() {
            return usage;
        }

        public String getSummary() {
            return summary;
        }

        public String getDescription() {
            return description;
        }

        /** Returns a list of strings indicating positional arguments */
        public List<String> getPositionalArguments() {
            return positionalArguments;
        }

        /** Returns options (both optional and required) */
        public Options getOptions() {
            return options;
        }

        /** Returns the environments where this command is available to be used */
        public Set<Environment> getEnvironments() {
            return Collections.unmodifiableSet(environment);
        }

        public List<BundleInformation> getBundles() {
            return Collections.unmodifiableList(bundles);
        }

        public boolean needsFileTabCompletions() {
            return fileTabCompletionNeeded;
        }
    }

    public static class PluginID {
        private final String pluginID;

        public PluginID(String pluginID) {
            this.pluginID = pluginID;
        }

        public String getPluginID() {
            return this.pluginID;
        }

        public boolean isValidID() {
            return !this.pluginID.equals("");
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PluginID)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            PluginID testObj = (PluginID) obj;
            if (this.pluginID.equals(testObj.getPluginID())) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.pluginID.hashCode();
        }
    }

    public static class Configurations {
        private Map<String, String> fileNames = new HashMap<String, String>();

        public Configurations(Map<String, String> fileNames) {
            this.fileNames = fileNames;
        }

        public static Configurations emptyConfigurations() {
            return new Configurations(Collections.<String, String>emptyMap());
        }
        public boolean containsFile(String fileName) {
            return fileNames.containsKey(fileName);
        }
        public boolean isEmpty() {
            return fileNames.isEmpty();
        }
        public String getFullFilePath(String fileName) {
            return fileNames.get(fileName);
        }
    }
}

