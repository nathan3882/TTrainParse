package me.nathan.forms;

import me.nathan.ttrainparse.ManipulableFile;
import me.nathan.ttrainparse.Segmentation;
import me.nathan.ttrainparse.TTrainParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CoreForm {
    private final TTrainParser main;
    private JPanel coreFormPanel;
    private JLabel currentDayLabel;
    private JLabel mainInfoLabel;

    public CoreForm(TTrainParser main) {
        this.main = main;
        int currentDay = new Date().getDay();
        int[] showThese = new int[2];
        if (currentDay == 6 || currentDay == 7) { //Weekend, show monday
            showThese[0] = 1;
        } else if (currentDay == 5) {
            showThese[0] = 5;
            showThese[1] = 1;
        } else {
            showThese[0] = currentDay;
            showThese[1] = currentDay + 1;
        }

        String string = DayOfWeek.of(currentDay).name();
        currentDayLabel.setText(currentDayLabel.getText().replace("{DAY}", string.substring(0, 1).toUpperCase() + string.substring(1)));
        String mainString = "<html>";

        Map<DayOfWeek, String> opticallyReadText = new HashMap<>();
        //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

        BufferedImage allDayImage;

        if (main.hasCroppedTimetableFileAlready(true)) { //true = check for jpg
            //Has a valid cropped jpg file already, set ImageIO allDayCroppedImage
            try {
                allDayImage = ImageIO.read(main.getCroppedTimetableFileName(true));
            } catch (IOException e) {
                e.printStackTrace();
                main.displayError("Could not load image file, file name in data.yml is invalid!");
                return;
            }
            Segmentation segmentation = new Segmentation(main, allDayImage);

            for (int aDayToShow : showThese) {
                DayOfWeek dowOf = DayOfWeek.of(aDayToShow);

                ManipulableFile mFile = new ManipulableFile(main, segmentation.getDay(dowOf));
                File pdfFile = mFile.toPdf(dowOf.name() + ".pdf");

            }
        }
        mainString += "</html>";
        mainInfoLabel.setText(mainString);
        /**
         * Get OCR string as ocrString
         *
         * ocrString.split[" "]
         *    <Subject, Array Position> list of integers is the position in split array the subject is
         * Map<String, List<Integer>
         * Iterate through splitting, if come across subject name add it to map
         * times for the X lesson are between X lesson and the next lowest number that's been stored referencing the split array
         *
         *
         *
         * Friday   Biology 09:00 - 10:05     Biology 10:05 - 11:10   ;   Business studies 11:30 - 12:35
         *
         *
         */
    }
}
