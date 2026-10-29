package duplexx_gym;

import duplexx.DBConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/** Member > My Bookings. Shows the logged-in member's bookings from the database. */
public class MyBookings extends JFrame {

    // Background picture (same one your Book a Gym Session form uses)
    private static final String BG_PATH =
            "/Images/814493313_969790048738559_5385946376610830579_n (1).jpg";

    private static final Color RED = new Color(0xDC3545);
    private static final Color CARD_BG = new Color(20, 20, 20, 235);

    private BackgroundPanel root;
    private JPanel listPanel;

    public MyBookings() {
        initUI();
        loadBookings();
    }

    // ================= UI =================

    private void initUI() {
        setTitle("Duplexx Fitness Gym");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800,500);
        setLocationRelativeTo(null);

        root = new BackgroundPanel(BG_PATH);
        root.setLayout(new BorderLayout());
        setContentPane(root);

        // ----- title -----
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createEmptyBorder(20, 0, 6, 0));

        JLabel title = new JLabel("My Bookings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Check the status of your gym sessions.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        sub.setForeground(new Color(0xCCCCCC));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        head.add(title);
        head.add(Box.createVerticalStrut(6));
        head.add(sub);
        root.add(head, BorderLayout.NORTH);

        // ----- list of booking cards + buttons (scrollable) -----
        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(6, 24, 20, 24));

        JScrollPane scroll = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 18, 0));
        p.setOpaque(false);
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        p.add(redButton("Dashboard", e -> {
            Dashboard d = new Dashboard();
            d.setLocationRelativeTo(null);
            d.setVisible(true);
            dispose();
        }));
        p.add(redButton("Book a Session", e -> {
            BookingSession b = new BookingSession();
            b.setLocationRelativeTo(null);
            b.setVisible(true);
            dispose();
        }));
        p.add(redButton("Logout", e -> {
           Home home = new Home();
        home.setLocationRelativeTo(null);
        home.setVisible(true);
        this.dispose();
        }));
        return p;
    }

    private JButton redButton(String text, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        b.setBackground(RED);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    // ================= DATABASE =================

    private void loadBookings() {
        listPanel.removeAll();

        int userId = UserSession.getUserId();   // match the getter name in your UserSession.java
        boolean any = false;

        if (userId > 0) {
            String sql = "SELECT b.booking_id, t.trainer_name, t.specialization, "
                    + "b.booking_date, b.booking_time, b.status "
                    + "FROM bookings b LEFT JOIN trainers t ON b.trainer_id = t.trainer_id "
                    + "WHERE b.user_id = ? "
                    + "ORDER BY b.booking_date DESC, b.booking_time DESC";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        any = true;
                        listPanel.add(buildCard(
                                rs.getInt("booking_id"),
                                nz(rs.getString("trainer_name")),
                                nz(rs.getString("specialization")),
                                nz(rs.getString("booking_date")),
                                formatTime(rs.getString("booking_time")),
                                nz(rs.getString("status"))));
                        listPanel.add(Box.createVerticalStrut(16));
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Could not load bookings: " + e.getMessage());
            }
        }

        if (!any) {
            String msg = userId > 0 ? "You have no bookings yet." : "Please log in to see your bookings.";
            listPanel.add(buildEmptyCard(msg));
            listPanel.add(Box.createVerticalStrut(16));
        }

        listPanel.add(Box.createVerticalStrut(10));
        listPanel.add(buildButtons());

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void cancelBooking(int bookingId) {
        if (JOptionPane.showConfirmDialog(this, "Cancel this booking?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "UPDATE bookings SET status = 'Cancelled' WHERE booking_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bookingId);
            pst.setInt(2, UserSession.getUserId());
            pst.executeUpdate();
            loadBookings();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ================= CARDS =================

    private JPanel buildCard(int bookingId, String trainer, String spec,
                             String date, String time, String status) {
        RoundedPanel card = new RoundedPanel(CARD_BG);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        card.setAlignmentX(CENTER_ALIGNMENT);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.5;
        c.insets = new Insets(4, 0, 8, 0);

        // trainer name (red)
        JLabel name = new JLabel(trainer);
        name.setFont(new Font("Segoe UI", Font.BOLD, 22));
        name.setForeground(new Color(0xE63946));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        card.add(name, c);

        // specialization + date
        c.gridwidth = 1;
        c.gridy = 1;
        c.gridx = 0; card.add(infoLabel("Specialization:", spec), c);
        c.gridx = 1; card.add(infoLabel("Date:", date), c);

        // time
        c.gridy = 2;
        c.gridx = 0; card.add(infoLabel("Time:", time), c);

        // status banner
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2;
        c.insets = new Insets(12, 0, 0, 0);
        card.add(statusBanner(status), c);

        // cancel button (pending only)
        if (status.equalsIgnoreCase("Pending")) {
            JButton cancel = new JButton("Cancel Booking");
            cancel.setFocusPainted(false);
            cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cancel.addActionListener(e -> cancelBooking(bookingId));
            c.gridy = 4;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(12, 0, 0, 0);
            card.add(cancel, c);
        }

        card.setMaximumSize(new Dimension(1250, card.getPreferredSize().height));
        return card;
    }

    private JPanel buildEmptyCard(String text) {
        RoundedPanel card = new RoundedPanel(CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        card.setAlignmentX(CENTER_ALIGNMENT);
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        l.setForeground(Color.WHITE);
        card.add(l);
        card.setMaximumSize(new Dimension(1250, 110));
        return card;
    }

    private JLabel infoLabel(String key, String value) {
        JLabel l = new JLabel("<html><b>" + esc(key) + "</b> " + esc(value) + "</html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        l.setForeground(Color.WHITE);
        return l;
    }

    private JLabel statusBanner(String status) {
        String text;
        Color bg;
        Color fg;
        String s = status.toLowerCase(Locale.ENGLISH);

        if (s.equals("pending")) {
            text = "Status: Pending \u2014 Waiting for trainer confirmation.";
            bg = new Color(0x5C4A00);
            fg = new Color(0xFFD43B);
        } else if (s.equals("confirmed") || s.equals("approved") || s.equals("accepted")) {
            text = "Status: " + status + " \u2014 See you at the gym!";
            bg = new Color(0x14532D);
            fg = new Color(0x86EFAC);
        } else {
            text = "Status: " + status;
            bg = new Color(0x5B1A1A);
            fg = new Color(0xFCA5A5);
        }

        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(new Font("Segoe UI", Font.BOLD, 18));
        l.setBorder(BorderFactory.createEmptyBorder(18, 10, 18, 10));
        return l;
    }

    // ================= HELPERS =================

    private static String nz(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatTime(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "-";
        }
        try {
            String t = raw.length() > 8 ? raw.substring(0, 8) : raw;
            return LocalTime.parse(t).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        } catch (Exception e) {
            return raw;
        }
    }

    // ================= CUSTOM PANELS =================

    /** Dark rounded card. */
    private static class RoundedPanel extends JPanel {
        private final Color fill;

        RoundedPanel(Color fill) {
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Background picture, scaled to fill the window, with a dark overlay. */
    private static class BackgroundPanel extends JPanel {
        private Image img;

        BackgroundPanel(String path) {
            URL url = MyBookings.class.getResource(path);
            if (url != null) {
                img = new ImageIcon(url).getImage();
            }
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}