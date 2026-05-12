package com.bankmgmt.utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

/** Applies coherent light/dark palettes across Swing trees. */
public final class ThemeManager {

    public static final Color LIGHT_BG = new Color(248, 249, 252);
    public static final Color LIGHT_PANEL = Color.WHITE;
    public static final Color ACCENT = new Color(37, 99, 235);
    public static final Color DARK_BG = new Color(24, 27, 38);
    public static final Color DARK_PANEL = new Color(35, 39, 53);

    private static boolean darkMode;

    private ThemeManager() {
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
    }

    public static void applyTheme(JRootPane root) {
        Color bg = darkMode ? DARK_BG : LIGHT_BG;
        Color fg = darkMode ? new Color(230, 235, 245) : new Color(30, 41, 59);
        Color panelBg = darkMode ? DARK_PANEL : LIGHT_PANEL;

        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        setUIFont(new FontUIResource(base));

        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        defaults.put("Panel.background", panelBg);
        defaults.put("Label.foreground", fg);
        defaults.put("Button.background", ACCENT);
        defaults.put("Button.foreground", Color.WHITE);
        defaults.put("Table.background", panelBg);
        defaults.put("Table.foreground", fg);
        defaults.put("Table.gridColor", darkMode ? new Color(60, 65, 85) : new Color(226, 232, 240));
        defaults.put("TextField.background", darkMode ? new Color(45, 50, 68) : Color.WHITE);
        defaults.put("TextField.foreground", fg);
        defaults.put("ComboBox.background", panelBg);

        if (root != null) {
            root.getContentPane().setBackground(bg);
            paintTree(root.getContentPane(), bg, fg, panelBg);
        }
        SwingUtilities.updateComponentTreeUI(root);
    }

    private static void paintTree(Container c, Color bg, Color fg, Color panelBg) {
        for (Component child : c.getComponents()) {
            if (child instanceof JPanel j) {
                j.setBackground(panelBg);
                j.setForeground(fg);
                Border b = j.getBorder();
                if (b instanceof EmptyBorder) {
                    // keep
                }
                paintTree(j, bg, fg, panelBg);
            } else if (child instanceof JLabel lbl) {
                lbl.setForeground(fg);
            } else if (child instanceof JScrollPane sp) {
                sp.getViewport().setBackground(panelBg);
                Component view = sp.getViewport().getView();
                if (view instanceof JComponent jc) {
                    jc.setBackground(panelBg);
                    jc.setForeground(fg);
                }
            } else if (child instanceof Container cc) {
                paintTree(cc, bg, fg, panelBg);
            }
        }
    }

    private static void setUIFont(FontUIResource f) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    public static Border padded(int pad) {
        return new EmptyBorder(pad, pad, pad, pad);
    }
}
