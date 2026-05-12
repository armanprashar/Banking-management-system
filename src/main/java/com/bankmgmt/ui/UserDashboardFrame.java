package com.bankmgmt.ui;

import com.bankmgmt.AppContext;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.model.Account;
import com.bankmgmt.model.BankTransaction;
import com.bankmgmt.model.TransactionType;
import com.bankmgmt.model.User;
import com.bankmgmt.service.BankingService;
import com.bankmgmt.utils.FormValidator;
import com.bankmgmt.utils.ReceiptFormatter;
import com.bankmgmt.utils.ThemeManager;
import com.bankmgmt.utils.TransactionReportExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer-facing dashboard — accounts, operations, filtered ledger, exports, theme toggle.
 * Demonstrates SwingWorker for non-blocking JDBC reads (multithreading / responsive UI).
 */
public class UserDashboardFrame extends JFrame {

    private final AppContext ctx;
    private final User user;

    private final DefaultTableModel accountsModel = new DefaultTableModel(
            new Object[]{"ID", "Number", "Balance", "Status", "Type"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable accountsTable = new JTable(accountsModel);

    private final DefaultTableModel txModel = new DefaultTableModel(
            new Object[]{"Receipt", "Type", "Amount", "When", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable txTable = new JTable(txModel);

    private final JComboBox<TransactionType> typeFilter = new JComboBox<>(new TransactionType[]{
            null, TransactionType.DEPOSIT, TransactionType.WITHDRAW, TransactionType.TRANSFER
    });
    private final JTextField fromDate = new JTextField(12);
    private final JTextField toDate = new JTextField(12);

    private final JComboBox<Account> accountCombo = new JComboBox<>();
    private final JTextField depositAmt = new JTextField(10);
    private final JTextField withdrawAmt = new JTextField(10);
    private final JTextField transferAmt = new JTextField(10);
    private final JTextField transferToNum = new JTextField(16);
    private final JTextField noteField = new JTextField(24);

    private List<BankTransaction> lastLoadedTx = new ArrayList<>();

    public UserDashboardFrame(AppContext ctx, User user) {
        super("Bank — Customer · " + user.getFullName());
        this.ctx = ctx;
        this.user = user;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Session");
        JMenuItem logout = new JMenuItem("Sign out");
        logout.addActionListener(e -> signOut());
        file.add(logout);
        bar.add(file);
        setJMenuBar(bar);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Accounts", buildAccountsTab());
        tabs.addTab("Transactions", buildTransactionsTab());
        tabs.addTab("Banking", buildBankingTab());
        tabs.addTab("Appearance", buildAppearanceTab());

        add(tabs);
        ThemeManager.applyTheme(getRootPane());

        reloadAccountsAsync();
        reloadTransactionsAsync(null, null, null);
    }

    private JPanel buildAppearanceTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel lbl = new JLabel("Switch workspace chrome without restarting.");
        JButton toggle = new JButton("Toggle dark / light theme");
        toggle.addActionListener(e -> {
            ThemeManager.setDarkMode(!ThemeManager.isDarkMode());
            ThemeManager.applyTheme(getRootPane());
        });
        p.add(lbl, BorderLayout.NORTH);
        p.add(toggle, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAccountsTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reloadAccountsAsync());
        JButton open = new JButton("Open new account");
        open.addActionListener(e -> openAccountAsync());
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(refresh);
        north.add(open);
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(accountsTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTransactionsTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                           boolean isSelected, boolean cellHasFocus) {
                String label = value == null ? "ALL TYPES" : ((TransactionType) value).name();
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        fromDate.setToolTipText("From yyyy-MM-dd (optional)");
        toDate.setToolTipText("To yyyy-MM-dd (optional)");
        JButton apply = new JButton("Apply filters");
        apply.addActionListener(e -> {
            LocalDateTime from = parseStart(fromDate.getText());
            LocalDateTime to = parseEnd(toDate.getText());
            TransactionType tf = (TransactionType) typeFilter.getSelectedItem();
            reloadTransactionsAsync(from, to, tf);
        });
        JButton export = new JButton("Export CSV…");
        export.addActionListener(e -> exportCsv());
        JButton mini = new JButton("Mini statement (selected account row)");
        mini.addActionListener(e -> showMiniStatement());

        filters.add(new JLabel("Type"));
        filters.add(typeFilter);
        filters.add(new JLabel("From"));
        filters.add(fromDate);
        filters.add(new JLabel("To"));
        filters.add(toDate);
        filters.add(apply);
        filters.add(export);
        filters.add(mini);

        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(txTable), BorderLayout.CENTER);

        txTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showReceiptForSelectedRow();
                }
            }
        });

