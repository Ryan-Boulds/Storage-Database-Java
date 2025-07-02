import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class LogCablesTab extends JPanel {

    public LogCablesTab() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // Stack components vertically

        // Create a dropdown for cables
        JComboBox<String> cableTypeCombo = new JComboBox<>(new String[]{
            "HDMI", "DisplayPort", "Ethernet", "USB-C to USB-C", "USB-A to USB-C", "USB-A to USB-B", "Headset"
        });
        cableTypeCombo.setPreferredSize(new Dimension(200, 30));

        // Add buttons to add/remove cables from storage
        JButton addToStorageButton = new JButton("Add to Storage");
        JButton removeFromStorageButton = new JButton("Remove from Storage");

        addToStorageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(cableTypeCombo.getSelectedItem() + " added to storage.");
            }
        });

        removeFromStorageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(cableTypeCombo.getSelectedItem() + " removed from storage.");
            }
        });

        // Add components to the panel
        add(new JLabel("Select Cable Type:"));
        add(cableTypeCombo);
        add(addToStorageButton);
        add(removeFromStorageButton);
    }
}
