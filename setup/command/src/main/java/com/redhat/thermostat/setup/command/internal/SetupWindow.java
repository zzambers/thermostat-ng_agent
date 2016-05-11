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

package com.redhat.thermostat.setup.command.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;

import com.redhat.thermostat.client.swing.components.ThermostatEditorPane;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.client.swing.components.ThermostatTextArea;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.setup.command.internal.model.CredentialGenerator;
import com.redhat.thermostat.setup.command.internal.model.ThermostatQuickSetup;
import com.redhat.thermostat.setup.command.internal.model.ThermostatSetup;
import com.redhat.thermostat.shared.locale.Translate;

public class SetupWindow {
    private CountDownLatch shutdown;
    private JFrame frame;
    private JPanel mainView;
    private JPanel topPanel;
    private JLabel title;
    private JLabel progress;
    private StartView startView;
    private MongoUserSetupView mongoUserSetupView;
    private UserPropertiesView userPropertiesView;
    private SetupCompleteView setupCompleteView;
    private String storageUsername = null;
    private char[] storagePassword = null;
    private String clientUsername = null;
    private char[] clientPassword = null;
    private String agentUsername = null;
    private char[] agentPassword = null;
    private boolean showDetailedBlurb = false;
    private boolean setupCancelled = false;
    private final ThermostatSetup thermostatSetup;
    private SwingWorker<IOException, Void> finishAction;

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(SetupWindow.class);

    private static final int FRAME_WIDTH = 600;
    private static final int FRAME_HEIGHT = 400;
    private static final int FRAME_LARGE_HEIGHT = 600;

    public SetupWindow(ThermostatSetup thermostatSetup) {
        this.thermostatSetup = thermostatSetup;
        this.shutdown = new CountDownLatch(1);
    }

