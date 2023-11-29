/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import com.skcraft.launcher.dialog.LauncherFrame;
import com.skcraft.launcher.swing.*;
import com.skcraft.launcher.util.Environment;
import com.skcraft.launcher.util.MultiLineTableCellRenderer;
import com.skcraft.launcher.util.Platform;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class RedditLauncherFrame extends LauncherFrame {

    private final double sizeModifier = 2.0;

    public RedditLauncherFrame(@NonNull final Launcher launcher) {
        super(launcher);
        setPreferredSize(new Dimension((int) (800 * sizeModifier), (int) (500 * sizeModifier)));
        setLocationRelativeTo(null);
        setIcons();
    }

    @Override
    public WebpagePanel createNewsPanel() {
        return WebpagePanel.forHTML("");
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
        super.initComponents();
        getContentPane().removeAll();
        instancesTable.setBackground(new Color(0, 0, 0, 0));
        instancesTable.setSelectionBackground(Theme.primary);
        instancesTable.setSelectionForeground(Theme.primaryText);
        instancesTable.setForeground(Theme.secondaryText);

        Font boldFont = new Font(instancesTable.getFont().getName(), Font.BOLD, (int) (16 * sizeModifier));
        instancesTable.setFont(boldFont);
        instancesTable.setOpaque(false);

        instancesTable.setRowHeight((int) (65 * sizeModifier));
        instancesTable.getColumnModel().getColumn(1).setPreferredWidth((int) (200 * sizeModifier));
        MultiLineTableCellRenderer renderer = new MultiLineTableCellRenderer();
        renderer.setFont(boldFont);
        instancesTable.getColumnModel().getColumn(1).setCellRenderer(renderer);

        redditInit();
    }

    private void redditInit() {
        RedditBackgroundPanel root = new RedditBackgroundPanel(Theme.subreddit, Theme.postCount, Theme.randomise, Theme.interval, Theme.fade);

        JPanel launchControls = new JPanel();
        launchControls.setOpaque(false);
        launchControls.add(optionsButton);
        launchControls.add(launchButton);

        JPanel updateControls = new JPanel();
        updateControls.add(refreshButton);
        updateControls.add(selfUpdateButton);
        updateControls.setBackground(getAltFrostColor(Theme.frost));

        JPanel left = new FrostPanel(root, Theme.frost);
        left.setLayout(new BorderLayout());
        left.setPreferredSize(new Dimension((int) (250 * sizeModifier), (int) (300 * sizeModifier)));
        left.add(instancesTable, BorderLayout.CENTER);
        left.add(updateControls, BorderLayout.PAGE_END);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BorderLayout());
        center.add(launchControls, BorderLayout.PAGE_END);

        root.setLayout(new BorderLayout());
        root.add(left, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);

        add(root);

        setMinimumSize(new Dimension((int) (600 * sizeModifier), (int) (350 * sizeModifier)));
        setPreferredSize(new Dimension((int) (700 * sizeModifier), (int) (420 * sizeModifier)));
    }

    private void setIcons() {
        Image mainIcon = SwingHelper.createImage(LauncherFrame.class, "/com/skcraft/launcher/icon.png");
        Image titleIcon = SwingHelper.createImage(LauncherFrame.class, "/com/skcraft/launcher/title.png");
        ArrayList<Image> icons = new ArrayList<Image>();
        if (mainIcon != null) {
            icons.add(mainIcon);
        }
        if (titleIcon != null) {
            icons.add(titleIcon.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        }
        setIconImages(icons);
    }

    private Color getAltFrostColor(Color c) {
        int r = Math.min(c.getRed() + 20, 255);
        int g = Math.min(c.getGreen() + 20, 255);
        int b = Math.min(c.getBlue() + 20, 255);
        int a = Math.min(c.getAlpha() + 40, 255);
        return new Color(r, g, b, a);
    }
}
