package com.bankmgmt.ui;

import com.bankmgmt.AppContext;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterDialog extends JDialog {

    private final JTextField username = new JTextField(18);
    private final JTextField email = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JTextField fullName = new JTextField(18);
    private final JTextField phone = new JTextField(18);
    private final JTextField secQ = new JTextField(24);
    private final JTextField secA = new JTextField(18);

    public RegisterDialog(JFrame owner, AppContext ctx) {
        super(owner, "Create customer profile", true);
        setMinimumSize(new Dimension(460, 460));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        int row = 0;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Username"), gc);
        gc.gridx = 1;
        form.add(username, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Email"), gc);
        gc.gridx = 1;
        form.add(email, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Password"), gc);
        gc.gridx = 1;
        form.add(password, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Full name"), gc);
        gc.gridx = 1;
        form.add(fullName, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Phone (optional)"), gc);
        gc.gridx = 1;
        form.add(phone, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Security question"), gc);
        gc.gridx = 1;
        form.add(secQ, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        form.add(new JLabel("Security answer"), gc);
        gc.gridx = 1;
        form.add(secA, gc);

        root.add(form, BorderLayout.CENTER);

        JButton submit = new JButton("Submit registration");
        submit.addActionListener(e -> {
            try {
                AuthService auth = ctx.auth();
                auth.registerCustomer(
                        username.getText(),
                        email.getText(),
                        new String(password.getPassword()),
                        fullName.getText(),
                        phone.getText(),
                        secQ.getText(),
                        secA.getText()
                );
                Dialogs.info(this, "Registration successful. You can sign in.");
                dispose();
            } catch (BankException ex) {
                Dialogs.error(this, ex.getMessage());
            }
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(submit);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
    }
}
