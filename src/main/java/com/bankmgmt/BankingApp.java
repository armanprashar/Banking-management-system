package com.bankmgmt;

import com.bankmgmt.ui.LoginFrame;

import javax.swing.*;

/**
 * Bootstrap — configures cross-platform LAF then hands off to the login MVC shell.
 */
public final class BankingApp {

    private BankingApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException e) {
                // Fallback to Metal if system LAF unavailable
            }
            LoginFrame frame = new LoginFrame(AppContext.get());
            frame.setVisible(true);
        });
    }
}
