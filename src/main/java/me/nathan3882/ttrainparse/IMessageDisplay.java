package me.nathan3882.ttrainparse;

import com.sun.istack.internal.Nullable;

import javax.swing.*;

public interface IMessageDisplay {

    default void displayMessage(String message) {
        JPanel panel = getPanel();
        if (panel != null) {
            JOptionPane.showMessageDialog(panel, message);
        }
    }

    @Nullable
    JPanel getPanel();
}
