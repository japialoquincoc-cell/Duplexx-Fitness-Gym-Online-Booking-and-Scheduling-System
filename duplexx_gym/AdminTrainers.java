package duplexx_gym;

import duplexx.DBConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/** Admin > Manage Trainers (add / list / delete). Styled like AdminPanel. */
public class AdminTrainers extends JFrame {

    private static final Color INK = new Color(0x111827);
    private static final Color NAV = new Color(0x1F2937);
    private static final Color BORDER = new Color(0xE5E7EB);

    private final String adminName;

    private final JTextField txtName = new JTextField();
    private final JTextField txtSpec = new JTextField();
    private final JTextField txtUser = new JTextField();
    private final JPasswordField txtPass = new JPasswordField();

    private DefaultTableModel model;
    private JTable table;

    public AdminTrainers(String adminName) {
        this.adminName = (adminName == null || adminName.isEmpty()) ? "Admin" : adminName;
        initUI();
        loadTrainers();
    }

    // ================= UI =================

    private void initUI() {
        setTitle("Duplexx Fitness Gym - Manage Trainers");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== Header + nav (same as AdminPanel) =====
        JPanel top = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(INK);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel("Duplexx Fitness Gym \u2014 Manage Trainers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel welcome = new JLabel("Welcome, " + adminName + "!");
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        welcome.setForeground(Color.WHITE);
        header.add(title);
        header.add(welcome);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        nav.setBackground(NAV);

        JButton btnDashboard = new JButton("Dashboard");
        btnDashboard.addActionListener(e -> goDashboard());

        JButton btnMembers = new JButton("Members");
        btnMembers.addActionListener(e -> {
            new AdminMembers(adminName).setVisible(true);
            this.dispose();
        });

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadTrainers());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> logout());

        nav.add(btnDashboard);
        nav.add(btnMembers);
        nav.add(btnRefresh);
        nav.add(btnLogout);

        top.add(header, BorderLayout.NORTH);
        top.add(nav, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ===== Main =====
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        main.add(buildAddCard(), BorderLayout.NORTH);
        main.add(buildListCard(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildAddCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBorder(cardBorder());

        JLabel h = new JLabel("Add Trainer");
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        h.setForeground(INK);
        card.add(h, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(2, 2, 16, 8));
        form.add(field("Trainer name", txtName));
        form.add(field("Specialization", txtSpec));
        form.add(field("Trainer username", txtUser));
        form.add(field("Password (8+ characters)", txtPass));
        card.add(form, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add Trainer");
        btnAdd.addActionListener(e -> addTrainer());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        south.add(btnAdd);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildListCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));

        JLabel h = new JLabel("Trainer List");
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        h.setForeground(INK);
        card.add(h, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new String[]{"ID", "Name", "Specialization", "Username"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        card.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnDelete = new JButton("Delete Selected Trainer");
        btnDelete.addActionListener(e -> deleteSelected());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        south.add(btnDelete);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    private JPanel field(String label, JTextField input) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JPanel w = new JPanel(new BorderLayout(0, 4));
        w.add(l, BorderLayout.NORTH);
        w.add(input, BorderLayout.CENTER);
        w.setPreferredSize(new Dimension(280, 56));
        return w;
    }

    // ================= DATABASE =================

    private void loadTrainers() {
        model.setRowCount(0);
        String sql = "SELECT trainer_id, trainer_name, specialization, username "
                + "FROM trainers ORDER BY trainer_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("trainer_id"),
                    rs.getString("trainer_name"),
                    rs.getString("specialization"),
                    rs.getString("username")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not load trainers: " + e.getMessage());
        }
    }

    private void addTrainer() {
        String name = txtName.getText().trim();
        String spec = txtSpec.getText().trim();
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (name.isEmpty() || spec.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }
        if (pass.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT COUNT(*) FROM trainers WHERE username = ?")) {
                chk.setString(1, user);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this, "That username is already taken.");
                        return;
                    }
                }
            }

            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO trainers (trainer_name, specialization, username, password) "
                    + "VALUES (?, ?, ?, ?)")) {
                ins.setString(1, name);
                ins.setString(2, spec);
                ins.setString(3, user);
                ins.setString(4, pass);
                ins.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Trainer added!");
            txtName.setText("");
            txtSpec.setText("");
            txtUser.setText("");
            txtPass.setText("");
            loadTrainers();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a trainer first.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String name = String.valueOf(model.getValueAt(row, 1));

        if (JOptionPane.showConfirmDialog(this, "Delete trainer \"" + name + "\"?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "DELETE FROM trainers WHERE trainer_id = ?")) {
            pst.setInt(1, id);
            pst.executeUpdate();
            loadTrainers();
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "This trainer still has bookings, so it can't be deleted.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ================= NAVIGATION =================

    private void goDashboard() {
        new AdminPanel(adminName).setVisible(true);
        this.dispose();
    }

    private void logout() {
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
    }
}