package com.bankmgmt.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Read-only receipt surface — printable artifact for resume demos. */
public class ReceiptViewerDialog extends JDialog {

    public ReceiptViewerDialog(Window owner, String receiptText) {
        super(owner, "Transaction receipt", ModalityType.APPLICATION_MODAL);
        JTextArea area = new JTextArea(receiptText, 18, 42);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new EmptyBorder(8, 8, 8, 8));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        add(sp, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }
}
