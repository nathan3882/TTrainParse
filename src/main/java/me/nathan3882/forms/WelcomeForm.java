package me.nathan3882.forms;

import me.nathan3882.ttrainparse.DataFileInfo;
import me.nathan3882.ttrainparse.ParsedTimetable;
import me.nathan3882.ttrainparse.TTrainParser;
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


public class WelcomeForm implements TTrainParser.IMessageDisplay {

    public JPanel welcomePanel;
    public JLabel welcomeLabel;
    public JFileChooser timetableFileChooser;
    private JButton selectFile;
    private JCheckBox confirmValidTimetable;
    private JButton advanceToLoginButton;
    private TTrainParser main;

    private File selectedFile;
    private BufferedImage selectedFileImage;

    public BufferedImage allDayCroppedImage = null;
    boolean successfullyParsed = false;

    private boolean isValidFile = false;

    public WelcomeForm(TTrainParser main) {
        this.main = main;
        advanceToLoginButton.setEnabled(false);
        confirmValidTimetable.setEnabled(false);

        selectFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                timetableFileChooser = new JFileChooser(TTrainParser.USER_DIRECTORY); //opens current directory it's being run in
                timetableFileChooser.setMultiSelectionEnabled(false); //must select one timetable
                int value = timetableFileChooser.showOpenDialog(welcomePanel); //When clicked, open dialog on the welcomePanel JPanel
                selectedFile = timetableFileChooser.getSelectedFile();
                if (value == JFileChooser.APPROVE_OPTION) {
                    isValidFile = main.getFileSuffix(selectedFile).equalsIgnoreCase("jpg");
                    if (isValidFile) {
                        confirmValidTimetable.setEnabled(true);
                        return;
                    }
                    resetWelcomeButtons();
                    displayMessage(getPanel(), "We've detected that's an invalid file!");
                }
            }
        });
        //when checkbox for clarifying entered file is checked, advanceToTrainButton.setEnabled(true)
        confirmValidTimetable.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {

                //open register / login screen
                if (isValidFile) {
                    advanceToLoginButton.setEnabled(confirmValidTimetable.isSelected());

                }
            }
        });
        /**
         * This listener below handles advance button when it's clicked.
         * It generates a new cropped PDF file if doesnt already exist
         */
        advanceToLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                long start = 0;
                if (advanceToLoginButton.isEnabled() && isValidFile) {
                    try {
                        selectedFileImage = ImageIO.read(selectedFile); //Get BufferedImage object from previously selected file
                    } catch (Exception e) {
                        e.printStackTrace();
                        displayMessage(getPanel(), "Your selected file has been relocated since you selected it! Please reselect");
                        resetWelcomeButtons();
                        return;
                    }


                    DataFileInfo info = new DataFileInfo();

                    /**TODO potentially jump straight into getting cropped days from big file?
                     Instead of cropping to get all days then cropping again, will stop image equality reducing*/
                    if (!main.hasCroppedTimetableFileAlready(false)) { //hasn't got a pdf
                        start = System.currentTimeMillis();
                        ParsedTimetable timetable = new ParsedTimetable(main, selectedFileImage); //parses jpg
                        successfullyParsed = timetable.successfullyParsed();
                        if (!successfullyParsed) {
                            resetWelcomeButtons();
                            displayMessage(getPanel(), "We've detected that's an invalid file!");
                            return;
                        }
                        displayMessage(getPanel(), "Timetable has been successfully stored!");
                        allDayCroppedImage = timetable.getSuccessfullyParsedImage(); //variable equal to cropped image now
                        main.allDayCroppedImage = allDayCroppedImage;

                        String nesPngPath = selectedFile.getName().split("\\.")[0] + ".png";
                        info.setTimetableCroppedPngFileName(nesPngPath);
                        try {
                            ImageIO.write(allDayCroppedImage, "png", new File(TTrainParser.USER_DIRECTORY_FILE_SEP + nesPngPath)); //overwrites the uncropped jpg/png to cropped png
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        main.openPanel(main.LOGIN_REGISTER_PANEL);

                    } else {
                        successfullyParsed = true;
                    }

                    if (!successfullyParsed) { //Terminate
                        displayMessage(getPanel(), "Parsing was not successful! Does the provided image contain timetable borders?");
                        return;
                    } else { //Update data file
                        YamlWriter writer = null;
                        try {
                            //TODO Store System current millis for the time which the user had first timetable parsed
                            writer = new YamlWriter(new FileWriter(main.USER_DIRECTORY_FILE_SEP + "data.yml"));
                            writer.write(info); //writes previously collected data about jpg & pdf file names
                            writer.close();
                        } catch (IOException | YamlException e) {
                            e.printStackTrace();
                        }
                    }
                    displayMessage(getPanel(), "Timetable parsed successfully!\nThis took" + (System.currentTimeMillis() - start) + "ms");
                }
            }
        });
    }

    private void resetWelcomeButtons() {
        advanceToLoginButton.setEnabled(false);
        confirmValidTimetable.setSelected(false);
        confirmValidTimetable.setEnabled(false);

        isValidFile = false;
    }

    @Override
    public void displayMessage(JPanel panel, String message) {
        JOptionPane.showMessageDialog(panel, message);
    }

    @Override
    public JPanel getPanel() {
        return this.welcomePanel;
    }
}
