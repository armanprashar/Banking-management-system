package com.bankmgmt.ui;

import com.bankmgmt.AppContext;
import com.bankmgmt.exceptions.AuthenticationException;
import com.bankmgmt.model.User;
import com.bankmgmt.service.AuthService;
import com.bankmgmt.utils.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Primary entry screen — routes admins vs customers after credential verification. */
public class LoginFrame extends JFrame {

    private final AppContext ctx;
    private final JTextField usernameField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);

    public LoginFrame(AppContext ctx) {
        super("Bank Management — Sign In");
        this.ctx = ctx;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(420, 320));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Secure Banking Console", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Username"), gc);
        gc.gridy++;
        form.add(usernameField, gc);
        gc.gridy++;
        form.add(new JLabel("Password"), gc);
        gc.gridy++;
        form.add(passwordField, gc);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JButton forgotBtn = new JButton("Forgot password?");
        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> new RegisterDialog(this, ctx).setVisible(true));
        forgotBtn.addActionListener(e -> new ForgotPasswordDialog(this, ctx).setVisible(true));
        buttons.add(forgotBtn);
        buttons.add(registerBtn);
        buttons.add(loginBtn);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        ThemeManager.applyTheme(getRootPane());
        getRootPane().setDefaultButton(loginBtn);
    }

    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            Dialogs.warn(this, "Enter username and password.");
            return;
        }
        AuthService auth = ctx.auth();
        try {
            User user = auth.login(u, p);
            dispose();
            if (auth.isPlatformAdmin(user)) {
                new AdminDashboardFrame(ctx, user).setVisible(true);
            } else {
                new UserDashboardFrame(ctx, user).setVisible(true);
            }
        } catch (AuthenticationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
