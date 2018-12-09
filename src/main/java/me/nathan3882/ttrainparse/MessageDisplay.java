package me.nathan3882.ttrainparse;

import javax.swing.*;

public class MessageDisplay {

    protected void displayMessage(String message) {
        JOptionPane.showMessageDialog(getPanel(), message);
    }

    public JPanel getPanel() {
        return null;
    }
}
