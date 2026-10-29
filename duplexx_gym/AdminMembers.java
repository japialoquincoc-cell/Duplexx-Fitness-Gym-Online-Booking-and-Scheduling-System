package duplexx_gym;

import duplexx.DBConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class AdminMembers extends JFrame {

    // Change this if your members table has a different name
    private static final String USERS = "user";

    private static final Color INK = new Color(0x111827);

    private final String adminName;
    private final JLabel lblCount = new JLabel("Total members: 0");
    private final JTextField txtSearch = new JTextField(20);
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public AdminMembers(String adminName) {
        this.adminName = (adminName == null || adminName.isEmpty()) ? "Admin" : adminName;
        initUI();
        loadMembers();
    }

    private void initUI() {
        setTitle("Duplexx Fitness Gym - Members");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== Header + nav =====
        JPanel top = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(INK);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel("Duplexx Fitness Gym \u2014 Members");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel welcome = new JLabel("Welcome, " + adminName + "!");
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        welcome.setForeground(Color.WHITE);
        header.add(title);
        header.add(welcome);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        nav.setBackground(new Color(0x1F2937));

        JButton btnDashboard = new JButton("Dashboard");
        btnDashboard.addActionListener(e -> {
            new AdminPanel(adminName).setVisible(true);
            this.dispose();
        });

        JButton btnTrainers = new JButton("Trainers");
        btnTrainers.addActionListener(e -> {
            new AdminTrainers(adminName).setVisible(true);
            this.dispose();
        });

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadMembers());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            Home home = new Home();
        home.setLocationRelativeTo(null);
        home.setVisible(true);
        this.dispose();
        });

        nav.add(btnDashboard);
        nav.add(btnTrainers);
        nav.add(btnRefresh);
        nav.add(btnLogout);

        top.add(header, BorderLayout.NORTH);
        top.add(nav, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ===== Main =====
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel heading = new JLabel("Registered Members");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(INK);

        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchRow.add(new JLabel("Search:"));
        searchRow.add(txtSearch);

        JPanel headRow = new JPanel(new BorderLayout());
        JPanel headLeft = new JPanel(new GridLayout(2, 1));
        headLeft.add(heading);
        headLeft.add(lblCount);
        headRow.add(headLeft, BorderLayout.WEST);
        headRow.add(searchRow, BorderLayout.EAST);
        main.add(headRow, BorderLayout.NORTH);

        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(24);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        add(main, BorderLayout.CENTER);
    }

    private void applyFilter() {
        String text = txtSearch.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        }
    }

    /**
     * Loads every column of the members table (except password columns),
     * newest first, so it works with whatever columns your table has
     * (name, email, join date, etc.).
     */
    private void loadMembers() {
        model.setRowCount(0);
        model.setColumnCount(0);

        String newestFirst = "SELECT * FROM " + USERS + " ORDER BY user_id DESC";
        String plain = "SELECT * FROM " + USERS;

        if (!runQuery(newestFirst) && !runQuery(plain)) {
            JOptionPane.showMessageDialog(this,
                    "Could not load members from table '" + USERS + "'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        lblCount.setText("Total members: " + model.getRowCount());
    }

    private boolean runQuery(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            ResultSetMetaData md = rs.getMetaData();
            List<Integer> shown = new ArrayList<>();
            model.setRowCount(0);
            model.setColumnCount(0);

            for (int i = 1; i <= md.getColumnCount(); i++) {
                String col = md.getColumnLabel(i);
                if (col.toLowerCase().contains("password")) {
                    continue; // never show passwords
                }
                shown.add(i);
                model.addColumn(prettify(col));
            }

            while (rs.next()) {
                Object[] row = new Object[shown.size()];
                for (int k = 0; k < shown.size(); k++) {
                    Object v = rs.getObject(shown.get(k));
                    row[k] = v == null ? "" : v.toString();
                }
                model.addRow(row);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String prettify(String col) {
        String[] parts = col.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}