package duplexx_gym;

import duplexx.DBConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public class AdminPanel extends JFrame {

    // Change these if your table names are different
    private static final String USERS    = "tbl_users";
    private static final String TRAINERS = "trainers";
    private static final String BOOKINGS = "bookings";

    private static final Color INK = new Color(0x111827);

    private final String adminName;
    private final JLabel lblMembers  = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblTrainers = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblBookings = new JLabel("0", SwingConstants.LEFT);
    private final JLabel lblPending  = new JLabel("0", SwingConstants.LEFT);
    private DefaultTableModel model;

    public AdminPanel(String adminName) {
        this.adminName = (adminName == null || adminName.isEmpty()) ? "Admin" : adminName;
        initUI();
        loadData();
    }

    private void initUI() {
        setTitle("Duplexx Fitness Gym - Admin Panel");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== Header + nav =====
        JPanel top = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(INK);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel("Duplexx Fitness Gym \u2014 Admin Panel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel welcome = new JLabel("Welcome, " + adminName + "!");
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        welcome.setForeground(Color.WHITE);
        header.add(title);
        header.add(welcome);

        JPanel nav = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 8));
        nav.setBackground(new Color(0x1F2937));

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());

        JButton btnMembers = new JButton("Members");
        btnMembers.addActionListener(e -> {
            new AdminMembers(adminName).setVisible(true);
            this.dispose();
        });

        JButton btnTrainers = new JButton("Trainers");
        btnTrainers.addActionListener(e -> {
            new AdminTrainers(adminName).setVisible(true);
            this.dispose();
        });

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> logout());

        nav.add(btnRefresh);
        nav.add(btnMembers);
        nav.add(btnTrainers);
        nav.add(btnLogout);

        top.add(header, BorderLayout.NORTH);
        top.add(nav, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ===== Main =====
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel overview = new JLabel("Dashboard Overview");
        overview.setFont(new Font("Segoe UI", Font.BOLD, 24));
        overview.setForeground(INK);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.add(statCard("Total Members", lblMembers));
        cards.add(statCard("Total Trainers", lblTrainers));
        cards.add(statCard("Total Bookings", lblBookings));
        cards.add(statCard("Pending Bookings", lblPending));

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.add(overview, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);
        main.add(north, BorderLayout.NORTH);

        // Recent bookings table
        JLabel recent = new JLabel("Recent Bookings");
        recent.setFont(new Font("Segoe UI", Font.BOLD, 18));

        model = new DefaultTableModel(
                new String[]{"ID", "Member", "Trainer", "Date", "Time", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(24);

        JPanel tableBox = new JPanel(new BorderLayout(0, 8));
        tableBox.add(recent, BorderLayout.NORTH);
        tableBox.add(new JScrollPane(table), BorderLayout.CENTER);
        main.add(tableBox, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    private JPanel statCard(String label, JLabel value) {
        JPanel c = new JPanel(new GridLayout(2, 1));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        c.setPreferredSize(new Dimension(180, 90));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        value.setFont(new Font("Segoe UI", Font.BOLD, 28));
        c.add(l);
        c.add(value);
        return c;
    }

    private void loadData() {
        lblMembers.setText(String.valueOf(count("SELECT COUNT(*) FROM " + USERS)));
        lblTrainers.setText(String.valueOf(count("SELECT COUNT(*) FROM " + TRAINERS)));
        lblBookings.setText(String.valueOf(count("SELECT COUNT(*) FROM " + BOOKINGS)));
        lblPending.setText(String.valueOf(
                count("SELECT COUNT(*) FROM " + BOOKINGS + " WHERE status = 'Pending'")));
        loadRecent();
    }

    private int count(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadRecent() {
        model.setRowCount(0);

        String tail = "t.trainer_name, b.booking_date, b.booking_time, b.status "
                + "FROM " + BOOKINGS + " b "
                + "LEFT JOIN " + TRAINERS + " t ON b.trainer_id = t.trainer_id ";

        // 1st try: with the member's name
        String withName = "SELECT b.booking_id, u.name AS member_name, " + tail
                + "LEFT JOIN " + USERS + " u ON b.user_id = u.user_id "
                + "ORDER BY b.booking_id DESC LIMIT 10";

        // fallback: show the user id if the users join doesn't match your table
        String withoutName = "SELECT b.booking_id, CONCAT('User #', b.user_id) AS member_name, " + tail
                + "ORDER BY b.booking_id DESC LIMIT 10";

        if (!runRecent(withName)) {
            runRecent(withoutName);
        }
    }

    private boolean runRecent(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("booking_id"),
                    nz(rs.getString("member_name")),
                    nz(rs.getString("trainer_name")),
                    nz(rs.getString("booking_date")),
                    nz(rs.getString("booking_time")),
                    nz(rs.getString("status"))
                });
            }
            return true; // query worked, so no fallback needed
        } catch (Exception e) {
            model.setRowCount(0);
            return false;
        }
    }

    private String nz(String s) {
        return s == null ? "Unknown" : s;
    }

    private void logout() {
        int choice = javax.swing.JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        Home home = new Home();
        home.setLocationRelativeTo(null);
        home.setVisible(true);
        this.dispose();
    }
}