import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class LoadingWindow extends JFrame {
    private final JProgressBar progressBar;
    private final JTextArea logArea;
    private final Handler logHandler;

    public LoadingWindow() {
        setTitle("Inventory Management - Initializing");
        setSize(600, 400); // Larger window for more impact
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(30, 30, 30)); // Dark background for modern look

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Loading label
        JLabel loadingLabel = new JLabel("Initializing Database...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 18));
        loadingLabel.setForeground(new Color(200, 200, 200)); // Light text for contrast
        mainPanel.add(loadingLabel, BorderLayout.NORTH);

        // Progress bar (indeterminate)
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(550, 25));
        progressBar.setForeground(new Color(0, 120, 215)); // Blue progress bar for visual pop
        progressBar.setBackground(new Color(50, 50, 50));
        mainPanel.add(progressBar, BorderLayout.CENTER);

        // Log area (terminal-like)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setRows(15); // More rows for more logs
        logArea.setBackground(new Color(40, 40, 40)); // Dark terminal background
        logArea.setForeground(new Color(255, 255, 255));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        mainPanel.add(logScrollPane, BorderLayout.SOUTH);

        add(mainPanel);

        // Custom logging handler to redirect logs to JTextArea
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                SwingUtilities.invokeLater(() -> {
                    String message = String.format("[%tF %tT] [%s] %s: %s%n",
                            record.getMillis(), record.getMillis(),
                            record.getLevel(),
                            record.getLoggerName(),
                            record.getMessage());
                    logArea.append(message);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };

        // Add handler to the root logger
        java.util.logging.Logger.getLogger("").addHandler(logHandler);

        setVisible(true);
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void close() {
        // Remove the custom logging handler
        java.util.logging.Logger.getLogger("").removeHandler(logHandler);
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
        });
    }
}