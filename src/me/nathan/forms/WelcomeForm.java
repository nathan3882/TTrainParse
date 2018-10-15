package me.nathan.forms;

import me.nathan.ttrainparse.DataFileInfo;
import me.nathan.ttrainparse.ManipulableFile;
import me.nathan.ttrainparse.ParsedTimetable;
import me.nathan.ttrainparse.TTrainParser;
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


public class WelcomeForm {

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
                    main.displayError("We've detected that's an invalid file!");
                    //When click Submit, use isValidFile to condition whether to advance
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

        long now = System.currentTimeMillis();
        /**
         * This listener below handles advance button when it's clicked.
         * It generates a new cropped PDF file if doesnt already exist
         */
        advanceToLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (advanceToLoginButton.isEnabled() && isValidFile) {
                    try {
                        selectedFileImage = ImageIO.read(selectedFile); //Get BufferedImage object from previously selected file
                    } catch (Exception e) {
                        e.printStackTrace();
                        main.displayError("Your selected file has been relocated since you selected it! Please reselect");
                        resetWelcomeButtons();
                        return;
                    }


                    DataFileInfo info = new DataFileInfo();

                    /**TODO potentially jump straight into getting cropped days from big file?
                     Instead of cropping to get all days then cropping again, will stop image equality reducing*/
                    if (!main.hasCroppedTimetableFileAlready(false)) { //hasn't got a pdf
                        ParsedTimetable timetable = new ParsedTimetable(main, selectedFileImage); //parses jpg
                        successfullyParsed = timetable.successfullyParsed();

                        allDayCroppedImage = timetable.getSuccessfullyParsedImage(); //variable equal to cropped image now

                        info.setTimetableCroppepJpgFileName(selectedFile.getName());
                        try {
                            ImageIO.write(allDayCroppedImage, "jpg", selectedFile); //outputs cropped jpg to uncropped jpg file
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String outputFileName = selectedFile.getName().split("\\.")[0] + ".pdf";
                        info.setTimetableCroppedPdfFileName(outputFileName);
                        new ManipulableFile(main, selectedFile).toPdf(outputFileName, true);
                    } else {
                        successfullyParsed = true;
                    }

                    if (!successfullyParsed) { //Terminate
                        main.displayError("Parsing was not successful! Does the provided image contain timetable borders?");
                        return;
                    } else { //Update data file
                        YamlWriter writer = null;
                        try {
                            //TODO Store System current millis for the time which the user had first timetable parsed
                            writer = new YamlWriter(new FileWriter("data.yml"));
                            writer.write(info); //writes previously collected data about jpg & pdf file names
                            writer.close();
                        } catch (IOException | YamlException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Timetable parsed successfully in " + (System.currentTimeMillis() - now) + "ms");
                }
            }
        });
    }

    private void resetWelcomeButtons() {
        advanceToLoginButton.setEnabled(false);
        confirmValidTimetable.setSelected(false);
        confirmValidTimetable.setEnabled(false);
        timetableFileChooser.setCurrentDirectory(new File(TTrainParser.USER_DIRECTORY)); //TODO Make all instantiations of File with parameter TTrainParser.USER_DIRECTORY equal to a const in TTrainParser
        isValidFile = false;
    }

    public JPanel getWelcomePanel() {
        return welcomePanel;
    }

    public File getSelectedFile() {
        return selectedFile;
    }
}