        return p;
    }

    private JPanel buildBankingTab() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        int row = 0;
        gc.gridx = 0;
        gc.gridy = row;
        grid.add(new JLabel("Account"), gc);
        gc.gridx = 1;
        grid.add(accountCombo, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        grid.add(new JLabel("Note (optional)"), gc);
        gc.gridx = 1;
        grid.add(noteField, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        grid.add(new JLabel("Deposit amount"), gc);
        gc.gridx = 1;
        JPanel depRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        depRow.add(depositAmt);
        JButton depBtn = new JButton("Deposit");
        depBtn.addActionListener(e -> runDeposit());
        depRow.add(depBtn);
        grid.add(depRow, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        grid.add(new JLabel("Withdraw amount"), gc);
        gc.gridx = 1;
        JPanel wRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        wRow.add(withdrawAmt);
        JButton wBtn = new JButton("Withdraw");
        wBtn.addActionListener(e -> runWithdraw());
        wRow.add(wBtn);
        grid.add(wRow, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        grid.add(new JLabel("Transfer to account #"), gc);
        gc.gridx = 1;
        grid.add(transferToNum, gc);
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        grid.add(new JLabel("Transfer amount"), gc);
        gc.gridx = 1;
        JPanel tRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tRow.add(transferAmt);
        JButton tBtn = new JButton("Transfer");
        tBtn.addActionListener(e -> runTransfer());
        tRow.add(tBtn);
        grid.add(tRow, gc);

        p.add(grid, BorderLayout.NORTH);
        return p;
    }

    private LocalDateTime parseStart(String raw) {
        if (!FormValidator.isNonBlank(raw)) {
            return null;
        }
        LocalDate d = LocalDate.parse(raw.trim());
        return LocalDateTime.of(d, LocalTime.MIN);
    }

    private LocalDateTime parseEnd(String raw) {
        if (!FormValidator.isNonBlank(raw)) {
            return null;
        }
        LocalDate d = LocalDate.parse(raw.trim());
        return LocalDateTime.of(d, LocalTime.MAX);
    }

    private void signOut() {
        dispose();
        new LoginFrame(ctx).setVisible(true);
    }

    private Account selectedAccountFromCombo() throws BankException {
        Account a = (Account) accountCombo.getSelectedItem();
        if (a == null) {
            throw new BankException("Select an account.");
        }
        return a;
    }

    private long selectedAccountIdFromTable() throws BankException {
        int r = accountsTable.getSelectedRow();
        if (r < 0) {
            throw new BankException("Select an account in the Accounts tab.");
        }
        return Long.parseLong(accountsModel.getValueAt(r, 0).toString());
    }

    private void reloadAccountsAsync() {
        new SwingWorker<List<Account>, Void>() {
            @Override
            protected List<Account> doInBackground() {
                return ctx.accounts().findByUserId(user.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Account> list = get();
                    accountsModel.setRowCount(0);
                    DefaultComboBoxModel<Account> cb = new DefaultComboBoxModel<>();
                    for (Account a : list) {
                        accountsModel.addRow(new Object[]{
                                a.getId(), a.getAccountNumber(), a.getBalance(),
                                a.getStatus(), a.getAccountType()
                        });
                        cb.addElement(a);
                    }
                    accountCombo.setModel(cb);
                } catch (Exception ex) {
                    Dialogs.error(UserDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void openAccountAsync() {
        new SwingWorker<Account, Void>() {
            @Override
            protected Account doInBackground() throws Exception {
                return ctx.banking().openAccount(user.getId(), "SAVINGS");
            }

            @Override
            protected void done() {
                try {
                    Account a = get();
                    Dialogs.info(UserDashboardFrame.this, "Opened account " + a.getAccountNumber());
                    reloadAccountsAsync();
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    Dialogs.error(UserDashboardFrame.this, c.getMessage());
                }
            }
        }.execute();
    }

    private void reloadTransactionsAsync(LocalDateTime from, LocalDateTime to, TransactionType tf) {
        new SwingWorker<List<BankTransaction>, Void>() {
            @Override
            protected List<BankTransaction> doInBackground() {
                return ctx.banking().history(user.getId(), from, to, tf);
            }

            @Override
            protected void done() {
                try {
                    List<BankTransaction> list = get();
                    lastLoadedTx = list;
                    txModel.setRowCount(0);
                    for (BankTransaction t : list) {
                        txModel.addRow(new Object[]{
                                t.getReceiptRef(),
                                t.getTransactionType(),
                                t.getAmount(),
                                t.getCreatedAt(),
                                t.getDescription()
                        });
                    }
                } catch (Exception ex) {
                    Dialogs.error(UserDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void exportCsv() {
        if (lastLoadedTx.isEmpty()) {
            Dialogs.warn(this, "Nothing to export — apply filters first.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                TransactionReportExporter.exportCsv(f.toPath(), lastLoadedTx);
                Dialogs.info(this, "Saved " + f.getAbsolutePath());
            } catch (Exception ex) {
                Dialogs.error(this, ex.getMessage());
            }
        }
    }

    private void showMiniStatement() {
        try {
            long accId = selectedAccountIdFromTable();
            List<BankTransaction> rows = ctx.banking().mini(accId, 10);
            StringBuilder sb = new StringBuilder("Mini statement (last 10)\n\n");
            for (BankTransaction t : rows) {
                sb.append(t.getCreatedAt()).append(" | ")
                        .append(t.getTransactionType()).append(" | ")
                        .append(t.getAmount()).append(" | ")
                        .append(t.getReceiptRef()).append('\n');
            }
            JTextArea ta = new JTextArea(sb.toString(), 16, 48);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Mini statement",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void showReceiptForSelectedRow() {
        int r = txTable.getSelectedRow();
        if (r < 0) {
            return;
        }
        String ref = txModel.getValueAt(r, 0).toString();
        BankTransaction tx = ctx.transactions().findByReceiptRef(ref);
        if (tx == null) {
            Dialogs.warn(this, "Receipt not found.");
            return;
        }
        String fromNum = tx.getFromAccountId() == null ? null
                : ctx.accounts().findById(tx.getFromAccountId()).map(Account::getAccountNumber).orElse("?");
        String toNum = tx.getToAccountId() == null ? null
                : ctx.accounts().findById(tx.getToAccountId()).map(Account::getAccountNumber).orElse("?");
        String text = ReceiptFormatter.format(tx, fromNum, toNum);
        new ReceiptViewerDialog(this, text).setVisible(true);
    }

    private void runDeposit() {
        try {
            Account acc = selectedAccountFromCombo();
            BigDecimal amt = FormValidator.parsePositiveMoney(depositAmt.getText());
            BankingService bank = ctx.banking();
            BankTransaction tx = bank.deposit(acc.getId(), user.getId(), amt, noteField.getText().trim());
            Dialogs.info(this, "Deposit OK · Receipt " + tx.getReceiptRef());
            reloadAccountsAsync();
            reloadTransactionsAsync(null, null, null);
        } catch (Exception ex) {
            Dialogs.error(this, rootCauseMessage(ex));
        }
    }

    private void runWithdraw() {
        try {
            Account acc = selectedAccountFromCombo();
            BigDecimal amt = FormValidator.parsePositiveMoney(withdrawAmt.getText());
            BankTransaction tx = ctx.banking().withdraw(acc.getId(), user.getId(), amt, noteField.getText().trim());
            Dialogs.info(this, "Withdraw OK · Receipt " + tx.getReceiptRef());
            reloadAccountsAsync();
            reloadTransactionsAsync(null, null, null);
        } catch (Exception ex) {
            Dialogs.error(this, rootCauseMessage(ex));
        }
    }

    private void runTransfer() {
        try {
            Account acc = selectedAccountFromCombo();
            BigDecimal amt = FormValidator.parsePositiveMoney(transferAmt.getText());
            BankTransaction tx = ctx.banking().transfer(acc.getId(), transferToNum.getText().trim(),
                    user.getId(), amt, noteField.getText().trim());
            Dialogs.info(this, "Transfer OK · Receipt " + tx.getReceiptRef());
            reloadAccountsAsync();
            reloadTransactionsAsync(null, null, null);
        } catch (Exception ex) {
            Dialogs.error(this, rootCauseMessage(ex));
        }
    }

    private static String rootCauseMessage(Exception ex) {
        Throwable t = ex.getCause() != null ? ex.getCause() : ex;
        return t.getMessage() != null ? t.getMessage() : t.toString();
    }
}
