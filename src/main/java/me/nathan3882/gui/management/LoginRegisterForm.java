package me.nathan3882.gui.management;

import me.nathan3882.data.DataFileInfo;
import me.nathan3882.ttrainparse.MessageDisplay;
import me.nathan3882.ttrainparse.TTrainParser;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;

import javax.swing.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginRegisterForm extends MessageDisplay {
    private JPanel loginRegisterPanel;
    private JTextField emailTextField;

    private String startingEmailTextFieldText;
    private JLabel aboveEverythingLabel;
    private JButton advanceToTrainsButton;
    private TTrainParser mainInstance;

    public LoginRegisterForm(TTrainParser mainInstance) {
        this.mainInstance = mainInstance;
        mainInstance.loginRegisterForm = this;
        advanceToTrainsButton.setEnabled(true);

        emailTextField.addMouseListener(getMouseListener());
        advanceToTrainsButton.addActionListener(getActionListener());

    }

    private ActionListener getActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String emailText = emailTextField.getText();
                if (isValidEmailAddress(emailText)) {
                    DataFileInfo info = new DataFileInfo();
                    YamlReader reader = null;
                    try {
                        reader = new YamlReader(new FileReader(TTrainParser.USER_DIRECTORY_FILE_SEP + "data.yml"));
                        info = reader.read(DataFileInfo.class);
                    } catch (FileNotFoundException | YamlException exception) {
                        exception.printStackTrace();
                    }
                    if (reader == null) return;

                    info.setEmail(emailText);
                    info.setTimetableCroppedPngFileName(info.timetableCroppedPngFileName);

                    YamlWriter writer = null;
                    try { //TODO Store System current millis for the time which the user had first timetable parsed
                        writer = new YamlWriter(new FileWriter(TTrainParser.USER_DIRECTORY_FILE_SEP + "data.yml"));
                        writer.write(info); //writes previously collected data about jpg & pdf file names
                        writer.close();
                    } catch (IOException | YamlException e1) {
                        e1.printStackTrace();
                    }
                    mainInstance.openPanel(TTrainParser.CORE_PANEL);
                } else {
                    displayMessage("'" + emailText + "'\nis not a valid email!");
                }
            }
        };
    }

    private MouseListener getMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (emailTextField.getText().equalsIgnoreCase("Enter email")) {
                    emailTextField.setText("");
                }
            }
        };
    }

    private boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        Pattern p = Pattern.compile(ePattern);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    @Override
    public JPanel getPanel() {
        return this.loginRegisterPanel;
    }
}
