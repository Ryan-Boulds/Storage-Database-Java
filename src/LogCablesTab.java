import javax.swing.*;

public class LogCablesTab extends JPanel {

    public LogCablesTab() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Create a dropdown for cables
        JComboBox<String> cableTypeCombo = UIUtils.createFormattedComboBox(
            new String[]{"HDMI", "DisplayPort", "Ethernet", "USB-C to USB-C", 
                         "USB-A to USB-C", "USB-A to USB-B", "Headset"}
        );

        // Add buttons to add/remove cables from storage
        JButton addToStorageButton = UIUtils.createFormattedButton("Add to Storage");
        JButton removeFromStorageButton = UIUtils.createFormattedButton("Remove from Storage");

        addToStorageButton.addActionListener(e -> 
            System.out.println(cableTypeCombo.getSelectedItem() + " added to storage.")
        );

        removeFromStorageButton.addActionListener(e -> 
            System.out.println(cableTypeCombo.getSelectedItem() + " removed from storage.")
        );

        // Add components to the panel
        add(UIUtils.createAlignedLabel("Select Cable Type:"));
        add(cableTypeCombo);
        add(addToStorageButton);
        add(removeFromStorageButton);
    }
}