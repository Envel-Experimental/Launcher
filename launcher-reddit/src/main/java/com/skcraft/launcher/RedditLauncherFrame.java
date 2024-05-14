/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import com.skcraft.launcher.dialog.LauncherFrame;
import com.skcraft.launcher.swing.ColoredButton;
import com.skcraft.launcher.swing.FrostPanel;
import com.skcraft.launcher.swing.RedditBackgroundPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.Environment;
import com.skcraft.launcher.util.MultiLineTableCellRenderer;
import com.skcraft.launcher.util.Platform;
import java.awt.image.BufferedImage;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

@Log
public class RedditLauncherFrame extends LauncherFrame {

    private double sizeModifier;

    public RedditLauncherFrame(@NonNull final Launcher launcher) {
        super(launcher);
        setPreferredSize(new Dimension((int) (800 * sizeModifier), (int) (500 * sizeModifier)));
        setLocationRelativeTo(null);
        setIcons();
    }

    protected JButton createPrimaryButton(String name) {
        JButton button = new JButton(name);
        if (Environment.detectPlatform() == Platform.WINDOWS) {
            button = new ColoredButton(name, Theme.primary, Theme.primaryAlt);
            button.setFont(new Font(button.getFont().getName(), Font.PLAIN, (int) (Theme.primarySize * sizeModifier)));
            button.setForeground(Theme.primaryText);
            button.setPreferredSize(new Dimension((int) (Theme.primaryButtonSize.width * sizeModifier), (int) (Theme.primaryButtonSize.height * sizeModifier)));
        }
        return button;
    }

    protected JButton createSecondaryButton(String name) {
        JButton button = new ColoredButton(name, Theme.secondary, Theme.secondaryAlt);
        button.setFont(new Font(button.getFont().getName(), Font.PLAIN, (int) (Theme.secondarySize * sizeModifier)));
        button.setForeground(Theme.secondaryText);
        button.setPreferredSize(new Dimension((int) (Theme.secondaryButtonSize.width * sizeModifier), (int) (Theme.secondaryButtonSize.height * sizeModifier)));
        return button;
    }

    protected JCheckBox createCheckBox(String name) {
        JCheckBox box = new JCheckBox(name);
        box.setFont(new Font(box.getFont().getName(), Font.PLAIN, (int) (Theme.secondarySize * sizeModifier)));
        box.setBackground(Theme.secondary);
        box.setForeground(Theme.secondaryText);
        box.setPreferredSize(new Dimension((int) (Theme.secondaryButtonSize.width * sizeModifier), (int) (Theme.secondaryButtonSize.height * sizeModifier)));
        box.setHorizontalAlignment(SwingConstants.CENTER);
        return box;
    }

    @Override
    public void initComponents() {
        calculateSizeModifier();
        super.initComponents();
        getContentPane().removeAll();
        instancesTable.setBackground(new Color(0, 0, 0, 0));
        instancesTable.setSelectionBackground(Theme.primary);
        instancesTable.setSelectionForeground(Theme.primaryText);
        instancesTable.setForeground(Theme.secondaryText);

        Font boldFont = new Font(instancesTable.getFont().getName(), Font.BOLD, (int) (25 * sizeModifier));
        instancesTable.setFont(boldFont);
        instancesTable.setOpaque(false);

        instancesTable.setRowHeight((int) (105 * sizeModifier));
        instancesTable.getColumnModel().getColumn(1).setPreferredWidth((int) (200 * sizeModifier));

        MultiLineTableCellRenderer renderer = new MultiLineTableCellRenderer();
        renderer.setFont(boldFont);
        instancesTable.getColumnModel().getColumn(1).setCellRenderer(renderer);

        redditInit();
    }

    private void redditInit() {
        RedditBackgroundPanel root = new RedditBackgroundPanel(Theme.subreddit, Theme.postCount, Theme.randomise, Theme.interval, Theme.fade);
        root.setLayout(new BorderLayout());

        JPanel launchControls = new JPanel();
        launchControls.setOpaque(false);
        launchControls.add(optionsButton);
        launchControls.add(launchButton);

        int buttonWidth = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 0.1);
        int buttonHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 0.09);

        launchButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        optionsButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

        int leftPanelWidth = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 0.18);
        int leftPanelHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 0.3);

        JPanel left = new FrostPanel(root, Theme.frost);
        left.setLayout(new BorderLayout());
        left.setPreferredSize(new Dimension(leftPanelWidth, leftPanelHeight));
        left.add(instancesTable, BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BorderLayout());
        //center.add(getHeaderImage(), Theme.headerAlignY);
        center.add(launchControls, BorderLayout.PAGE_END);

        int fontSize = (int) (sizeModifier * 22);
        Font buttonFont = launchButton.getFont().deriveFont(Font.BOLD, fontSize);

        launchButton.setFont(buttonFont);
        optionsButton.setFont(buttonFont);

        root.add(left, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);

        add(root);

        int windowWidth = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 0.52);
        int windowHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 0.56);
        setPreferredSize(new Dimension(windowWidth, windowHeight));
        pack();

        setLocationRelativeTo(null);
    }

    private void calculateSizeModifier() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        sizeModifier = Math.min(screenWidth / 1920.0, screenHeight / 1080.0);
    }

    private void setIcons() {
        Image titleIcon = SwingHelper.createImage(LauncherFrame.class, "/com/skcraft/launcher/title.png");
        ArrayList<Image> icons = new ArrayList<Image>();
        if (titleIcon != null) {
            icons.add(titleIcon);
        }
        if (titleIcon != null) {
            icons.add(titleIcon.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        }
        setIconImages(icons);
    }

    protected JLabel getHeaderImage() {
        JLabel label = new JLabel();
        try {
            BufferedImage image = ImageIO.read(RedditLauncher.class.getResourceAsStream("/com/skcraft/launcher/header.png"));
            int width = Math.min(image.getWidth(), 350);
            label.setIcon(new ImageIcon(image.getScaledInstance(width, -1, Image.SCALE_SMOOTH)));
            label.setHorizontalAlignment(Theme.headerAlignX);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return label;
    }
}
