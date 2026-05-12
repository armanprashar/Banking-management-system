package com.bankmgmt.ui;

import javax.swing.*;
import java.awt.*;

/** Standard success/error/information prompts for consistent UX. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
