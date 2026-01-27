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

        // Wire up EventDispatcher callbacks for script execution
        setupEventDispatcher();
    }

    /**
     * Set up EventDispatcher callbacks to connect it to DirPlayer script execution.
     */
    private void setupEventDispatcher() {
        // Provide player access to EventDispatcher
        player.eventDispatcher.setPlayerSupplier(() -> player);

        // Handler invoker - calls script handlers via DirPlayer
        player.eventDispatcher.setHandlerInvoker(invocation -> {
            try {
                // Get receiver as ScriptInstanceRef if provided
                com.dirplayer.player.script.ScriptInstanceRef receiver = invocation.instanceRef;

                // Call the script handler and get full result
                com.dirplayer.player.ScopeResult result = player.callScriptHandlerWithResult(
                    receiver,
                    invocation.handlerRef.scriptRef,
                    invocation.handlerRef.handlerName,
                    invocation.args
                );

                // Return result with proper passed flag
                return result.passed ?
                    com.dirplayer.player.events.EventResult.passed() :
                    com.dirplayer.player.events.EventResult.withResult(result.returnValue);
            } catch (com.dirplayer.player.ScriptError e) {
                // Handler not found is normal - just pass to next
                if (e.getMessage() != null && e.getMessage().contains("Handler not found")) {
                    return com.dirplayer.player.events.EventResult.passed();
                }
                System.err.println("Script error in handler: " + e.getMessage());
                e.printStackTrace();
                return com.dirplayer.player.events.EventResult.passed();
            }
        });

        // Datum handler invoker - calls handlers on datum objects
        player.eventDispatcher.setDatumHandlerInvoker(invocation -> {
            try {
                int result = player.callDatumHandler(
                    invocation.receiverRef,
                    invocation.handlerName,
                    invocation.args
                );
                return com.dirplayer.player.events.EventResult.withResult(result);
            } catch (com.dirplayer.player.ScriptError e) {
                // HandlerNotFound is expected for missing handlers
                return com.dirplayer.player.events.EventResult.passed();
            }
        });

        // Error handler
        player.eventDispatcher.setErrorHandler(err -> {
            System.err.println("Script error: " + err.getMessage());
            if (err.getCause() != null) {
                err.getCause().printStackTrace();
            }
        });
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

            // Dispatch startMovie event (matches Rust behavior)
            try {
                player.eventDispatcher.invokeGlobalEvent("startMovie", new java.util.ArrayList<>());
            } catch (Exception e) {
                System.err.println("startMovie error: " + e.getMessage());
            }

            // Dispatch beginSprite for initial sprites
            try {
                player.eventDispatcher.dispatchBeginSpriteEvent("beginSprite", new java.util.ArrayList<>());
            } catch (Exception e) {
                System.err.println("beginSprite error: " + e.getMessage());
            }

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
