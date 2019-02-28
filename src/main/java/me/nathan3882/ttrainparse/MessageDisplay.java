package me.nathan3882.ttrainparse;

import com.sun.istack.internal.Nullable;

import javax.swing.*;

public class MessageDisplay {

    protected void displayMessage(String message) {
        JOptionPane.showMessageDialog(getPanel(), message);
    }

    @Nullable
    protected JPanel getPanel() {
        return null;
    }
}
