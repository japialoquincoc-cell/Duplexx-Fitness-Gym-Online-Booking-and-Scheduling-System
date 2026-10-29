package duplexx_gym;

import com.toedter.calendar.JDateChooser;
import duplexx.DBConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Trainer dashboard (dark card design with red table header, status pills and
 * per-row action buttons). Sized for 1000 x 588.
 *
 * Workflow
 *   Pending                  -> Confirmed, Cancelled (Reject)
 *   Confirmed / Rescheduled  -> Checked-in, No-show, Rescheduled, Cancelled
 *   Checked-in               -> Completed
 *   Completed / Cancelled / No-show -> final (no more actions)
 */
public class TrainerPanel extends JFrame {

    public static final String PENDING = "Pending";
    public static final String CONFIRMED = "Confirmed";
    public static final String CHECKED_IN = "Checked-in";
    public static final String COMPLETED = "Completed";
    public static final String CANCELLED = "Cancelled";
    public static final String NO_SHOW = "No-show";
    public static final String RESCHEDULED = "Rescheduled";

    // Column of the user table that holds the member's email (run: DESCRIBE user;)
    private static final String USER_EMAIL_COL = "u.email";

    // Same background image you use on the other forms. If it is not found the background is plain black.
    private static final String BG_IMAGE = "/Images/814493313_969790048738559_5385946376610830579_n (1).jpg";

    private static final int ROW_H = 56;
    private static final Color GREEN = new Color(40, 167, 69);
    private static final Color BLUE = new Color(0, 123, 255);
    private static final Color TEAL = new Color(23, 162, 184);
    private static final Color PURPLE = new Color(111, 66, 193);
    private static final Color GRAY = new Color(108, 117, 125);
    private static final Color RED = new Color(220, 53, 69);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static final String[] TIMES = {
        "06:00 AM", "07:00 AM", "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
        "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM",
        "06:00 PM", "07:00 PM", "08:00 PM"
    };

    private final int trainerId;

