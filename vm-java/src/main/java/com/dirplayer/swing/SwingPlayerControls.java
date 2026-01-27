package com.dirplayer.swing;

import com.dirplayer.player.Movie;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Control panel for playback controls (play, stop, step, etc.)
 */
public class SwingPlayerControls extends JPanel {
    private static final long serialVersionUID = 1L;

    private final SwingPlayer swingPlayer;

    // Control buttons
    private JButton playButton;
    private JButton stopButton;
    private JButton stepButton;
    private JButton resetButton;

    // Frame slider
    private JSlider frameSlider;
    private JLabel frameLabel;
    private boolean sliderAdjusting = false;

    // Info labels
    private JLabel statusLabel;
    private JLabel fpsLabel;

    public SwingPlayerControls(SwingPlayer swingPlayer) {
        this.swingPlayer = swingPlayer;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create sub-panels
        JPanel buttonPanel = createButtonPanel();
        JPanel sliderPanel = createSliderPanel();
        JPanel statusPanel = createStatusPanel();

        add(buttonPanel, BorderLayout.WEST);
        add(sliderPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.EAST);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        // Reset button
        resetButton = new JButton("\u23EE"); // Previous track symbol
        resetButton.setToolTipText("Reset (Home)");
        resetButton.setMargin(new Insets(2, 6, 2, 6));
        resetButton.addActionListener(e -> swingPlayer.reset());
        panel.add(resetButton);

        // Play/Pause button
        playButton = new JButton("\u25B6"); // Play symbol
        playButton.setToolTipText("Play/Pause (Space)");
        playButton.setMargin(new Insets(2, 6, 2, 6));
        playButton.addActionListener(e -> swingPlayer.togglePlayback());
        panel.add(playButton);

        // Stop button
        stopButton = new JButton("\u25A0"); // Stop symbol
        stopButton.setToolTipText("Stop (Escape)");
        stopButton.setMargin(new Insets(2, 6, 2, 6));
        stopButton.addActionListener(e -> swingPlayer.stop());
        panel.add(stopButton);

        // Step button
        stepButton = new JButton("\u23ED"); // Next track symbol
        stepButton.setToolTipText("Step Frame (Right Arrow)");
        stepButton.setMargin(new Insets(2, 6, 2, 6));
        stepButton.addActionListener(e -> swingPlayer.stepFrame());
        panel.add(stepButton);

        return panel;
    }

    private JPanel createSliderPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // Frame label
        frameLabel = new JLabel("Frame: 0 / 0");
        frameLabel.setPreferredSize(new Dimension(100, 20));
        panel.add(frameLabel, BorderLayout.WEST);

        // Frame slider
        frameSlider = new JSlider(1, 100, 1);
        frameSlider.setEnabled(false);
        frameSlider.addChangeListener(e -> {
            if (!sliderAdjusting && !frameSlider.getValueIsAdjusting()) {
                int frame = frameSlider.getValue();
                swingPlayer.getPlayer().goToFrame(frame);
            }
        });
        panel.add(frameSlider, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        // FPS label
        fpsLabel = new JLabel("FPS: --");
        fpsLabel.setPreferredSize(new Dimension(70, 20));
        panel.add(fpsLabel);

        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setPreferredSize(new Dimension(80, 20));
        panel.add(statusLabel);

        return panel;
    }

    public void updateState(boolean isPlaying, Movie movie) {
        // Update play button text
        if (isPlaying) {
            playButton.setText("\u23F8"); // Pause symbol
            statusLabel.setText("Playing");
        } else {
            playButton.setText("\u25B6"); // Play symbol
            statusLabel.setText("Stopped");
        }

        // Update frame info
        if (movie != null) {
            int currentFrame = movie.currentFrame;
            int totalFrames = movie.score != null ? movie.score.totalFrames : 1;

            // Update frame label
            frameLabel.setText(String.format("Frame: %d / %d", currentFrame, totalFrames));

            // Update slider
            sliderAdjusting = true;
            frameSlider.setMinimum(1);
            frameSlider.setMaximum(Math.max(1, totalFrames));
            frameSlider.setValue(currentFrame);
            frameSlider.setEnabled(true);
            sliderAdjusting = false;

            // Update FPS label
            if (movie.frameRate > 0) {
                fpsLabel.setText(String.format("FPS: %d", movie.frameRate));
            }
        } else {
            frameLabel.setText("Frame: 0 / 0");
            frameSlider.setEnabled(false);
            fpsLabel.setText("FPS: --");
            statusLabel.setText("No Movie");
        }
    }
}
