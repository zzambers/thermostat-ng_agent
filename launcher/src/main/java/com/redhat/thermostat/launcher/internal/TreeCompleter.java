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

import static java.util.Objects.requireNonNull;
import static jline.console.completer.ArgumentCompleter.ArgumentDelimiter;
import static jline.console.completer.ArgumentCompleter.ArgumentList;
import static jline.console.completer.ArgumentCompleter.WhitespaceArgumentDelimiter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

public class TreeCompleter implements Completer {

    private final ArgumentDelimiter delimiter;

    private final List<Node> branches;

    private Node currentNode;
    private ArgumentList list;
    private boolean alphabeticalSortingEnabled = false;

    public static final int NOT_FOUND = -1;
    private static final String EMPTY_SPACE = " ";
    private static final Node START_NODE = null;

    /**
     * This method adds a child branch to the start node
     * @param child a node containing a completer
     */
    public void addBranch(final Node child) {
        branches.add(child);
    }

    /**
     * This method adds child branches to the start node
     * @param branchList a list of nodes containing a completer each
     */
    public void addBranches(final List<Node> branchList) {
        for (final Node branch : branchList) {
            addBranch(branch);
        }
    }

    /**
     * @return the start node branches
     */
    public List<Node> getBranches() {
        return branches;
    }

    public TreeCompleter() {
        delimiter = new WhitespaceArgumentDelimiter();
        branches = new ArrayList<>();
    }

    /**
     * Sets whether the completions returned by the completer will be alphabetical
     * @param completeAlphabetically true will result in alphabetically sorted completions
     */
    public void setAlphabeticalCompletions(boolean completeAlphabetically) {
        alphabeticalSortingEnabled = completeAlphabetically;
    }

    /**
     * @return whether the completer will be alphabetically sorted
     */
    public boolean isAlphabeticalSortingEnabled() {
        return alphabeticalSortingEnabled;
    }

    /**
     * This method is called when attempting to tab complete
     * @param buffer the input that will be tab completed
     * @param cursorPosition the position of the cursorPosition within the buffer
     * @param candidates the list of possible completions will get filled when found
     * @return the new position of the cursorPosition, a return of NOT_FOUND means no completion
     * was found or completion is finished, resulting in no change of the cursorPosition position
     */
    @Override
    public int complete(final String buffer, final int cursorPosition, final List<CharSequence> candidates) {
        requireNonNull(candidates);

        if (cursorPosition > buffer.length()) {
            return NOT_FOUND;
        }
        final String currentBuffer = buffer.substring(0, cursorPosition);
        refreshCompleter();

        list = delimiter.delimit(currentBuffer, cursorPosition);
        if (list.getCursorArgumentIndex() < 0) {
            return NOT_FOUND;
        }

        int position = cursorPosition;
        currentNode = traverseBranches(currentBuffer, Arrays.asList(list.getArguments()));
        final List<Completer> completers = getAllCompleters(currentNode);

        //Complete possible arguments off a space or the current word up to the cursor
        if (currentBuffer.endsWith(EMPTY_SPACE)) {
            completeList(candidates, completers);
        } else {
            if (currentNode != START_NODE && currentNode.getBranches().isEmpty()) {
                currentNode = currentNode.getRestartNode();
            }
            final List<Completer> relevantCompleters = filterRelevantCompleters(completers, list.getCursorArgument());
            for (final Completer completer : relevantCompleters) {
                position = getInlinedCursorPosition(completer, candidates);
            }
        }

        if (alphabeticalSortingEnabled) {
            inplaceSortAlphabetically(candidates);
        }

        return position;
    }

    private void inplaceSortAlphabetically(List<CharSequence> candidates) {
        Collections.sort(candidates, new Comparator<CharSequence>() {
            @Override
            public int compare(final CharSequence t0, final CharSequence t1) {
                return t0.toString().compareTo(t1.toString());
            }
        });
    }

    private int getInlinedCursorPosition(final Completer completer, final List<CharSequence> candidates) {
        int cursor = completer.complete(list.getCursorArgument(), list.getArgumentPosition(), candidates);
        return cursor + list.getBufferPosition() - list.getArgumentPosition();
    }

    private void refreshCompleter() {
        currentNode = START_NODE;
    }

