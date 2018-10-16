package me.nathan.forms;

import me.nathan.ttrainparse.DataFileInfo;
import me.nathan.ttrainparse.TTrainParser;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

public class LoginRegisterForm {
    private final TTrainParser mainInstance;
    private JPanel loginRegisterPanel;
    private JTextField emailTextField;
    private String startingEmailTextFieldText;
    private JLabel aboveEverythingLabel;
    private JButton advanceToTrainsButton;

    public LoginRegisterForm(TTrainParser mainInstance) {
        this.mainInstance = mainInstance;
        advanceToTrainsButton.setEnabled(false);
        emailTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (emailTextField.getText().equalsIgnoreCase("Enter email")) {
                    emailTextField.setText(" ");
                }
            }
        });
        advanceToTrainsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (advanceToTrainsButton.isEnabled()) {
                    DataFileInfo info = new DataFileInfo();
                    YamlReader reader = null;
                    try {
                        reader = new YamlReader(new FileReader(TTrainParser.USER_DIRECTORY + File.separator + "data.yml"));
                        info = reader.read(DataFileInfo.class);
                    } catch (FileNotFoundException | YamlException exception) {
                        mainInstance.displayError("An error occurred whilst reading from data.yml file!");
                        exception.printStackTrace();
                    }
                    if (reader == null) {
                        mainInstance.displayError("There's no data.yml file");
                        return;
                    }
                    info.setEmail(emailTextField.getText());
                    info.setTimetableCroppepJpgFileName(info.timetableCroppedJpgFileName);
                    YamlWriter writer = null;
                    try {
                        //TODO Store System current millis for the time which the user had first timetable parsed
                        writer = new YamlWriter(new FileWriter(mainInstance.USER_DIRECTORY + File.separator + "data.yml"));
                        writer.write(info); //writes previously collected data about jpg & pdf file names
                        writer.close();
                    } catch (IOException | YamlException e1) {
                        e1.printStackTrace();
                    }
                    mainInstance.coreForm = new CoreForm(mainInstance);
                    mainInstance.corePanel = mainInstance.coreForm.getWelcomePanel();
                    mainInstance.cards.add(mainInstance.corePanel, mainInstance.CORE_PANEL);
                    mainInstance.openPanel(mainInstance.CORE_PANEL);
                }
            }
        });
        emailTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                check();
            }

            public void removeUpdate(DocumentEvent e) {
                check();
            }

            public void insertUpdate(DocumentEvent e) {
                check();
            }

            public void check() {
                String text = emailTextField.getText();
                if (isValidEmailAddress(text)) {
                    if (!advanceToTrainsButton.isEnabled()) {
                        advanceToTrainsButton.setEnabled(true);
                    }
                } else {
                    if (advanceToTrainsButton.isEnabled()) {
                        advanceToTrainsButton.setEnabled(false);
                    }
                }
            }
        });
    }

    public JPanel getLoginRegisterPanel() {
        return loginRegisterPanel;
    }

    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

}
