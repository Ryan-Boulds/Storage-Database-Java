import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.*;

public class UIComponentUtils {
    public static JLabel createAlignedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JTextField createFormattedTextField() {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(450, 30));
        textField.setMaximumSize(new Dimension(450, 30));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        return textField;
    }

    public static JComboBox<String> createFormattedComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setPreferredSize(new Dimension(450, 30));
        comboBox.setMaximumSize(new Dimension(450, 30));
        comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return comboBox;
    }

    public static JButton createFormattedButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    public static JCheckBox createFormattedCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return checkBox;
    }

    public static JPanel createFormattedDatePicker() {
        JPanel datePanel = new JPanel();
        datePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setMaximumSize(new Dimension(450, 30));
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField dateField = new JTextField();
        dateField.setPreferredSize(new Dimension(400, 30));
        dateField.setMaximumSize(new Dimension(400, 30));
        dateField.setEditable(false);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        dateField.setText(dateFormat.format(new Date()));

        JButton calendarButton = new JButton("...");
        calendarButton.setPreferredSize(new Dimension(40, 30));
        calendarButton.setMaximumSize(new Dimension(40, 30));

        calendarButton.addActionListener(e -> {
            JDialog calendarDialog = new JDialog((Frame) null, "Select Date", true);
            calendarDialog.setLayout(new BorderLayout());
            calendarDialog.setSize(250, 200);
            calendarDialog.setLocationRelativeTo(datePanel);

            JPanel calendarPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            calendarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            Calendar cal = Calendar.getInstance();

            JComboBox<String> monthCombo = new JComboBox<>(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            });
            monthCombo.setPreferredSize(new Dimension(200, 30));
            monthCombo.setMaximumSize(new Dimension(200, 30));
            monthCombo.setSelectedIndex(cal.get(Calendar.MONTH));

            JComboBox<Integer> dayCombo = new JComboBox<>();
            dayCombo.setPreferredSize(new Dimension(200, 30));
            dayCombo.setMaximumSize(new Dimension(200, 30));

            JComboBox<Integer> yearCombo = new JComboBox<>();
            for (int i = cal.get(Calendar.YEAR) - 10; i <= cal.get(Calendar.YEAR) + 10; i++) {
                yearCombo.addItem(i);
            }
            yearCombo.setPreferredSize(new Dimension(200, 30));
            yearCombo.setMaximumSize(new Dimension(200, 30));
            yearCombo.setSelectedItem(cal.get(Calendar.YEAR));

            updateDayCombo(dayCombo, yearCombo, monthCombo);
            ActionListener updateDays = e2 -> updateDayCombo(dayCombo, yearCombo, monthCombo);
            yearCombo.addActionListener(updateDays);
            monthCombo.addActionListener(updateDays);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            okButton.setPreferredSize(new Dimension(80, 30));
            cancelButton.setPreferredSize(new Dimension(80, 30));
            okButton.addActionListener(e2 -> {
                int year = (Integer) yearCombo.getSelectedItem();
                int month = monthCombo.getSelectedIndex();
                int day = (Integer) dayCombo.getSelectedItem();
                cal.set(year, month, day);
                dateField.setText(dateFormat.format(cal.getTime()));
                calendarDialog.dispose();
            });
            cancelButton.addActionListener(e2 -> calendarDialog.dispose());
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            calendarPanel.add(new JLabel("Month:"));
            calendarPanel.add(monthCombo);
            calendarPanel.add(new JLabel("Day:"));
            calendarPanel.add(dayCombo);
            calendarPanel.add(new JLabel("Year:"));
            calendarPanel.add(yearCombo);
            calendarDialog.add(calendarPanel, BorderLayout.CENTER);
            calendarDialog.add(buttonPanel, BorderLayout.SOUTH);
            calendarDialog.setVisible(true);
        });

        datePanel.add(dateField);
        datePanel.add(calendarButton);
        return datePanel;
    }

    private static void updateDayCombo(JComboBox<Integer> dayCombo, JComboBox<Integer> yearCombo, JComboBox<String> monthCombo) {
        dayCombo.removeAllItems();
        Calendar cal = Calendar.getInstance();
        cal.set((Integer) yearCombo.getSelectedItem(), monthCombo.getSelectedIndex(), 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            dayCombo.addItem(i);
        }
        dayCombo.setSelectedIndex(0);
    }

    public static JScrollPane createScrollableContentPanel(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(60);
        scrollPane.setDoubleBuffered(true);
        scrollPane.addMouseWheelListener(e -> {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            int unitsToScroll = e.getUnitsToScroll() * 10;
            int newPosition = verticalScrollBar.getValue() + unitsToScroll;
            verticalScrollBar.setValue(newPosition);
        });
        return scrollPane;
    }

    public static JFrame createMainFrame(String title, JPanel... tabs) {
        JFrame frame = new JFrame(title);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        for (JPanel tab : tabs) {
            String tabName = tab.getClass().getSimpleName().replace("Tab", "");
            tabbedPane.addTab(tabName, tab);
        }

        frame.add(tabbedPane);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                FileUtils.saveDevices();
                FileUtils.saveCables();
                FileUtils.saveAccessories();
                FileUtils.saveTemplates();
                System.exit(0);
            }
        });

        return frame;
    }

    public static String getSelectedPorts(JCheckBox... ports) {
        StringBuilder sb = new StringBuilder();
        for (JCheckBox port : ports) {
            if (port.isSelected()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(port.getText());
            }
        }
        return sb.length() > 0 ? sb.toString() : "None";
    }

    public static String getDateFromPicker(JPanel datePicker) {
        for (Component comp : datePicker.getComponents()) {
            if (comp instanceof JTextField) {
                return ((JTextField) comp).getText();
            }
        }
        return "";
    }
}