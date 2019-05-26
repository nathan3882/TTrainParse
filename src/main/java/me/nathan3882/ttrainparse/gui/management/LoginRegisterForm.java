package me.nathan3882.ttrainparse.gui.management;

import me.nathan3882.ttrainparse.IMessageDisplay;
import me.nathan3882.ttrainparse.TTrainParser;
import me.nathan3882.ttrainparse.User;
import me.nathan3882.ttrainparse.data.DataFileInfo;
import me.nathan3882.ttrainparse.data.Encryption;
import me.nathan3882.ttrainparse.data.SqlConnection;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlWriter;

import javax.swing.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginRegisterForm implements IMessageDisplay {
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
    private TTrainParser tTrainParser;

    public LoginRegisterForm(TTrainParser tTrainParser) {
        this.tTrainParser = tTrainParser;
        tTrainParser.loginRegisterForm = this;
        tTrainParser.configureCrsComboBox(selectHomeCrsBox);
        User user = tTrainParser.getUser();
        if (user.hasEmailPwAndHomeData()) {
            homeCrsHelpLabel.setText("<html><center>New house? Add click below if you want to change:</html></center>");
            tTrainParser.changeCrsComboBoxToCurrentCrs(selectHomeCrsBox);
        } else {
            homeCrsHelpLabel.setText("<html><center>Add your home station below:</html></center>");
        }
        if (tTrainParser.hasLocallyStoredEmail()) {
            aboveEverythingLabel.setText("<html><center>Welcome<br>You've probably already stored your timetable<br>Please login/register below using your previously created account</center></html>");
        }
        emailHelpLabel.setText("<html><center>Your email goes below</center></html>");
        passwordHelpLabel.setText("<html><center>Your six digit password goes below<br>Note: These 6 digits will allow you to use the phone app too!</center></html>");
        advanceToTrainsButton.setEnabled(true);

        emailTextField.addMouseListener(getMouseListener());
        advanceToTrainsButton.addActionListener(getAdvanceToTrainsBtnListener());

    }

    public void writeToDatafile(DataFileInfo info) {
        YamlWriter writer;
        try { //TODO Store System current millis for the time which the user had first timetable parsed
            writer = new YamlWriter(new FileWriter(TTrainParser.USER_DIRECTORY_FILE_SEP + "data.yml"));
            writer.write(info); //writes previously collected data about jpg & pdf file names
            writer.close();
        } catch (IOException | YamlException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public JPanel getPanel() {
        return this.loginRegisterPanel;
    }

    private ActionListener getAdvanceToTrainsBtnListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advance();
            }
        };
    }

    private void advance() {
        String emailText = emailTextField.getText();
        char[] pw = sixDigitPwField.getPassword();
        if (isValidEmailAddress(emailText)) {
            if (isValidPasscode(pw)) {
                String enteredPassword = new String(pw);
                if (selectHomeCrsBox.getSelectedIndex() != -1) {
                    String selected = (String) selectHomeCrsBox.getSelectedItem();
                    String crs = selected.split(" / ")[0];
                    tTrainParser.createDataFileIfNotPresent();
                    DataFileInfo info = tTrainParser.getYamlReadDatafile();
                    tTrainParser.getUser().setEmail(emailText);
                    tTrainParser.getSqlConnection().openConnection();
                    if (tTrainParser.hasInternet() && tTrainParser.getSqlConnection().connectionEstablished()) {
                        boolean noEmailStored = !User.hasSqlEntry(tTrainParser, SqlConnection.SqlTableName.TIMETABLE_USERDATA, emailText);
                        if (noEmailStored) { //No account stored for entered email
                            if (info == null || !tTrainParser.hasLocallyStoredEmail()) {//create an account
                                info.setTimetableCroppedPngFileName(info.timetableCroppedPngFileName);
                                doEmails(emailText, info);
                                doDatabase(emailText, enteredPassword, crs);
                                sendRegisteredMessage();
                            } else {
                                //Have got local email stored, but no database email stored
                                String localEmail = info.getEmail();
                                if (emailText.equals(localEmail)) { //entered same as locally stored, but not in db yet
                                    doDatabase(emailText, enteredPassword, crs);
                                    sendRegisteredMessage();
                                } else { //different than locally stored, change local email - still not in db, will be next login
                                    doEmails(emailText, info);
                                    tTrainParser.changeCrsComboBoxToCurrentCrs(selectHomeCrsBox);
                                    displayMessage("Local Account changed - please re login!");
                                }
                            }
                        } else { //Same email as entered stored in database, logging in to existing
                            String gottenDBSalt = tTrainParser.getUser().getDatabaseSalt(emailText);
                            String gottenDBBytes = tTrainParser.getUser().getDatabaseStoredPwBytes(emailText);

                            if (gottenDBBytes.equals("invalid email")) { //= 'invalid email' when record doesnt exists
                                displayMessage("This email is incorrect!");
                                return;
                            }

                            boolean authenticated = Encryption.authenticate(enteredPassword, gottenDBBytes, gottenDBSalt);
                            if (!authenticated) {
                                displayMessage("This password is incorrect!");
                                return;
                            } else {
                                String localEmail = info.getEmail();
                                if (emailText.equals(localEmail)) { //entered same as locally stored, but not in db yet
                                    if (!crs.equals(tTrainParser.getUser().getHomeCrs())) {
                                        tTrainParser.getUser().updateHomeCrs(crs);
                                    }
                                    displayMessage("Successfully logged in to existing acc!");
                                    tTrainParser.openPanel(TTrainParser.CORE_PANEL);
                                } else { //different than locally stored, change local email - still not in db, will be next login
                                    doEmails(emailText, info);
                                    tTrainParser.changeCrsComboBoxToCurrentCrs(selectHomeCrsBox);
                                    displayMessage("Local Account changed - please re login!");
                                }
                            }
                        }
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

    private void sendRegisteredMessage() {
        displayMessage("Registered to database! Remember these details!");
    }

    private void doEmails(String emailText, DataFileInfo info) {
        info.setEmail(emailText);
        tTrainParser.getUser().setEmail(emailText);
        writeToDatafile(info);
    }

    private void doDatabase(String emailText, String enteredPassword, String crs) {
        Encryption encry = new Encryption(enteredPassword, Encryption.generateSalt());
        byte[] databaseSalt = encry.getSalt();
        byte[] databaseBytes = encry.getOriginalEncrypted();
        tTrainParser.getUser().storeEmailAndPasswordWithCrs(emailText, databaseBytes, databaseSalt, crs);
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
                if (emailTextField.getText().contains("Enter email")) {
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
}
