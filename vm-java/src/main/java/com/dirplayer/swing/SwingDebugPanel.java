package com.dirplayer.swing;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.CastLib;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.score.Score;
import com.dirplayer.player.score.SpriteChannel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

/**
 * Debug panel showing sprite channels, cast members, and script execution info.
 */
public class SwingDebugPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final SwingPlayer swingPlayer;

    // Debug components
    private JTabbedPane tabbedPane;

    // Channels tab
    private JList<String> channelList;
    private DefaultListModel<String> channelListModel;

    // Cast tab
    private JTree castTree;
    private DefaultTreeModel castTreeModel;
    private DefaultMutableTreeNode castRootNode;

    // Variables tab
    private JTextArea variablesText;

    // Script tab
    private JTextArea scriptText;

    // Breakpoints tab
    private DefaultListModel<String> breakpointListModel;
    private JList<String> breakpointList;

    // Console tab
    private JTextArea consoleText;

    public SwingDebugPanel(SwingPlayer swingPlayer) {
        this.swingPlayer = swingPlayer;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Debug"));

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        // Create tabs
        tabbedPane.addTab("Channels", createChannelsTab());
        tabbedPane.addTab("Cast", createCastTab());
        tabbedPane.addTab("Variables", createVariablesTab());
        tabbedPane.addTab("Script", createScriptTab());
        tabbedPane.addTab("Breakpoints", createBreakpointsTab());
        tabbedPane.addTab("Console", createConsoleTab());

        add(tabbedPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createChannelsTab() {
        JPanel panel = new JPanel(new BorderLayout());

        channelListModel = new DefaultListModel<>();
        channelList = new JList<>(channelListModel);
        channelList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        channelList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = channelList.getSelectedIndex();
                if (index >= 0) {
                    swingPlayer.setDebugSelectedChannel(index + 1);
                } else {
                    swingPlayer.setDebugSelectedChannel(null);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(channelList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCastTab() {
        JPanel panel = new JPanel(new BorderLayout());

        castRootNode = new DefaultMutableTreeNode("Cast Libraries");
        castTreeModel = new DefaultTreeModel(castRootNode);
        castTree = new JTree(castTreeModel);
        castTree.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(castTree);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createVariablesTab() {
        JPanel panel = new JPanel(new BorderLayout());

        variablesText = new JTextArea();
        variablesText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        variablesText.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(variablesText);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createScriptTab() {
        JPanel panel = new JPanel(new BorderLayout());

        scriptText = new JTextArea();
        scriptText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        scriptText.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(scriptText);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBreakpointsTab() {
        JPanel panel = new JPanel(new BorderLayout());

        breakpointListModel = new DefaultListModel<>();
        breakpointList = new JList<>(breakpointListModel);
        breakpointList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(breakpointList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Breakpoint controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addBreakpoint());
        controlPanel.add(addButton);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeBreakpoint());
        controlPanel.add(removeButton);

        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> clearBreakpoints());
        controlPanel.add(clearButton);

        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createConsoleTab() {
        JPanel panel = new JPanel(new BorderLayout());

        consoleText = new JTextArea();
        consoleText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        consoleText.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(consoleText);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Console controls
        JPanel controlPanel = new JPanel(new BorderLayout());

        JTextField commandField = new JTextField();
        commandField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        commandField.addActionListener(e -> {
            String cmd = commandField.getText().trim();
            if (!cmd.isEmpty()) {
                executeCommand(cmd);
                commandField.setText("");
            }
        });
        controlPanel.add(commandField, BorderLayout.CENTER);

        JButton executeButton = new JButton("Execute");
        executeButton.addActionListener(e -> {
            String cmd = commandField.getText().trim();
            if (!cmd.isEmpty()) {
                executeCommand(cmd);
                commandField.setText("");
            }
        });
        controlPanel.add(executeButton, BorderLayout.EAST);

        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        JButton stepIntoButton = new JButton("Into");
        stepIntoButton.setToolTipText("Step Into (F11)");
        stepIntoButton.setMargin(new Insets(1, 4, 1, 4));
        stepIntoButton.addActionListener(e -> swingPlayer.debugStepInto());
        panel.add(stepIntoButton);

        JButton stepOverButton = new JButton("Over");
        stepOverButton.setToolTipText("Step Over (F10)");
        stepOverButton.setMargin(new Insets(1, 4, 1, 4));
        stepOverButton.addActionListener(e -> swingPlayer.debugStepOver());
        panel.add(stepOverButton);

        JButton stepOutButton = new JButton("Out");
        stepOutButton.setToolTipText("Step Out (Shift+F11)");
        stepOutButton.setMargin(new Insets(1, 4, 1, 4));
        stepOutButton.addActionListener(e -> swingPlayer.debugStepOut());
        panel.add(stepOutButton);

        return panel;
    }

    public void update(DirPlayer player) {
        updateChannelsTab(player);
        updateCastTab(player);
        updateVariablesTab(player);
    }

    private void updateChannelsTab(DirPlayer player) {
        channelListModel.clear();

        if (player == null) return;

        Movie movie = player.getMovie();
        if (movie == null || movie.score == null) return;

        Score score = movie.score;

        for (int i = 0; i < score.channels.size(); i++) {
            SpriteChannel channel = score.channels.get(i);
            Sprite sprite = channel.sprite;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%2d: ", i + 1));

            if (sprite != null && sprite.memberRef != null && sprite.memberRef.isValid()) {
                sb.append(String.format("(%d,%d) ", sprite.memberRef.getCastLib(), sprite.memberRef.getCastMember()));
                sb.append(String.format("@%d,%d ", sprite.locH, sprite.locV));
                if (sprite.puppet) {
                    sb.append("[P] ");
                }
                if (!sprite.visible) {
                    sb.append("[H] ");
                }
            } else {
                sb.append("(empty)");
            }

            channelListModel.addElement(sb.toString());
        }
    }

    private void updateCastTab(DirPlayer player) {
        castRootNode.removeAllChildren();

        if (player == null) {
            castTreeModel.reload();
            return;
        }

        Movie movie = player.getMovie();
        if (movie == null || movie.castManager == null) {
            castTreeModel.reload();
            return;
        }

        List<CastLib> casts = movie.castManager.casts;
        for (int i = 0; i < casts.size(); i++) {
            CastLib castLib = casts.get(i);

            DefaultMutableTreeNode castNode = new DefaultMutableTreeNode(
                String.format("Cast %d: %s (%d members)",
                    i + 1,
                    castLib.name.isEmpty() ? "(unnamed)" : castLib.name,
                    castLib.members.size()));

            for (CastMember member : castLib.members.values()) {
                String memberInfo = String.format("%d: %s [%s]",
                    member.number,
                    member.name != null && !member.name.isEmpty() ? member.name : "(unnamed)",
                    member.memberType);
                castNode.add(new DefaultMutableTreeNode(memberInfo));
            }

            castRootNode.add(castNode);
        }

        castTreeModel.reload();
    }

    private void updateVariablesTab(DirPlayer player) {
        if (player == null) {
            variablesText.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Global Variables ===\n");

        // Get global variables from player
        try {
            var globals = player.getGlobals();
            if (globals != null) {
                for (var entry : globals.entrySet()) {
                    sb.append(String.format("%s = %s\n", entry.getKey(), entry.getValue()));
                }
            }
        } catch (Exception e) {
            sb.append("(unable to read globals)\n");
        }

        variablesText.setText(sb.toString());
    }

    private void addBreakpoint() {
        String input = JOptionPane.showInputDialog(this,
            "Enter breakpoint location (script:handler:bytecode):",
            "Add Breakpoint",
            JOptionPane.PLAIN_MESSAGE);

        if (input != null && !input.trim().isEmpty()) {
            breakpointListModel.addElement(input.trim());
            // TODO: Actually add breakpoint to player
        }
    }

    private void removeBreakpoint() {
        int index = breakpointList.getSelectedIndex();
        if (index >= 0) {
            breakpointListModel.remove(index);
            // TODO: Actually remove breakpoint from player
        }
    }

    private void clearBreakpoints() {
        breakpointListModel.clear();
        // TODO: Actually clear breakpoints from player
    }

    private void executeCommand(String command) {
        appendConsole("> " + command);

        try {
            DirPlayer player = swingPlayer.getPlayer();
            if (player != null) {
                String result = player.evaluateLingo(command);
                if (result != null && !result.isEmpty()) {
                    appendConsole(result);
                }
            }
        } catch (Exception e) {
            appendConsole("Error: " + e.getMessage());
        }
    }

    public void appendConsole(String text) {
        consoleText.append(text + "\n");
        consoleText.setCaretPosition(consoleText.getDocument().getLength());
    }
}
