package com.dirplayer.swing;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.director.DirectorFile;
import com.dirplayer.io.BinaryReader;
import com.dirplayer.rendering.IntRect;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

/**
 * Swing-based desktop player for Director movies.
 * Provides the same features as the Rust/WASM player but in a native Java desktop application.
 */
public class SwingPlayer extends JFrame {
    private static final long serialVersionUID = 1L;

    // Core components
    private DirPlayer player;
    private SwingCanvas canvas;
    private SwingPlayerControls controls;
    private SwingDebugPanel debugPanel;
    private SwingSoundManager soundManager;

    // Playback state
    private Timer playbackTimer;
    private boolean isPlaying = false;
    private int frameRate = 15; // Default frame rate

    // Debug state
    private Integer debugSelectedChannel = null;
    private boolean showDebugInfo = false;

    // Window settings
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final String TITLE = "DirPlayer - Swing";

    public SwingPlayer() {
        super(TITLE);
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupPlaybackTimer();
        setupWindowListeners();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        player = new DirPlayer();
        canvas = new SwingCanvas(this);
        controls = new SwingPlayerControls(this);
        debugPanel = new SwingDebugPanel(this);
        soundManager = new SwingSoundManager(player);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Main canvas in center
        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setPreferredSize(new Dimension(640, 480));
        add(canvasScroll, BorderLayout.CENTER);

        // Controls at bottom
        add(controls, BorderLayout.SOUTH);

        // Debug panel on right (initially hidden)
        debugPanel.setPreferredSize(new Dimension(250, 0));
        debugPanel.setVisible(false);
        add(debugPanel, BorderLayout.EAST);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openFile());
        fileMenu.add(openItem);

