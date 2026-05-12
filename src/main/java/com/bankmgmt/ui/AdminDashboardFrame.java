package com.bankmgmt.ui;

import com.bankmgmt.AppContext;
import com.bankmgmt.exceptions.BankException;
import com.bankmgmt.model.Account;
import com.bankmgmt.model.AccountStatus;
import com.bankmgmt.model.ActivityLog;
import com.bankmgmt.model.BankTransaction;
import com.bankmgmt.model.User;
import com.bankmgmt.service.AdminService;
import com.bankmgmt.utils.ThemeManager;
import com.bankmgmt.utils.TransactionReportExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Operations desk — user lifecycle, account freezes, ledger audit trail, activity logs. */
public class AdminDashboardFrame extends JFrame {

    private final AppContext ctx;
    private final User adminUser;

    private final DefaultTableModel usersModel = new DefaultTableModel(
            new Object[]{"ID", "Username", "Email", "Role", "Active", "Full name"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable usersTable = new JTable(usersModel);

    private final DefaultTableModel accountsModel = new DefaultTableModel(
            new Object[]{"ID", "Number", "User ID", "Balance", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable accountsTable = new JTable(accountsModel);

    private final DefaultTableModel ledgerModel = new DefaultTableModel(
            new Object[]{"Receipt", "Type", "Amount", "When", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ledgerTable = new JTable(ledgerModel);

    private final DefaultTableModel activityModel = new DefaultTableModel(
            new Object[]{"When", "User ID", "Action", "Details"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable activityTable = new JTable(activityModel);

    private final JTextField userSearch = new JTextField(18);
    private final JTextField accountSearch = new JTextField(18);
    private final JTextField ledgerFrom = new JTextField(10);
    private final JTextField ledgerTo = new JTextField(10);
    private final JTextField ledgerContains = new JTextField(16);
    private final JTextField activityFilter = new JTextField(16);

    private List<BankTransaction> lastLedger = List.of();

    public AdminDashboardFrame(AppContext ctx, User adminUser) {
        super("Bank — Administrator · " + adminUser.getUsername());
        this.ctx = ctx;
        this.adminUser = adminUser;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Session");
        JMenuItem logout = new JMenuItem("Sign out");
        logout.addActionListener(e -> signOut());
        file.add(logout);
        bar.add(file);
        setJMenuBar(bar);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Customers", buildUsersTab());
        tabs.addTab("Accounts", buildAccountsTab());
        tabs.addTab("Transaction log", buildLedgerTab());
        tabs.addTab("Activity logs", buildActivityTab());
        tabs.addTab("Appearance", buildAppearanceTab());

        add(tabs);
        ThemeManager.applyTheme(getRootPane());

        reloadUsersAsync("");
        reloadAccountsAsync("");
        reloadLedgerAsync(null, null, "");
        reloadActivityAsync("");
    }

    private JPanel buildAppearanceTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(24, 24, 24, 24));
        JButton toggle = new JButton("Toggle dark / light theme");
        toggle.addActionListener(e -> {
            ThemeManager.setDarkMode(!ThemeManager.isDarkMode());
            ThemeManager.applyTheme(getRootPane());
        });
        p.add(toggle, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildUsersTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Search"));
        north.add(userSearch);
        JButton go = new JButton("Search");
        go.addActionListener(e -> reloadUsersAsync(userSearch.getText()));
        JButton all = new JButton("Load all");
        all.addActionListener(e -> {
            userSearch.setText("");
            reloadUsersAsync("");
        });
        JButton deactivate = new JButton("Toggle selected user active flag");
        deactivate.addActionListener(e -> toggleActiveSelected());
        north.add(go);
        north.add(all);
        north.add(deactivate);
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAccountsTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Account # contains"));
        north.add(accountSearch);
        JButton go = new JButton("Search");
        go.addActionListener(e -> reloadAccountsAsync(accountSearch.getText()));
        JButton freeze = new JButton("Freeze selected");
        freeze.addActionListener(e -> setStatusSelected(AccountStatus.FROZEN));
        JButton active = new JButton("Activate selected");
        active.addActionListener(e -> setStatusSelected(AccountStatus.ACTIVE));
        north.add(go);
        north.add(freeze);
        north.add(active);
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(accountsTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildLedgerTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ledgerFrom.setToolTipText("yyyy-MM-dd");
        ledgerTo.setToolTipText("yyyy-MM-dd");
        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> reloadLedgerAsync(parseDayStart(ledgerFrom.getText()),
                parseDayEnd(ledgerTo.getText()), ledgerContains.getText()));
        JButton export = new JButton("Export CSV…");
        export.addActionListener(e -> exportLedger());
        north.add(new JLabel("From"));
        north.add(ledgerFrom);
        north.add(new JLabel("To"));
        north.add(ledgerTo);
        north.add(new JLabel("Contains"));
        north.add(ledgerContains);
        north.add(apply);
        north.add(export);
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(ledgerTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildActivityTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Action contains"));
        north.add(activityFilter);
        JButton go = new JButton("Filter");
        go.addActionListener(e -> reloadActivityAsync(activityFilter.getText()));
        JButton recent = new JButton("Recent 500");
        recent.addActionListener(e -> {
            activityFilter.setText("");
            reloadActivityRecent();
        });
        north.add(go);
        north.add(recent);
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(activityTable), BorderLayout.CENTER);
        return p;
    }

    private LocalDateTime parseDayStart(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        LocalDate d = LocalDate.parse(raw.trim());
        return LocalDateTime.of(d, LocalTime.MIN);
    }

    private LocalDateTime parseDayEnd(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        LocalDate d = LocalDate.parse(raw.trim());
        return LocalDateTime.of(d, LocalTime.MAX);
    }

    private void signOut() {
        dispose();
        new LoginFrame(ctx).setVisible(true);
    }

    private void reloadUsersAsync(String q) {
        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() {
                AdminService adm = ctx.admin();
                return q == null || q.isBlank() ? adm.allUsers() : adm.searchUsers(q.trim());
            }

            @Override
            protected void done() {
                try {
                    usersModel.setRowCount(0);
                    for (User u : get()) {
                        usersModel.addRow(new Object[]{
                                u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(),
                                u.isActive(), u.getFullName()
                        });
                    }
                } catch (Exception ex) {
                    Dialogs.error(AdminDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void reloadAccountsAsync(String partial) {
        new SwingWorker<List<Account>, Void>() {
            @Override
            protected List<Account> doInBackground() {
                AdminService adm = ctx.admin();
                return partial == null || partial.isBlank() ? adm.allAccounts() : adm.searchAccounts(partial.trim());
            }

            @Override
            protected void done() {
                try {
                    accountsModel.setRowCount(0);
                    for (Account a : get()) {
                        accountsModel.addRow(new Object[]{
                                a.getId(), a.getAccountNumber(), a.getUserId(),
                                a.getBalance(), a.getStatus()
                        });
                    }
                } catch (Exception ex) {
                    Dialogs.error(AdminDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void reloadLedgerAsync(LocalDateTime from, LocalDateTime to, String contains) {
        new SwingWorker<List<BankTransaction>, Void>() {
            @Override
            protected List<BankTransaction> doInBackground() {
                return ctx.admin().ledger(from, to, contains);
            }

            @Override
            protected void done() {
                try {
                    lastLedger = get();
                    ledgerModel.setRowCount(0);
                    for (BankTransaction t : lastLedger) {
                        ledgerModel.addRow(new Object[]{
                                t.getReceiptRef(), t.getTransactionType(),
                                t.getAmount(), t.getCreatedAt(), t.getDescription()
                        });
                    }
                } catch (Exception ex) {
                    Dialogs.error(AdminDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void reloadActivityAsync(String actionPart) {
        new SwingWorker<List<ActivityLog>, Void>() {
            @Override
            protected List<ActivityLog> doInBackground() {
                return actionPart == null || actionPart.isBlank()
                        ? ctx.activityLogs().recent(500)
                        : ctx.activityLogs().filter(actionPart.trim());
            }

            @Override
            protected void done() {
                try {
                    activityModel.setRowCount(0);
                    for (ActivityLog l : get()) {
                        activityModel.addRow(new Object[]{
                                l.getCreatedAt(),
                                l.getUserId() == null ? "—" : l.getUserId(),
                                l.getAction(),
                                l.getDetails()
                        });
                    }
                } catch (Exception ex) {
                    Dialogs.error(AdminDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void reloadActivityRecent() {
        new SwingWorker<List<ActivityLog>, Void>() {
            @Override
            protected List<ActivityLog> doInBackground() {
                return ctx.activityLogs().recent(500);
            }

            @Override
            protected void done() {
                try {
                    activityModel.setRowCount(0);
                    for (ActivityLog l : get()) {
                        activityModel.addRow(new Object[]{
                                l.getCreatedAt(),
                                l.getUserId() == null ? "—" : l.getUserId(),
                                l.getAction(),
                                l.getDetails()
                        });
                    }
                } catch (Exception ex) {
                    Dialogs.error(AdminDashboardFrame.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void toggleActiveSelected() {
        int r = usersTable.getSelectedRow();
        if (r < 0) {
            Dialogs.warn(this, "Select a user row.");
            return;
        }
        long uid = Long.parseLong(usersModel.getValueAt(r, 0).toString());
        Object activeCell = usersModel.getValueAt(r, 4);
        boolean current = activeCell instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(activeCell));
        if (!Dialogs.confirm(this, "Set active=" + (!current) + " for user id " + uid + "?")) {
            return;
        }
        try {
            ctx.admin().toggleUserActive(uid, !current, adminUser);
            reloadUsersAsync(userSearch.getText());
        } catch (BankException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void setStatusSelected(AccountStatus status) {
        int r = accountsTable.getSelectedRow();
        if (r < 0) {
            Dialogs.warn(this, "Select an account row.");
            return;
        }
        long aid = Long.parseLong(accountsModel.getValueAt(r, 0).toString());
        try {
            ctx.admin().setAccountStatus(aid, status, adminUser);
            reloadAccountsAsync(accountSearch.getText());
        } catch (BankException ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void exportLedger() {
        if (lastLedger.isEmpty()) {
            Dialogs.warn(this, "Ledger empty — adjust filters.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                TransactionReportExporter.exportCsv(f.toPath(), lastLedger);
                Dialogs.info(this, "Saved " + f.getAbsolutePath());
            } catch (Exception ex) {
                Dialogs.error(this, ex.getMessage());
            }
        }
    }
}
