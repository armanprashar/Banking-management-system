package com.bankmgmt.ui;

import com.bankmgmt.AppContext;
import com.bankmgmt.exceptions.AuthenticationException;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Recovery flow verified against salted answer hash before password rotation. */
public class ForgotPasswordDialog extends JDialog {

    private final JTextField username = new JTextField(18);
    private final JLabel questionLabel = new JLabel("—");
    private final JTextField answer = new JTextField(18);
    private final JPasswordField newPass = new JPasswordField(18);
    private final AppContext ctx;

    public ForgotPasswordDialog(JFrame owner, AppContext ctx) {
        super(owner, "Reset password", true);
        this.ctx = ctx;
        setMinimumSize(new Dimension(420, 280));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Username"), gc);
        gc.gridx = 1;
        form.add(username, gc);
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel(" "), gc);
        gc.gridx = 1;
        JButton loadQ = new JButton("Load security question");
        loadQ.addActionListener(e -> loadQuestion());
        form.add(loadQ, gc);
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel("Question"), gc);
        gc.gridx = 1;
        form.add(questionLabel, gc);
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel("Answer"), gc);
        gc.gridx = 1;
        form.add(answer, gc);
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel("New password"), gc);
        gc.gridx = 1;
        form.add(newPass, gc);

        root.add(form, BorderLayout.CENTER);

        JButton reset = new JButton("Update password");
        reset.addActionListener(e -> applyReset());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(reset);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
    }

    private void loadQuestion() {
        try {
            String q = ctx.auth().recoveryQuestionFor(username.getText().trim());
            questionLabel.setText(q);
        } catch (AuthenticationException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void applyReset() {
        try {
            ctx.auth().resetPasswordAfterVerification(
                    username.getText().trim(),
                    answer.getText(),
                    new String(newPass.getPassword())
            );
            Dialogs.info(this, "Password updated.");
            dispose();
        } catch (AuthenticationException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (BankException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}