    public void run() throws CommandException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initialize();
                addViewListeners();
                showView(startView);
                frame.setVisible(true);
                startView.focusInitialComponent(); // cannot receive focus before everything is made visible
            }
        });

        try {
            shutdown.await();

            // Determine if we've finished successfully.
            if (finishAction != null) {
                IOException finishException = finishAction.get();
                if (finishException != null) {
                    logger.log(Level.INFO, "Setup failed.", finishException);
                    showErrorDialog(finishException);
                    throw new CommandException(translator.localize(LocaleResources.SETUP_FAILED), finishException);
                }
            } else if (setupCancelled) {
                logger.log(Level.INFO, "Setup was cancelled.");
                throw new CommandException(translator.localize(LocaleResources.SETUP_CANCELLED));
            }

            // if quick setup option was selected display
            // the setup complete window with user credentials
            if (startView.isQuickSetupSelected()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mainView.remove(startView);
                        showView(setupCompleteView);
                    }
                });
                shutdown = new CountDownLatch(1);
                shutdown.await();
            }
        } catch (InterruptedException | ExecutionException | InvocationTargetException e) {
            throw new CommandException(translator.localize(LocaleResources.SETUP_INTERRUPTED), e);
        } finally {
            // Explicitly dispose the window once we're done since we might have
            // intercepted another command and the window would otherwise
            // stay open.
            frame.dispose();
            cleanup();
        }
    }

    public static int getFrameWidth() {
        return FRAME_WIDTH;
    }
    
    private void showErrorDialog(final Exception e) throws InvocationTargetException, InterruptedException {
        doSynchronouslyOnEdt(new Runnable() {
            @Override
            public void run() {
                String reason = thermostatSetup.determineReasonFromException(e);
                ErrorDialog.createDialog(frame, reason, e).setVisible(true);
            }
        });
    }

    private void doSynchronouslyOnEdt(Runnable runnable) throws InvocationTargetException, InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeAndWait(runnable);
        }
    }

    private void cleanup() {
        if (storagePassword != null) {
            Arrays.fill(storagePassword, '\0');
        }
    }

    private void initialize() {
        frame = new JFrame(translator.localize(LocaleResources.WINDOW_TITLE).getContents());
        setLargeFrame(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelSetup();
            }
        });
        mainView = new JPanel(new BorderLayout());
        createTopPanel();
        mainView.add(topPanel, BorderLayout.NORTH);
        frame.add(mainView);

        startView = new StartView(new BorderLayout(), thermostatSetup);
        mongoUserSetupView = new MongoUserSetupView(new BorderLayout(), thermostatSetup);
        userPropertiesView = new UserPropertiesView(new BorderLayout());
        setupCompleteView = new SetupCompleteView(new BorderLayout());
    }

    private void createTopPanel() {
        title = new JLabel();
        title.setFont(new Font("Liberation Sans", Font.BOLD, 16));
        progress = new JLabel();

        topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets.left = 10;
        c.insets.top = 10;
        c.gridx = 0;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.LINE_START;
        topPanel.add(progress, c);
        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        topPanel.add(title, c);
    }

    private void addViewListeners() {
        startView.getNextBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showDetailedBlurb = false;
                startView.showMoreInfo(showDetailedBlurb);
                setLargeFrame(showDetailedBlurb);

                if (startView.isQuickSetupSelected()) {
                    runQuickSetup();
                } else {
                    mainView.remove(startView);
                    showView(mongoUserSetupView);
                }
            }
        });
        startView.getShowMoreInfoBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showDetailedBlurb = !showDetailedBlurb;
                startView.showMoreInfo(showDetailedBlurb);
                setLargeFrame(showDetailedBlurb);
            }
        });
        startView.getQuickSetupBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startView.setProgress(progress);
            }
        });
        startView.getCustomSetupBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startView.setProgress(progress);
            }
        });
        mongoUserSetupView.getBackBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainView.remove(mongoUserSetupView);
                showView(startView);
            }
        });
        mongoUserSetupView.getNextBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                storageUsername = mongoUserSetupView.getUsername();
                storagePassword = mongoUserSetupView.getPassword();

                if (thermostatSetup.isWebAppInstalled()) {
                    mainView.remove(mongoUserSetupView);
                    showView(userPropertiesView);
                    setLargeFrame(true);
                } else {
                    //webapp isn't installed so just run setup
                    //now to create mongodb user and quit
                    runSetup();
                }
            }
        });
        userPropertiesView.getBackBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainView.remove(userPropertiesView);
                showView(mongoUserSetupView);
                setLargeFrame(false);
            }
        });
        userPropertiesView.getFinishBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                agentUsername = userPropertiesView.getAgentUsername();
                agentPassword = userPropertiesView.getAgentPassword();
                clientUsername = userPropertiesView.getClientUsername();
                clientPassword = userPropertiesView.getClientPassword();
                runSetup();
            }
        });
        setupCompleteView.getFinishBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shutdown();
            }
        });

        ActionListener cancelButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cancelSetup();
            }
        };
        startView.getCancelBtn().addActionListener(cancelButtonListener);
        mongoUserSetupView.getCancelBtn().addActionListener(cancelButtonListener);
        userPropertiesView.getCancelBtn().addActionListener(cancelButtonListener);
    }

    private void setLargeFrame(boolean setLarge) {
        if (setLarge) {
            frame.setSize(FRAME_WIDTH, FRAME_LARGE_HEIGHT);
        } else {
            frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        }
    }

    private void runSetup() {
        finishAction = new SetupSwingWorker() {
            @Override
            void doSetup() throws IOException {
                thermostatSetup.createMongodbUser(storageUsername, storagePassword);
                if (thermostatSetup.isWebAppInstalled()) {
                    thermostatSetup.createAgentUser(agentUsername, agentPassword);
                    thermostatSetup.createClientAdminUser(clientUsername, clientPassword);
                }
                thermostatSetup.commit();
            }
        };
        finishAction.execute();
    }

    private void runQuickSetup() {
        final ThermostatQuickSetup quickSetup = new ThermostatQuickSetup(thermostatSetup);

        setupCompleteView.setClientCredentials(quickSetup.getClientUsername(), quickSetup.getClientPassword());
        setupCompleteView.setAgentCredentials(quickSetup.getAgentUsername(), quickSetup.getAgentPassword());

        finishAction = new SetupSwingWorker() {
            @Override
            void doSetup() throws IOException {
                quickSetup.run();
            }
        };
        finishAction.execute();
    }

    private void showView(SetupView view) {
        mainView.add(view.getUiComponent(), BorderLayout.CENTER);
        view.setTitle(title);
        view.setProgress(progress);
        view.setDefaultButton();
        view.focusInitialComponent();
        mainView.revalidate();
        mainView.repaint();
    }

    private void cancelSetup() {
        setupCancelled = true;
        shutdown();
    }

    private void shutdown() {
        shutdown.countDown();
    }

    private abstract class SetupSwingWorker extends SwingWorker<IOException, Void> {
        @Override
        public IOException doInBackground() {
            try {
                doSynchronouslyOnEdt(new Runnable() {
                    @Override
                    public void run() {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        startView.disableButtons();
                        mongoUserSetupView.disableButtons();
                        userPropertiesView.disableButtons();
                    }
                });
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }

            try {
                doSetup();
                return null;
            } catch (IOException e) {
                shutdown();
                return e;
            }
        }

        @Override
        public void done() {
            startView.enableButtons();
            mongoUserSetupView.enableButtons();
            userPropertiesView.enableButtons();
            frame.setCursor(Cursor.getDefaultCursor());
            shutdown();
        }

        abstract void doSetup() throws IOException;
    }

    private static class ErrorDialog extends JDialog {

        static JDialog createDialog(JFrame parent, String reason, Throwable throwable) {
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);

            final JButton showMoreInfoButton = createShowMoreInfoButton();
            final JScrollPane stackTracePane = createStackTracePane(throwable);
            stackTracePane.setVisible(false);
            final JTextComponent stepsToResolveText = createStepsToResolveText();
            stepsToResolveText.setVisible(false);

            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.add(stepsToResolveText);
            messagePanel.add(stackTracePane);

            JPanel infoButtonPanel = new JPanel();
            infoButtonPanel.setLayout(new BoxLayout(infoButtonPanel, BoxLayout.LINE_AXIS));
            infoButtonPanel.add(showMoreInfoButton);
            infoButtonPanel.add(Box.createHorizontalGlue());
            messagePanel.add(infoButtonPanel);

            optionPane.setMessage(new Object[] {
                    translator.localize(LocaleResources.SETUP_FAILED_DIALOG_MESSAGE, reason).getContents(),
                    messagePanel,
            });

            final JDialog dialog = optionPane.createDialog(parent, translator.localize(LocaleResources.SETUP_FAILED_DIALOG_TITLE).getContents());
            dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            showMoreInfoButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    boolean moreInfoVisible = stepsToResolveText.isVisible();
                    stepsToResolveText.setVisible(!moreInfoVisible);
                    stackTracePane.setVisible(!moreInfoVisible);
                    String buttonText;
                    if (stepsToResolveText.isVisible()) {
                        buttonText = translator.localize(LocaleResources.SHOW_LESS_ERROR_INFO).getContents();
                    } else {
                        buttonText = translator.localize(LocaleResources.SHOW_MORE_ERROR_INFO).getContents();
                    }
                    showMoreInfoButton.setText(buttonText);
                    dialog.pack();
                }
            });
            return dialog;
        }

        private static JButton createShowMoreInfoButton() {
            JButton button = new JButton();
            button.setText(translator.localize(LocaleResources.SHOW_MORE_ERROR_INFO).getContents());
            return button;
        }

        private static JScrollPane createStackTracePane(Throwable throwable) {
            ThermostatTextArea textArea = new ThermostatTextArea();
            textArea.setEditable(false);
            textArea.setText(stackTracetoString(throwable));
            return new ThermostatScrollPane(textArea);
        }

        private static String stackTracetoString(Throwable throwable) {
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            return stringWriter.toString();
        }

        private static JTextComponent createStepsToResolveText() {
            ThermostatEditorPane component = new ThermostatEditorPane();

            final String userGuideURL = new ApplicationInfo().getUserGuide();
            component.setEditorKit(ThermostatEditorPane.createEditorKitForContentType("text/html"));
            component.setEditable(false);
            component.setBackground(new Color(0, 0, 0, 0));
            component.setHighlighter(null);
            component.setText(translator.localize(LocaleResources.STEPS_TO_RESOLVE_ERROR_LABEL_TEXT, userGuideURL).getContents());
            component.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(new URI(userGuideURL));
                            } catch (IOException | URISyntaxException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            });

            return component;
        }

    }

}
