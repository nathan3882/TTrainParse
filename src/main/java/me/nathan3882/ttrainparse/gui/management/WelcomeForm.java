package me.nathan3882.ttrainparse.gui.management;

import me.nathan3882.ttrainparse.MessageDisplay;
import me.nathan3882.ttrainparse.ParsedTimetable;
import me.nathan3882.ttrainparse.TTrainParser;
import me.nathan3882.ttrainparse.User;
import me.nathan3882.ttrainparse.data.DataFileInfo;
import me.nathan3882.ttrainparse.data.SqlConnection;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class WelcomeForm extends MessageDisplay {

    public JPanel welcomePanel;
    public JLabel welcomeLabel;
    private boolean isUpdating;
    private JFileChooser timetableFileChooser;
    private JButton selectFile;
    private JCheckBox confirmValidTimetable;
    private JButton advanceToLoginButton;
    private TTrainParser mainInstance;

    private File selectedFile;

    private boolean isValidFile = false;

    public WelcomeForm(TTrainParser main) {
        this(main, false);
    }

    public WelcomeForm(TTrainParser main, boolean isUpdating) {
        this.mainInstance = main;
        this.setUpdating(isUpdating);
        mainInstance.welcomeForm = this;
        advanceToLoginButton.setEnabled(false);
        confirmValidTimetable.setEnabled(false);

        selectFile.addActionListener(getSelectFileListener());

        confirmValidTimetable.addItemListener(createConfirmValidTimetableListener());

        advanceToLoginButton.addActionListener(createAdvanceToLoginListener(main));
    }

    private static String getDefaultHeaderText() {
        return "<html><center>Welcome!<br>We've sensed you haven't got a timetable stored already...<br>Click the button below to do so!</center></html>";
    }

    private static String getUpdatingHeaderText() {
        return "<html><center>Welcome!<br>We've sensed you've had previously had a timetable...<br>But would like to update it!<br>Click the button below to do so!</center></html>";
    }

    public void setHeaderText(String string) {
        this.welcomeLabel.setText(string);
    }

    /**
     * generates a new cropped PDF file if doesnt already exist
     */
    private ActionListener createAdvanceToLoginListener(TTrainParser main) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                boolean successfullyParsed;
                BufferedImage selectedFileImage;
                long start = 0;
                if (advanceToLoginButton.isEnabled() && isValidFile) {
                    try {
                        selectedFileImage = ImageIO.read(selectedFile); //Get BufferedImage object from previously selected file
                    } catch (Exception e) {
                        TTrainParser.getDebugManager().handle(e);
                        e.printStackTrace();
                        displayMessage("Your selected file has been relocated since you selected it! Please reselect");
                        resetWelcomeButtons();
                        return;
                    }


                    DataFileInfo info = new DataFileInfo();
                    info.setDefaults();

                    if (isUpdatingTimetable() || !main.hasCroppedTimetableFileAlready(false)) { //is updating or hasn't got a pdf
                        start = System.currentTimeMillis();
                        ParsedTimetable timetable = new ParsedTimetable(main, selectedFileImage); //parses jpg
                        successfullyParsed = timetable.successfullyParsed();
                        if (!successfullyParsed) {
                            resetWelcomeButtons();
                            displayMessage("Error occurred while cropping borders.");
                            return;
                        }

                        BufferedImage allDayCroppedImage = timetable.getSuccessfullyParsedImage(); //variable equal to cropped image now
                        main.allDayCroppedImage = allDayCroppedImage;

                        String nesPngPath = selectedFile.getName().split("\\.")[0] + ".png";
                        info.setTimetableCroppedPngFileName(nesPngPath);
                        try {
                            ImageIO.write(allDayCroppedImage, "png", new File(TTrainParser.USER_DIRECTORY_FILE_SEP + nesPngPath)); //overwrites the uncropped jpg/png to cropped png
                        } catch (IOException e) {
                            TTrainParser.getDebugManager().handle(e);
                            e.printStackTrace();
                        }
                        if (isUpdatingTimetable()) {
                            User user = main.getUser();

                            main.getSqlConnection().openConnection();
                            if (!user.hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_RENEWAL)) {
                                user.generateDefaultRenewValues();
                                user.setTableUpdatesLeft(TTrainParser.DEFAULT_FORCE_UPDATE_QUANTITY);
                            }
                            mainInstance.getUser().setTableUpdatesLeft(mainInstance.getUser().getTableUpdatesLeft() - 1);
                            user.setPreviousUploadTime(System.currentTimeMillis());

                            main.openPanel(TTrainParser.CORE_PANEL);
                        } else {
                            main.openPanel(TTrainParser.LOGIN_REGISTER_PANEL);
                        }
                    } else {
                        successfullyParsed = true;
                    }

                    if (!successfullyParsed) { //Terminate
                        displayMessage("Parsing was not successfull! Does the provided image contain timetable borders?");
                        return;
                    } else { //Update data file

                        YamlWriter writer = null;
                        try {
                            writer = new YamlWriter(new FileWriter(TTrainParser.USER_DIRECTORY_FILE_SEP + "data.yml"));
                            writer.write(info); //writes previously collected data about jpg & pdf file names
                            writer.close();
                        } catch (IOException | YamlException e) {
                            TTrainParser.getDebugManager().handle(e);
                            e.printStackTrace();
                        }
                    }
                    displayMessage("Timetable parsed successfully!\nThis took" + (System.currentTimeMillis() - start) + "ms");
                }
            }
        };
    }

    private ItemListener createConfirmValidTimetableListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {

                //open register / login screen
                if (isValidFile) {
                    advanceToLoginButton.setEnabled(confirmValidTimetable.isSelected());

                }
            }
        };
    }

    private ActionListener getSelectFileListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timetableFileChooser = new JFileChooser(TTrainParser.USER_DIRECTORY); //opens current directory it's being run in
                timetableFileChooser.setMultiSelectionEnabled(false); //must select one timetable
                int value = timetableFileChooser.showOpenDialog(welcomePanel); //When clicked, open dialog on the welcomePanel JPanel
                selectedFile = timetableFileChooser.getSelectedFile();
                if (value == JFileChooser.APPROVE_OPTION) {
                    isValidFile = mainInstance.getFileSuffix(selectedFile).equalsIgnoreCase("jpg");
                    if (isValidFile) {
                        confirmValidTimetable.setEnabled(true);
                        return;
                    }
                    resetWelcomeButtons();
                    displayMessage("We've detected that's an invalid file!");
                }
            }
        };
    }

    private void resetWelcomeButtons() {
        advanceToLoginButton.setEnabled(false);
        confirmValidTimetable.setSelected(false);
        confirmValidTimetable.setEnabled(false);
        isValidFile = false;
    }

    @Override
    public JPanel getPanel() {
        return this.welcomePanel;
    }

    @Override
    public String toString() {
        return "welcomePanel";
    }

    public boolean isUpdatingTimetable() {
        return isUpdating;
    }

    public void setUpdating(boolean isUpdating) {
        this.isUpdating = isUpdating;
        if (isUpdating) {
            setHeaderText(getUpdatingHeaderText());
            resetWelcomeButtons();
        } else {
            setHeaderText(getDefaultHeaderText());
        }
    }
}
