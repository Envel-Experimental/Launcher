/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.OfflineSession;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.dialog.component.SizeModifier;
import com.skcraft.launcher.swing.FormPanel;
import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TextFieldPopupMenu;
import com.skcraft.launcher.util.SharedLocale;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * The login dialog.
 */
public class LoginDialog extends JDialog {

    private final Launcher launcher;
    private final JLabel message = new JLabel(SharedLocale.tr("login.defaultMessage"));
    private final JTextField usernameText = new JTextField();
    private final JPasswordField passwordText = new JPasswordField();
    private final JButton loginButton = new JButton(SharedLocale.tr("login.login"));
    //private final LinkButton recoverButton = new LinkButton(SharedLocale.tr("login.recoverAccount"));
    private final JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));
    private final FormPanel formPanel = new FormPanel();
    private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);
    @Getter
    private Session session;

    /**
     * Create a new login dialog.
     *
     * @param owner    the owner
     * @param launcher the launcher
     */
    public LoginDialog(Window owner, @NonNull Launcher launcher, Optional<ReloginDetails> reloginDetails) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;

        setTitle(SharedLocale.tr("login.title"));
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(420, 0));
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dispose();
            }
        });

        reloginDetails.ifPresent(details -> message.setText(details.message));
    }

    public static Session showLoginRequest(Window owner, Launcher launcher) {
        return showLoginRequest(owner, launcher, null);
    }

    public static Session showLoginRequest(Window owner, Launcher launcher, ReloginDetails reloginDetails) {
        LoginDialog dialog = new LoginDialog(owner, launcher, Optional.ofNullable(reloginDetails));
        dialog.setVisible(true);
        return dialog.getSession();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        SizeModifier sizeModifier = new SizeModifier();
        sizeModifier.calculateSizeModifier();

        usernameText.setEditable(true);
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, (float) (25 * sizeModifier.sizeModifier)));

        int textFieldWidth = (int) (5 * sizeModifier.sizeModifier);
        int textFieldHeight = (int) (45 * sizeModifier.sizeModifier);

        int buttonWidth = (int) (5 * sizeModifier.sizeModifier);
        int buttonHeight = (int) (65 * sizeModifier.sizeModifier);

        loginButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        usernameText.setPreferredSize(new Dimension(textFieldWidth, textFieldHeight));
        usernameText.setFont(usernameText.getFont().deriveFont(25 * (float) sizeModifier.sizeModifier));

        message.setFont(new Font("Arial", Font.PLAIN, (int) (19 * sizeModifier.sizeModifier)));
        message.setHorizontalAlignment(SwingConstants.CENTER);

        formPanel.addRow(message);
        JLabel usernameLabel = new JLabel(SharedLocale.tr("login.nickname"));
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD, (float) (25 * sizeModifier.sizeModifier)));
        formPanel.addRow(usernameLabel, usernameText);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        int padding = (int) (13 * sizeModifier.sizeModifier);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding * 2, padding));
        buttonPanel.add(loginButton, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginButton);
        passwordText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        loginButton.addActionListener(e -> prepareLogin());
    }


    @SuppressWarnings("deprecation")
    private void prepareLogin() {
        String username = usernameText.getText();

        String regex = "^[a-zA-Z0-9_]*$";

        if (!username.isEmpty() && username.matches(regex)) {
            setResult(new OfflineSession(username));
        } else {
            SwingHelper.showErrorDialog(this, SharedLocale.tr("login.invalidUsernameError"), SharedLocale.tr("login.invalidUsernameTitle"));
        }
    }


    private void setResult(Session session) {
        this.session = session;
        dispose();
    }

    @Data
    public static class ReloginDetails {
        private final String username;
        private final String message;
    }
}