    private Node traverseBranches(final String currentBuffer, final List<String> arguments) {
        Node resultNode = START_NODE;
        for (final Iterator<String> it = arguments.iterator(); it.hasNext();) {
            final String arg = it.next();
            if (!it.hasNext() && !currentBuffer.endsWith(" ")) {
                //inline completion detected
                break;
            }
            final int branchIndex = getBranchIndex(arg, getPossibleMatches(resultNode));
            if (branchIndex != NOT_FOUND) {
                resultNode = findBranches(resultNode).get(branchIndex);
            }
        }
        return resultNode;
    }

    private void completeList(final List<CharSequence> candidates, final List<Completer> completerList) {
        for (final Completer completer : completerList) {
            completer.complete(null, 0, candidates);
        }
    }

    private List<Completer> getAllCompleters(final Node currentNode) {
        final List<Completer> completersFromBranches = new ArrayList<>();
        for (final Node node : findBranches(currentNode)) {
            completersFromBranches.add(node.getCompleter());
        }
        return completersFromBranches;
    }

    private List<Node> getChildNodesFromRestartNode(final Node node) {
        final List<Node> childrenNodeList;
        if (node.getRestartNode() == START_NODE) {
            childrenNodeList = getBranches();
        } else {
            childrenNodeList = node.getRestartNode().getBranches();
        }
        currentNode = node.getRestartNode();
        return childrenNodeList;
    }

    private List<Completer> filterRelevantCompleters(final List<Completer> completersFromBranches, final String cursorArgument) {
        final List<Completer> completers = new ArrayList<>();
        for (final Completer branchCompleter : completersFromBranches) {
            final List<CharSequence> candidates = new LinkedList<>();
            branchCompleter.complete(cursorArgument, 0, candidates);
            if (!candidates.isEmpty()) {
                completers.add(branchCompleter);
            }
        }
        return completers;
    }

    private List<CharSequence> findCompletions(final Completer branchCompleter) {
        final List<CharSequence> candidates = new LinkedList<>();
        branchCompleter.complete(null, 0, candidates);
        return candidates;
    }

    private int getBranchIndex(final String argument, final List<List<CharSequence>> listOfPossibleMatches) {
        for (final List<CharSequence> possibleMatches : listOfPossibleMatches) {
            for (final CharSequence word : possibleMatches) {
                if (word.toString().trim().equals(argument.trim())) {
                    return listOfPossibleMatches.indexOf(possibleMatches);
                }
            }
        }
        return NOT_FOUND;
    }

    private List<List<CharSequence>> getPossibleMatches(final Node node) {
        final List<List<CharSequence>> possibleMatches = new LinkedList<>();
        for (final Node branch : findBranches(node)) {
            possibleMatches.add(findCompletions(branch.getCompleter()));
        }
        return possibleMatches;
    }

    private List<Node> findBranches(final Node node) {
        if (node == START_NODE) {
            return getBranches();
        } else {
            if (node.getBranches().isEmpty()) {
                return getChildNodesFromRestartNode(node);
            }
            return node.getBranches();
        }
    }

    /**
     * A class to be used with the TreeCompleter
     * Each node contains a completer that will be used by the TreeCompleter
     * to check completion. Each node contains branches that are possible
     * paths for the completion to follow. Once completion has reached a node
     * with no branches it will use the restartNode to continue to find
     * any further completions.
     */
    public static class Node {
        private final Completer completer;
        private final List<Node> branches;
        private Node restartNode = START_NODE;

        public Node(final Completer data) {
            requireNonNull(data);
            this.completer = data;
            branches = new ArrayList<>();
        }

        public void addBranch(final Node branch) {
            branches.add(branch);
        }

        public Completer getCompleter() {
            return completer;
        }

        public List<Node> getBranches() {
            return branches;
        }

        public void setRestartNode(final Node restartNode) {
            this.restartNode = restartNode;
        }

        public Node getRestartNode() {
            return restartNode;
        }

    }

    /**
     * A helper method to quickly create a node containing a strings completer
     * @param strings the strings to be completed by the strings completer
     * @return the node containing the string completer
     */
    public static Node createStringNode(String... strings) {
        return new Node(new StringsCompleter(strings));
    }

    /**
     * A helper method to quickly create a node containing a strings completer
     * @param strings the strings to be completed by the strings completer
     * @return the node containing the string completer
     */
    public static Node createStringNode(List<String> strings) {
        return new Node(new StringsCompleter(strings));
    }
}
