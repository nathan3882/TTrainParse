package me.nathan.brockapptesting;

import net.sourceforge.tess4j.TesseractException;
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
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;


public class WelcomeForm {

    public JPanel welcomePanel;
    public JLabel welcomeLabel;
    public JFileChooser timetableFileChooser;
    private JButton selectFile;
    private JCheckBox confirmValidTimetable;
    private JButton advanceToTrainButton;
    private TTrainParser main;

    private File selectedFile;
    private BufferedImage selectedFileImage;

    public BufferedImage allDayCroppedImage = null;
    boolean successfullyParsed = false;

    private boolean isValidFile = false;

    public WelcomeForm(TTrainParser main) {
        this.main = main;
        advanceToTrainButton.setEnabled(false);
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
                    advanceToTrainButton.setEnabled(confirmValidTimetable.isSelected());

                }
            }
        });

        long now = System.currentTimeMillis();
        /**
         * This listener below handles advance button when it's clicked.
         * It generates a new cropped PDF file if doesnt already exist
         */
        advanceToTrainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (advanceToTrainButton.isEnabled() && isValidFile) {
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
                        main.jpgToPdf(selectedFile, outputFileName, false); //making a pdf of the cropped image
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

                    Map<DayOfWeek, String> opticallyReadText = new HashMap<>();
                    //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

                    if (main.hasCroppedTimetableFileAlready(true)) { //true = check for jpg
                        //Has a valid cropped JPG file already, set ImageIO allDayCroppedImage
                        try {
                            allDayCroppedImage = ImageIO.read(main.getCroppedTimetableFileName(true));
                        } catch (IOException e) {
                            e.printStackTrace();
                            main.displayError("Could not load image file, file name in data.yml is invalid!");
                            return;
                        }
                        Segmentation segmentation = new Segmentation(main, allDayCroppedImage);

                        String ocrText = "";
                        for (int i = 1; i <= 5; i++) {
                            DayOfWeek day = DayOfWeek.of(i);

                            /**Make a temp JPG version of the specific day, ie MONDAY**/
                            File file = new File(day.name() + ".jpg");
                            try {
                                ImageIO.write(segmentation.getDay(day), "jpg", file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                main.jpgToPdf(file, day.name() + ".pdf", true);

                                ocrText = main.getTesseractInstance().doOCR(new File(day.name() + ".pdf"));
                            } catch (TesseractException e) {
                                main.displayError("An error occured whilst doing OCR on PDF");
                                e.printStackTrace();
                            }
                            if (ocrText.length() < 30) {
                                continue; /*No Lessons*/
                            }

                            opticallyReadText.put(day, main.depleteFutileInfo(ocrText));

                        }
                    }
                    for (DayOfWeek day : opticallyReadText.keySet()) {
                        if (day == DayOfWeek.THURSDAY) {
                            System.out.println(opticallyReadText.get(day));
                        }
                    }
                    System.out.println("Timetable parsed successfully in " + (System.currentTimeMillis() - now) + "ms");
                }
            }
        });
    }

    private void resetWelcomeButtons() {
        advanceToTrainButton.setEnabled(false);
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
