package me.nathan3882.gui.management;

import me.nathan3882.data.DataFileInfo;
import me.nathan3882.data.Encryption;
import me.nathan3882.data.SqlConnection;
import me.nathan3882.ttrainparse.MessageDisplay;
import me.nathan3882.ttrainparse.TTrainParser;
import me.nathan3882.ttrainparse.User;

import javax.swing.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginRegisterForm extends MessageDisplay {
    private JPanel loginRegisterPanel;
    private JTextField emailTextField;

    private String startingEmailTextFieldText;
    private JLabel aboveEverythingLabel;
    private JButton advanceToTrainsButton;
    private JPasswordField sixDigitPwField;
    private JLabel passwordHelpLabel;
    private JLabel emailHelpLabel;
    private JLabel homeCrsHelpLabel;
    private JComboBox selectHomeCrsBox;
    private TTrainParser mainInstance;

    public LoginRegisterForm(TTrainParser mainInstance) {
        this.mainInstance = mainInstance;
        mainInstance.loginRegisterForm = this;
        mainInstance.configureCrsComboBox(selectHomeCrsBox);
        User user = mainInstance.getUser();
        if (user.hasEmailPwAndHomeData()) {
            homeCrsHelpLabel.setText("<html><center>New house? Add click below if you want to change:</html></center>");
            mainInstance.changeCrsComboBoxToCurrentCrs(selectHomeCrsBox);
        } else {
            homeCrsHelpLabel.setText("<html><center>Add your home station below:</html></center>");
        }
        if (mainInstance.hasLocallyStoredEmail()) {
            aboveEverythingLabel.setText("<html><center>Welcome<br>You've probably already stored your timetable<br>please login below using your previously created account</center></html>");
        }
        emailHelpLabel.setText("<html><center>Your email goes below</center></html>");
        passwordHelpLabel.setText("<html><center>Your six digit password goes below<br>Note: These 6 digits will allow you to use the phone app too!</center></html>");
        advanceToTrainsButton.setEnabled(true);

        emailTextField.addMouseListener(getMouseListener());
        advanceToTrainsButton.addActionListener(getAdvanceToTrainsBtnListener());

    }

    private ActionListener getAdvanceToTrainsBtnListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String emailText = emailTextField.getText();
                char[] pw = sixDigitPwField.getPassword();
                String enteredPassword = new String(pw);
                if (isValidEmailAddress(emailText)) {
                    if (isValidPasscode(pw)) {
                        if (selectHomeCrsBox.getSelectedIndex() != -1) {
                            String selected = (String) selectHomeCrsBox.getSelectedItem();
                            String crs = selected.split(" / ")[0];
                            mainInstance.doDatafileChecks();
                            mainInstance.getUser().setEmail(emailText);
                            if (mainInstance.hasInternet() && mainInstance.getSqlConnection().connectionEstablished()) {
                                DataFileInfo info = mainInstance.getYamlReadDatafile();
                                if (info == null || !mainInstance.hasLocallyStoredEmail()) { //essentially havent made an acc yet
                                    //create an account
                                    info.setEmail(emailText);
                                    info.setTimetableCroppedPngFileName(info.timetableCroppedPngFileName);
                                    mainInstance.writeToDatafile(info);

                                    Encryption encry = new Encryption(enteredPassword, Encryption.generateSalt());
                                    byte[] databaseSalt = encry.getSalt();
                                    byte[] databaseBytes = encry.getOriginalEncrypted();
                                    if (!User.hasSqlEntry(mainInstance, SqlConnection.SqlTableName.TIMETABLE_USERDATA, emailText)) {
                                        mainInstance.getUser().storeEmailAndPasswordWithCrs(emailText, databaseBytes, databaseSalt, crs);
                                    }
                                } else {
                                    String gottenDBSalt = mainInstance.getUser().getDatabaseSalt(emailText);
                                    String gottenDBBytes = mainInstance.getUser().getDatabaseStoredPwBytes(emailText);

                                    if (gottenDBBytes.equals("invalid email")) { //= 'invalid email' when record doesnt exists
                                        displayMessage("This email is incorrect!");
                                        return;
                                    }

                                    boolean authenticated = Encryption.authenticate(enteredPassword, gottenDBBytes, gottenDBSalt);
                                    if (!authenticated) {
                                        displayMessage("This password is incorrect!");
                                        return;
                                    }
                                    if (!crs.equals(mainInstance.getUser().getHomeCrs())) {
                                        mainInstance.getUser().updateHomeCrs(crs);
                                    }
                                }
                                displayMessage("Successfully authenticated!");
                                mainInstance.openPanel(TTrainParser.CORE_PANEL);
                            } else {
                                displayMessage("Your internet is either down, or our server is down!");
                            }
                        } else {
                            displayMessage("Please select a valid home station! This can be changed later.");
                        }
                    } else {
                        displayMessage("Passcode must be SIX NUMBERS eg 123456 or 593412");
                    }
                } else {
                    displayMessage("'" + emailText + "'\nis not a valid email!");
                }
            }
        };
    }


    private boolean isValidPasscode(char[] password) {
        if (password.length != 6) return false;
        for (int i = 0; i < password.length; i++) {
            char charAt = password[i];
            if (!Character.isDigit(charAt)) {
                return false;
            }
        }
        return true;
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