    // model columns: 0 id (hidden), 1 email, 2 date, 3 time, 4 status, 5 actions
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Email", "Date", "Time", "Status", "Actions"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{
        "All", PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED});

    /** Call with the logged-in trainer's id (trainers table) and display name. */
    public TrainerPanel(int trainerId, String trainerName) {
        this.trainerId = trainerId;
        buildUi(trainerName);
        loadBookings();
    }

    // ------------------------------------------------------------------ UI

    private void buildUi(String trainerName) {
        setTitle("Duplexx Fitness Gym");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 588);
        setMinimumSize(new Dimension(1000, 588));
        setLocationRelativeTo(null);
        // setResizable(false);   // uncomment to lock the size

        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout(0, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // ---- header card
        RoundedPanel header = new RoundedPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel title = new JLabel("Trainer Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);

        JLabel welcome = new JLabel("<html><span style='color:#bbbbbb'>Welcome, </span>"
                + "<b style='color:#ffffff'>" + escape(trainerName) + "</b></html>");
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(javax.swing.Box.createVerticalStrut(4));
        titleBox.add(welcome);
        header.add(titleBox, BorderLayout.WEST);

        RoundButton logout = new RoundButton("Logout", RED, 20, new Insets(8, 20, 8, 20));
        logout.addActionListener(e -> {
            dispose();
            new Home().setVisible(true);
        });
        JPanel logoutBox = new JPanel(new GridBagLayout());
        logoutBox.setOpaque(false);
        logoutBox.add(logout);
        header.add(logoutBox, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ---- bookings card
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 22, 18, 22));

        JLabel cardTitle = new JLabel("Member Bookings");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        cardTitle.setForeground(Color.WHITE);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);
        JLabel showLbl = new JLabel("Show:");
        showLbl.setForeground(new Color(200, 200, 200));
        showLbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        RoundButton refresh = new RoundButton("Refresh", new Color(70, 70, 70), 14, new Insets(7, 16, 7, 16));
        refresh.addActionListener(e -> loadBookings());
        filter.addActionListener(e -> loadBookings());
        tools.add(showLbl);
        tools.add(filter);
        tools.add(refresh);

        JPanel cardTop = new JPanel(new BorderLayout());
        cardTop.setOpaque(false);
        cardTop.add(cardTitle, BorderLayout.WEST);
        cardTop.add(tools, BorderLayout.EAST);
        card.add(cardTop, BorderLayout.NORTH);

        setupTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
        scroll.getViewport().setBackground(rowBg(false));
        card.add(scroll, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void setupTable() {
        table.setRowHeight(ROW_H);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(50, 50, 50));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(rowBg(false));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // body cells
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(rowBg(selected));
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });

        // red header
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 44));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(new Color(230, 57, 70));
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 14));
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });

        // status pill + action buttons
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        ActionsCell actions = new ActionsCell();
        table.getColumnModel().getColumn(5).setCellRenderer(actions);
        table.getColumnModel().getColumn(5).setCellEditor(actions);

        int[] widths = {0, 220, 110, 100, 120, 340};
        for (int i = 1; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.removeColumn(table.getColumnModel().getColumn(0));   // hide ID (stays in the model)
    }

    private static Color rowBg(boolean selected) {
        return selected ? new Color(40, 40, 40) : new Color(22, 22, 22);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    // ------------------------------------------------------------- renderers

    private static Color pillFill(String s) {
        switch (s) {
            case PENDING:     return new Color(84, 68, 0);
            case CONFIRMED:   return new Color(20, 70, 35);
            case CHECKED_IN:  return new Color(0, 50, 100);
            case COMPLETED:   return new Color(10, 70, 80);
            case CANCELLED:   return new Color(90, 20, 28);
            case NO_SHOW:     return new Color(62, 62, 62);
            case RESCHEDULED: return new Color(60, 40, 100);
            default:          return new Color(50, 50, 50);
        }
    }

    private static Color pillText(String s) {
        switch (s) {
            case PENDING:     return new Color(255, 193, 7);
            case CONFIRMED:   return new Color(80, 220, 120);
            case CHECKED_IN:  return new Color(90, 170, 255);
            case COMPLETED:   return new Color(60, 210, 230);
            case CANCELLED:   return new Color(255, 110, 120);
            case NO_SHOW:     return new Color(205, 205, 205);
            case RESCHEDULED: return new Color(190, 150, 255);
            default:          return Color.WHITE;
        }
    }

    private class StatusRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focus, int row, int col) {
            String s = String.valueOf(value);
            JPanel p = new JPanel(new GridBagLayout());
            p.setBackground(rowBg(selected));
            p.add(new Pill(s, pillFill(s), pillText(s)));
            return p;
        }
    }

    /** Buttons shown in the last column depend on the booking's status. */
    private class ActionsCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

        private int editingRow = -1;

        private JPanel build(int row, boolean live) {
            String status = String.valueOf(model.getValueAt(row, 4));
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, (ROW_H - 30) / 2));
            p.setBackground(rowBg(table.isRowSelected(row)));

            switch (status) {
                case PENDING:
                    btn(p, "Confirm", GREEN, live, CONFIRMED);
                    btn(p, "Reject", RED, live, CANCELLED);
                    break;
                case CONFIRMED:
                case RESCHEDULED:
                    btn(p, "Check In", BLUE, live, CHECKED_IN);
                    btn(p, "No Show", GRAY, live, NO_SHOW);
                    btn(p, "Reschedule", PURPLE, live, null);
                    btn(p, "Cancel", RED, live, CANCELLED);
                    break;
                case CHECKED_IN:
                    btn(p, "Complete", TEAL, live, COMPLETED);
                    break;
                default:
                    JLabel none = new JLabel("-");
                    none.setForeground(new Color(120, 120, 120));
                    p.add(none);
            }
            return p;
        }

        /** target == null means "reschedule". */
        private void btn(JPanel p, String text, Color color, boolean live, String target) {
            RoundButton b = new RoundButton(text, color, 12, new Insets(6, 10, 6, 10));
            if (live) {
                b.addActionListener(e -> {
                    final int r = editingRow;
                    stopCellEditing();
                    SwingUtilities.invokeLater(() -> {
                        if (target == null) {
                            reschedule(r);
                        } else {
                            change(r, target);
                        }
                    });
                });
            }
            p.add(b);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focus, int row, int col) {
            return build(row, false);
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value,
                boolean selected, int row, int col) {
            editingRow = row;
            return build(row, true);
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    // ----------------------------------------------------------------- data

    private void loadBookings() {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        model.setRowCount(0);
        String chosen = filter.getSelectedItem().toString();
        boolean all = chosen.equals("All");

        // Table is "user" (singular). The email column comes from the constant at the top.
        String sql = "SELECT b.booking_id, "
                + USER_EMAIL_COL + " AS member_email, b.booking_date, "
                + "b.booking_time, b.status "
                + "FROM bookings b JOIN user u ON u.user_id = b.user_id "
                + "WHERE b.trainer_id = ? " + (all ? "" : "AND b.status = ? ")
                + "ORDER BY b.booking_date, b.booking_time";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, trainerId);
            if (!all) {
                pst.setString(2, chosen);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Time t = rs.getTime("booking_time");
                    model.addRow(new Object[]{
                        rs.getInt("booking_id"),
                        rs.getString("member_email"),
                        rs.getDate("booking_date").toString(),
                        t.toLocalTime().format(TIME_FMT),
                        rs.getString("status"),
                        ""
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not load bookings: " + e.getMessage());
        }
    }

    /** Moves the booking to a new status, only if it is still in the status shown on screen. */
    private void change(int row, String newStatus) {
        if (row < 0 || row >= model.getRowCount()) {
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String current = String.valueOf(model.getValueAt(row, 4));

        if (newStatus.equals(CANCELLED)) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Cancel / reject this booking?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (newStatus.equals(NO_SHOW) && !slotHasPassed(row)) {
            JOptionPane.showMessageDialog(this,
                    "You can only mark No-show after the session time has passed.");
            return;
        }

        String sql = "UPDATE bookings SET status = ? "
                + "WHERE booking_id = ? AND trainer_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, newStatus);
            pst.setInt(2, id);
            pst.setInt(3, trainerId);
            pst.setString(4, current);
            if (pst.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(this,
                        "This booking was changed elsewhere. The list has been refreshed.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
        loadBookings();
    }

    private boolean slotHasPassed(int row) {
        LocalDate d = LocalDate.parse(String.valueOf(model.getValueAt(row, 2)));
        LocalTime t = LocalTime.parse(String.valueOf(model.getValueAt(row, 3)), TIME_FMT);
        return d.atTime(t).isBefore(LocalDateTime.now());
    }

    private void reschedule(int row) {
        if (row < 0 || row >= model.getRowCount()) {
            return;
        }
        int id = (int) model.getValueAt(row, 0);

        JDateChooser chooser = new JDateChooser();
        chooser.setMinSelectableDate(new Date());
        JComboBox<String> time = new JComboBox<>(TIMES);

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("New date:"));
        p.add(chooser);
        p.add(new JLabel("New time:"));
        p.add(time);

        int ok = JOptionPane.showConfirmDialog(this, p, "Reschedule booking",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        if (chooser.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please choose a new date.");
            return;
        }

        java.sql.Date sqlDate = new java.sql.Date(chooser.getDate().getTime());
        LocalTime lt = LocalTime.parse(time.getSelectedItem().toString(), TIME_FMT);
        if (sqlDate.toLocalDate().atTime(lt).isBefore(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "The new date and time must be in the future.");
            return;
        }
        Time sqlTime = Time.valueOf(lt);

        String checkSql = "SELECT COUNT(*) FROM bookings WHERE trainer_id = ? AND booking_date = ? "
                + "AND booking_time = ? AND booking_id <> ? AND status NOT IN ('Cancelled','No-show')";
        String updateSql = "UPDATE bookings SET booking_date = ?, booking_time = ?, status = ? "
                + "WHERE booking_id = ? AND trainer_id = ? "
                + "AND status IN ('Pending','Confirmed','Rescheduled')";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement chk = conn.prepareStatement(checkSql)) {
                chk.setInt(1, trainerId);
                chk.setDate(2, sqlDate);
                chk.setTime(3, sqlTime);
                chk.setInt(4, id);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this,
                                "You already have a booking at that time. Pick another slot.");
                        return;
                    }
                }
            }
            try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                upd.setDate(1, sqlDate);
                upd.setTime(2, sqlTime);
                upd.setString(3, RESCHEDULED);
                upd.setInt(4, id);
                upd.setInt(5, trainerId);
                if (upd.executeUpdate() == 0) {
                    JOptionPane.showMessageDialog(this, "This booking can no longer be rescheduled.");
                } else {
                    JOptionPane.showMessageDialog(this, "Booking rescheduled.");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
        loadBookings();
    }

    /** Quick test: opens the panel for trainer id 1. Remove once wired to your trainer login. */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new TrainerPanel(1, "jay").setVisible(true));
    }

    // ------------------------------------------------------ custom components

    /** Black background with the Duplexx image, darkened. */
    private static class BackgroundPanel extends JPanel {
        private final Image img;

        BackgroundPanel() {
            java.net.URL url = TrainerPanel.class.getResource(BG_IMAGE);
            img = url == null ? null : new ImageIcon(url).getImage();
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
            g.setColor(new Color(0, 0, 0, 195));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Dark translucent card with rounded corners. */
    private static class RoundedPanel extends JPanel {
        RoundedPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(14, 14, 14, 230));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.setColor(new Color(60, 60, 60));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Flat rounded button. */
    private static class RoundButton extends JButton {
        private final Color base;

        RoundButton(String text, Color bg, int fontSize, Insets margin) {
            super(text);
            this.base = bg;
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setMargin(margin);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = base;
            if (getModel().isPressed()) {
                c = base.darker();
            } else if (getModel().isRollover()) {
                c = base.brighter();
            }
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Fully rounded status badge. */
    private static class Pill extends JLabel {
        private final Color fill;

        Pill(String text, Color fill, Color fg) {
            super(text, SwingConstants.CENTER);
            this.fill = fill;
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}