        JMenuItem openUrlItem = new JMenuItem("Open URL...", KeyEvent.VK_U);
        openUrlItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
        openUrlItem.addActionListener(e -> openUrl());
        fileMenu.add(openUrlItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // Playback menu
        JMenu playbackMenu = new JMenu("Playback");
        playbackMenu.setMnemonic(KeyEvent.VK_P);

        JMenuItem playItem = new JMenuItem("Play", KeyEvent.VK_P);
        playItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        playItem.addActionListener(e -> togglePlayback());
        playbackMenu.add(playItem);

        JMenuItem stopItem = new JMenuItem("Stop", KeyEvent.VK_S);
        stopItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        stopItem.addActionListener(e -> stop());
        playbackMenu.add(stopItem);

        JMenuItem stepItem = new JMenuItem("Step Frame", KeyEvent.VK_F);
        stepItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        stepItem.addActionListener(e -> stepFrame());
        playbackMenu.add(stepItem);

        JMenuItem resetItem = new JMenuItem("Reset", KeyEvent.VK_R);
        resetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
        resetItem.addActionListener(e -> reset());
        playbackMenu.add(resetItem);

        playbackMenu.addSeparator();

        JMenuItem gotoFrameItem = new JMenuItem("Go to Frame...", KeyEvent.VK_G);
        gotoFrameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        gotoFrameItem.addActionListener(e -> gotoFrame());
        playbackMenu.add(gotoFrameItem);

        menuBar.add(playbackMenu);

        // Debug menu
        JMenu debugMenu = new JMenu("Debug");
        debugMenu.setMnemonic(KeyEvent.VK_D);

        JCheckBoxMenuItem showDebugPanelItem = new JCheckBoxMenuItem("Show Debug Panel");
        showDebugPanelItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
        showDebugPanelItem.addActionListener(e -> {
            debugPanel.setVisible(showDebugPanelItem.isSelected());
            revalidate();
        });
        debugMenu.add(showDebugPanelItem);

        JCheckBoxMenuItem showDebugInfoItem = new JCheckBoxMenuItem("Show Debug Info");
        showDebugInfoItem.addActionListener(e -> {
            showDebugInfo = showDebugInfoItem.isSelected();
            canvas.repaint();
        });
        debugMenu.add(showDebugInfoItem);

        debugMenu.addSeparator();

        JMenuItem stepIntoItem = new JMenuItem("Step Into");
        stepIntoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        stepIntoItem.addActionListener(e -> debugStepInto());
        debugMenu.add(stepIntoItem);

        JMenuItem stepOverItem = new JMenuItem("Step Over");
        stepOverItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
        stepOverItem.addActionListener(e -> debugStepOver());
        debugMenu.add(stepOverItem);

        JMenuItem stepOutItem = new JMenuItem("Step Out");
        stepOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK));
        stepOutItem.addActionListener(e -> debugStepOut());
        debugMenu.add(stepOutItem);

        menuBar.add(debugMenu);

        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenuItem actualSizeItem = new JMenuItem("Actual Size");
        actualSizeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK));
        actualSizeItem.addActionListener(e -> setCanvasScale(1.0));
        viewMenu.add(actualSizeItem);

        JMenuItem doubleItem = new JMenuItem("Double Size");
        doubleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK));
        doubleItem.addActionListener(e -> setCanvasScale(2.0));
        viewMenu.add(doubleItem);

        JMenuItem fitWindowItem = new JMenuItem("Fit to Window");
        fitWindowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        fitWindowItem.addActionListener(e -> canvas.setFitToWindow(true));
        viewMenu.add(fitWindowItem);

        menuBar.add(viewMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void setupPlaybackTimer() {
        playbackTimer = new Timer(1000 / frameRate, e -> tick());
        playbackTimer.setRepeats(true);
    }

    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
                soundManager.stopAll();
            }
        });
    }

    // ==================== File Operations ====================

    public void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Director Files (*.dir, *.dcr, *.dxr)", "dir", "dcr", "dxr"));
        chooser.setDialogTitle("Open Director Movie");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    public void openUrl() {
        String url = JOptionPane.showInputDialog(this,
            "Enter URL to Director movie:",
            "Open URL",
            JOptionPane.PLAIN_MESSAGE);

        if (url != null && !url.trim().isEmpty()) {
            loadUrl(url.trim());
        }
    }

    public void loadFile(File file) {
        try {
            stop();

            byte[] data = readFileBytes(file);
            String fileName = file.getName();
            URL basePath = file.getParentFile().toURI().toURL();

            // Enable synchronous mode for desktop loading (fetches external casts immediately)
            player.netManager.setSynchronousMode(true);
            player.netManager.setBasePath(basePath.toString());

            DirectorFile dirFile = DirectorFile.readBytes(data, fileName, basePath.toString());
            player.loadFromDirectorFile(dirFile);

            // Preload external casts (both phases)
            // MovieLoaded phase (preload mode 2 = before frame one)
            player.movie.castManager.loadFromDir(dirFile, player.netManager, player.bitmapManager, player.dirCache);
            // AfterFrameOne phase (preload mode 1 = after frame one) - fuse_client uses this
            player.movie.castManager.preloadCasts(
                com.dirplayer.player.CastManager.CastPreloadReason.AfterFrameOne,
                player.netManager, player.bitmapManager, player.dirCache);

            // Update frame rate from movie
            Movie movie = player.getMovie();
            if (movie != null && movie.frameRate > 0) {
                setFrameRate(movie.frameRate);
            }

            // Resize canvas to movie dimensions
            if (movie != null && movie.rect != null) {
                canvas.setMovieSize(movie.rect.width(), movie.rect.height());
            }

            // Update window title
            setTitle(TITLE + " - " + fileName);

            // Initialize sprites for frame 1 (matches Rust behavior)
            player.beginAllSprites();

            // Debug: print diagnostic info
            System.out.println("=== DIAGNOSTIC INFO ===");
            System.out.println("Frame: " + player.getMovie().currentFrame);
            System.out.println("Total frames: " + player.getMovie().score.totalFrames);
            System.out.println("Sprite spans: " + player.getMovie().score.spriteSpans.size());
            System.out.println("Channels: " + player.getMovie().score.channels.size());
            System.out.println("Channel init data: " + player.getMovie().score.channelInitializationData.size());

            // Print sprite spans
            for (var span : player.getMovie().score.spriteSpans) {
                System.out.println("  Span: channel=" + span.channelNumber + " frames " + span.startFrame + "-" + span.endFrame);
            }

            // Print channel init data
            for (var entry : player.getMovie().score.channelInitializationData) {
                System.out.println("  ChannelInitData: frameIdx=" + entry.frameIndex + " channelIdx=" + entry.channelIndex +
                    " castLib=" + entry.data.castLib + " castMember=" + entry.data.castMember +
                    " pos=(" + entry.data.posX + "," + entry.data.posY + ") size=" + entry.data.width + "x" + entry.data.height);
            }

            // Check sorted channels for frame 1
            java.util.List<Integer> sortedChannels = player.getMovie().score.getSortedChannelNumbers(1);
            System.out.println("Sorted channels for frame 1: " + sortedChannels);

            // Check first few channels after beginAllSprites
            for (int i = 0; i < Math.min(10, player.getMovie().score.channels.size()); i++) {
                var channel = player.getMovie().score.channels.get(i);
                var sprite = channel.sprite;
                System.out.println("Channel " + channel.number + ": memberRef=" +
                    (sprite.memberRef != null ? sprite.memberRef.getCastLib() + ":" + sprite.memberRef.getCastMember() : "null") +
                    " visible=" + sprite.visible + " entered=" + sprite.entered + " puppet=" + sprite.puppet +
                    " loc=(" + sprite.locH + "," + sprite.locV + ") size=" + sprite.width + "x" + sprite.height);
            }

            // Check cast members
            System.out.println("Cast libraries: " + player.getMovie().castManager.casts.size());
            for (var cast : player.getMovie().castManager.casts) {
                if (cast.members.size() > 0) {
                    System.out.println("  Cast " + cast.number + " (" + cast.name + "): " + cast.members.size() + " members, state=" + cast.state);
                }
            }

            // TEST: Manually set up a sprite to verify rendering works
            // Find the first bitmap member and display it
            com.dirplayer.player.CastMember testBitmap = null;
            com.dirplayer.player.CastMemberRef testRef = null;
            for (var cast : player.getMovie().castManager.casts) {
                for (var member : cast.members.values()) {
                    if (member.memberType == com.dirplayer.director.MemberType.Bitmap && member.bitmap != null) {
                        testBitmap = member;
                        testRef = new com.dirplayer.player.CastMemberRef(cast.number, member.number);
                        System.out.println("TEST: Using bitmap member " + cast.number + ":" + member.number +
                            " (" + member.name + ") " + member.bitmapWidth + "x" + member.bitmapHeight);
                        break;
                    }
                }
                if (testBitmap != null) break;
            }

            if (testBitmap != null && testRef != null) {
                // Set up sprite 1 to show this bitmap
                var sprite = player.getMovie().score.getSprite((short) 1);
                if (sprite != null) {
                    sprite.puppet = true;  // Make it puppeted so it renders
                    sprite.visible = true;
                    sprite.memberRef = testRef;
                    sprite.locH = 100 + testBitmap.regPointX;
                    sprite.locV = 100 + testBitmap.regPointY;
                    sprite.width = testBitmap.bitmapWidth;
                    sprite.height = testBitmap.bitmapHeight;
                    sprite.ink = 36;  // Background transparent
                    sprite.blend = 100;
                    System.out.println("TEST: Set sprite 1 to show bitmap at (100,100)");
                    System.out.println("TEST: sprite.puppet=" + sprite.puppet + " visible=" + sprite.visible +
                        " memberRef=" + sprite.memberRef.getCastLib() + ":" + sprite.memberRef.getCastMember() +
                        " isValid=" + sprite.memberRef.isValid());

                    // Check if it now appears in sorted channels
                    java.util.List<Integer> sortedAfter = player.getMovie().score.getSortedChannelNumbers(1);
                    System.out.println("TEST: Sorted channels after setup: " + sortedAfter);

                    // Check the bitmap itself
                    var bitmap = player.getBitmapManager().getBitmap(testBitmap.bitmap.bitmapId);
                    if (bitmap != null) {
                        System.out.println("TEST: Bitmap found in manager: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                            " data.length=" + bitmap.data.length);
                    } else {
                        System.out.println("TEST: ERROR - Bitmap NOT found in manager!");
                    }
                }
            }

            System.out.println("=== END DIAGNOSTIC ===");

            // Render first frame
            renderFrame();

            // Force debug panel to refresh
            debugPanel.forceFullUpdate(player);
            updateControls();

            System.out.println("Loaded: " + fileName);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load file: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void loadUrl(String url) {
        try {
            stop();

            // Enable synchronous mode for desktop loading
            player.netManager.setSynchronousMode(true);

            player.loadMovieFromFile(url, false);

            Movie movie = player.getMovie();
            if (movie != null && movie.frameRate > 0) {
                setFrameRate(movie.frameRate);
            }

            if (movie != null && movie.rect != null) {
                canvas.setMovieSize(movie.rect.width(), movie.rect.height());
            }

            setTitle(TITLE + " - " + url);

            // Initialize sprites for frame 1 (matches Rust behavior)
            player.beginAllSprites();

            renderFrame();
            debugPanel.forceFullUpdate(player);
            updateControls();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load URL: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private byte[] readFileBytes(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }

    // ==================== Playback Control ====================

    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            player.play();
            playbackTimer.start();
            updateControls();
        }
    }

    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            player.stop();
            playbackTimer.stop();
            updateControls();
        }
    }

    public void togglePlayback() {
        if (isPlaying) {
            stop();
        } else {
            play();
        }
    }

    public void stepFrame() {
        stop();
        player.step();
        renderFrame();
        updateControls();
    }

    public void reset() {
        stop();
        player.reset();
        renderFrame();
        updateControls();
    }

    public void gotoFrame() {
        String input = JOptionPane.showInputDialog(this,
            "Enter frame number:",
            "Go to Frame",
            JOptionPane.PLAIN_MESSAGE);

        if (input != null && !input.trim().isEmpty()) {
            try {
                int frame = Integer.parseInt(input.trim());
                player.goToFrame(frame);
                renderFrame();
                updateControls();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid frame number",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setFrameRate(int fps) {
        this.frameRate = fps;
        playbackTimer.setDelay(1000 / fps);
    }

    private void tick() {
        try {
            player.tick();
            renderFrame();
            updateControls();

            // Handle sounds
            soundManager.update();

        } catch (Exception e) {
            System.err.println("Tick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderFrame() {
        canvas.render(player, debugSelectedChannel, showDebugInfo);
    }

    private void updateControls() {
        controls.updateState(isPlaying, player.getMovie());
        debugPanel.update(player);
    }

    // ==================== Debug Operations ====================

    public void debugStepInto() {
        player.debugStepInto();
        renderFrame();
        updateControls();
    }

    public void debugStepOver() {
        player.debugStepOver();
        renderFrame();
        updateControls();
    }

    public void debugStepOut() {
        player.debugStepOut();
        renderFrame();
        updateControls();
    }

    public void setDebugSelectedChannel(Integer channel) {
        this.debugSelectedChannel = channel;
        renderFrame();
    }

    // ==================== View Operations ====================

    public void setCanvasScale(double scale) {
        canvas.setScale(scale);
        canvas.setFitToWindow(false);
        pack();
    }

    // ==================== Input Handling ====================

    public void handleMousePressed(int x, int y, int button) {
        player.handleMouseDown(x, y, button);
    }

    public void handleMouseReleased(int x, int y, int button) {
        player.handleMouseUp(x, y, button);
    }

    public void handleMouseMoved(int x, int y) {
        player.handleMouseMove(x, y);
    }

    public void handleKeyPressed(int keyCode, char keyChar) {
        player.handleKeyDown(keyCode, keyChar);
    }

    public void handleKeyReleased(int keyCode, char keyChar) {
        player.handleKeyUp(keyCode, keyChar);
    }

    // ==================== Utility ====================

    public DirPlayer getPlayer() {
        return player;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "DirPlayer Swing\n\n" +
            "A Java-based Shockwave/Director Player\n" +
            "Ported from the Rust implementation\n\n" +
            "Supports Director 4-12 movies (.dir, .dcr, .dxr)",
            "About DirPlayer",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== Main Entry Point ====================

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignore, use default
        }

        // Create and show player on EDT
        SwingUtilities.invokeLater(() -> {
            SwingPlayer player = new SwingPlayer();
            player.setVisible(true);

            // Load file from command line if provided
            if (args.length > 0) {
                File file = new File(args[0]);
                if (file.exists()) {
                    player.loadFile(file);
                }
            }
        });
    }
}
