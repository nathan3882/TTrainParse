package me.nathan.brockapptesting;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginRegisterForm {
    private final TTrainParser mainInstance;
    private JPanel loginRegisterPanel;
    private JTextField emailTextField;
    private String startingEmailTextFieldText;
    private JLabel aboveEverythingLabel;

    public LoginRegisterForm(TTrainParser mainInstance) {
        this.mainInstance = mainInstance;
        emailTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (emailTextField.getText().equalsIgnoreCase("Enter email")) {
                    emailTextField.setText(" ");
                }
            }
        });
    }

    public JPanel getLoginRegisterPanel() {
        return loginRegisterPanel;
    }

}
