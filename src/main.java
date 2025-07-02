import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class main {

    public static void main(String[] args) {
        // Set up the frame
        JFrame frame = new JFrame("Tabbed Pane Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        // Create a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Create the first panel with buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton button1 = new JButton("Button 1");
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button 1 pressed");
            }
        });
        
        JButton button2 = new JButton("Button 2");
        button2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Button 2 pressed");
            }
        });

        buttonPanel.add(button1);
        buttonPanel.add(button2);

        // Create the second panel with a text box and an Enter button
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());

        JTextField textField = new JTextField(15);
        JButton enterButton = new JButton("Enter");

        // Action listener for the Enter button
        enterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Print the text field content to the terminal
                System.out.println("Text entered: " + textField.getText());
            }
        });

        // Add components to the second panel
        textPanel.add(new JLabel("Enter text and press Enter:"));
        textPanel.add(textField);
        textPanel.add(enterButton);



        // created a demo tab
        JPanel demoPanel = new JPanel();
        demoPanel.setLayout(new FlowLayout());


        // Add tabs to the tabbed pane
        tabbedPane.addTab("Buttons", buttonPanel);
        tabbedPane.addTab("Text Box", textPanel);
         tabbedPane.addTab("demo tab", demoPanel);



        // Add the tabbed pane to the frame
        frame.add(tabbedPane);

        // Display the frame
        frame.setVisible(true);
    }
}
