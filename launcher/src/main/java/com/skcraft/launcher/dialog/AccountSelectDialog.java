package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.concurrency.SettableProgress;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.*;
import com.skcraft.launcher.dialog.component.SizeModifier;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.SharedLocale;
import com.skcraft.launcher.util.SwingExecutor;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;

public class AccountSelectDialog extends JDialog {
    private final JList<SavedSession> accountList;
    private final JButton loginButton = new JButton(SharedLocale.tr("accounts.play"));
    private final JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));
    private final JButton addOfflineButton = new JButton(SharedLocale.tr("accounts.addOffline"));
    private final JButton addMicrosoftButton = new JButton(SharedLocale.tr("accounts.addMicrosoft"));
    private final JButton removeSelected = new JButton(SharedLocale.tr("accounts.removeSelected"));
    private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);

    private final Launcher launcher;
    private Session selected;

    public AccountSelectDialog(Window owner, Launcher launcher) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;
        this.accountList = new JList<>(launcher.getAccounts());

        setTitle(SharedLocale.tr("accounts.title"));
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setMinimumSize(new Dimension(350, 250));
        setResizable(true);
        pack();
        setLocationRelativeTo(owner);
    }

    public static Session showAccountRequest(Window owner, Launcher launcher) {
        AccountSelectDialog dialog = new AccountSelectDialog(owner, launcher);
        dialog.setVisible(true);

        if (dialog.selected != null && dialog.selected.isOnline()) {
            launcher.getAccounts().update(dialog.selected.toSavedSession());
        }

        Persistence.commitAndForget(launcher.getAccounts());

        return dialog.selected;
    }

    private void initComponents() {

        SizeModifier sizeModifier = new SizeModifier();
        sizeModifier.calculateSizeModifier();


        setLayout(new BorderLayout());

        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountList.setLayoutOrientation(JList.VERTICAL);
        accountList.setVisibleRowCount(0);
        accountList.setCellRenderer(new AccountRenderer());

        JScrollPane accountPane = new JScrollPane(accountList);

        int width = (int) (400 * sizeModifier.sizeModifier);
        int height = (int) (250 * sizeModifier.sizeModifier);
        accountPane.setPreferredSize(new Dimension(width, height));

        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 25 * (float) sizeModifier.sizeModifier));
        cancelButton.setFont(cancelButton.getFont().deriveFont(25 * (float) sizeModifier.sizeModifier));
        addOfflineButton.setFont(loginButton.getFont());
        addMicrosoftButton.setFont(cancelButton.getFont());
        removeSelected.setFont(cancelButton.getFont());

        int buttonWidth = (int) (200 * sizeModifier.sizeModifier);
        int buttonHeight = (int) (100 * sizeModifier.sizeModifier);
        Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);
        loginButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(cancelButton);
        buttonPanel.add(loginButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder((int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier)));

        JPanel loginButtonsRow = new JPanel(new GridLayout(0, 1, 0, (int) (10 * sizeModifier.sizeModifier)));
        loginButtonsRow.add(addOfflineButton);
        loginButtonsRow.add(addMicrosoftButton);
        loginButtonsRow.add(removeSelected);
        loginButtonsRow.setBorder(BorderFactory.createEmptyBorder(
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier)));


        JPanel listAndLoginContainer = new JPanel(new BorderLayout((int) (20 * sizeModifier.sizeModifier), 0));
        listAndLoginContainer.add(accountPane, BorderLayout.CENTER);
        listAndLoginContainer.add(loginButtonsRow, BorderLayout.EAST);
        listAndLoginContainer.setBorder(BorderFactory.createEmptyBorder((int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier),
                (int) (13 * sizeModifier.sizeModifier)));

        add(listAndLoginContainer, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);


        loginButton.addActionListener(ev -> {
            SavedSession selectedSession = accountList.getSelectedValue();
            if (selectedSession != null) {
                attemptExistingLogin(selectedSession);
            }
        });

        cancelButton.addActionListener(ev -> dispose());

        addOfflineButton.addActionListener(ev -> {
            Session newSession = LoginDialog.showLoginRequest(this, launcher);

            if (newSession != null) {
                launcher.getAccounts().update(newSession.toSavedSession());
                setResult(newSession);
            }
        });

        addMicrosoftButton.addActionListener(ev -> attemptMicrosoftLogin());

        removeSelected.addActionListener(ev -> {
            SavedSession selectedValue = accountList.getSelectedValue();
            if (selectedValue != null) {
                boolean confirmed = SwingHelper.confirmDialog(this, SharedLocale.tr("accounts.confirmForget"),
                        SharedLocale.tr("accounts.confirmForgetTitle"));

                if (confirmed) {
                    launcher.getAccounts().remove(selectedValue);
                }
            }
        });

        accountList.setSelectedIndex(0);
    }


    @Override
    public void dispose() {
        accountList.setModel(new DefaultListModel<>());
        super.dispose();
    }

    private void setResult(Session result) {
        this.selected = result;
        dispose();
    }

    private void attemptMicrosoftLogin() {
        String status = SharedLocale.tr("login.microsoft.seeBrowser");
        SettableProgress progress = new SettableProgress(status, -1);

        ListenableFuture<?> future = launcher.getExecutor().submit(() -> {
            Session newSession = launcher.getMicrosoftLogin().login(() ->
                    progress.set(SharedLocale.tr("login.loggingInStatus"), -1));

            if (newSession != null) {
                launcher.getAccounts().update(newSession.toSavedSession());
                setResult(newSession);
            }

            return null;
        });

        ProgressDialog.showProgress(this, future, progress,
                SharedLocale.tr("login.loggingInTitle"), status);
        SwingHelper.addErrorDialogCallback(this, future);
    }

    private void attemptExistingLogin(SavedSession session) {
        setResult(new OfflineSession(session.getUsername()));

        if (accountList.getSelectedValue() != null) {
            Session offlineSession = new OfflineSession(session.getUsername());
            setResult(offlineSession);
        } else {
            LoginService loginService = null;
            if (session.getType() == UserType.MICROSOFT) {
                loginService = launcher.getLoginService(session.getType());
            }

            RestoreSessionCallable callable = new RestoreSessionCallable(loginService, session);

            ObservableFuture<Session> future = new ObservableFuture<>(launcher.getExecutor().submit(callable), callable);
            Futures.addCallback(future, new FutureCallback<Session>() {
                @Override
                public void onSuccess(Session result) {
                    setResult(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, SwingExecutor.INSTANCE);

            ProgressDialog.showProgress(this, future, SharedLocale.tr("login.loggingInTitle"),
                    SharedLocale.tr("login.loggingInStatus"));
            SwingHelper.addErrorDialogCallback(this, future);
        }
    }

    @RequiredArgsConstructor
    private static class RestoreSessionCallable implements Callable<Session>, ProgressObservable {
        private final LoginService service;
        private final SavedSession session;

        @Override
        public Session call() throws Exception {
            if (service != null) {
                return service.restore(session);
            } else {
                return new OfflineSession(session.getUsername());
            }
        }

        @Override
        public String getStatus() {
            return SharedLocale.tr("accounts.refreshingStatus");
        }

        @Override
        public double getProgress() {
            return -1;
        }
    }

    private static class AccountRenderer extends JLabel implements ListCellRenderer<SavedSession> {
        public AccountRenderer() {
            setHorizontalAlignment(LEFT);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SavedSession> list, SavedSession value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getUsername());
            setFont(getFont().deriveFont(Font.BOLD, 25f));
            if (value.getAvatarImage() != null) {
                setIcon(new ImageIcon(value.getAvatarImage()));
            } else {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int iconSize = (int) (screenSize.width * 0.03);

                setIcon(SwingHelper.createIcon(Launcher.class, "default_skin.png", iconSize, iconSize));
            }

            if (isSelected) {
                setOpaque(true);
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setOpaque(false);
                setForeground(list.getForeground());
            }

            return this;
        }
    }
}
