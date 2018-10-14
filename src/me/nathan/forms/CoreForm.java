package me.nathan.forms;

import me.nathan.ttrainparse.ManipulableFile;
import me.nathan.ttrainparse.Segmentation;
import me.nathan.ttrainparse.TTrainParser;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.*;

import static me.nathan.ttrainparse.TTrainParser.getTesseractInstance;

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
                File pdfFile = mFile.toPdf(dowOf.name() + ".pdf", true);
                String ocrText = null;
                try {
                    ocrText = getTesseractInstance().doOCR(pdfFile);
                } catch (TesseractException e) {
                    e.printStackTrace();
                }
                if (ocrText.length() < 30) {
                    continue; /*No Lessons*/
                }
                ocrText = main.depleteFutileInfo(ocrText);

                String[] words = ocrText.split(" ");
                LinkedHashMap<String, String> lessons = new LinkedHashMap<>();
                lessons.put("dummydummydummy", "dummydummydummy");
                //Have a map that stores <String, String>   key = Biology, 4   value = Biology, 7
                for (int i = 0; i < words.length; i++) {
                    String potentialSubject = words[i];
                    if (main.getSubjectNamesWithMultipleTeachers().keySet().contains(potentialSubject)) {
                        int latestIndex = lessons.keySet().size() - 1;
                        String key = getKey(lessons, latestIndex);
                        if (key.equals("dummydummydummy")) {
                            //previous key and value been entered, new dummy key and value found
                            String val = getValue(lessons, latestIndex);
                            lessons.remove(key);
                            lessons.put(potentialSubject + ", " + String.valueOf(i), val);
                            continue;
                        }
                        if (getValue(lessons, latestIndex).equals("dummydummydummy")) {
                            //key has been found, but next entry subject hasnt
                            lessons.put(key, potentialSubject + ", " + String.valueOf(i));
                            continue;
                        }
                    }
                }
                //if a value is null, set it to the split.length()
            }
        }
        mainString += "</html>";
        mainInfoLabel.setText(mainString);
        /**
         * Get OCR string as ocrString
         *
         * ocrString.split[" "]
         *    <Subject, Array Position> list of integers ]is the position in split array the subject is
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

    public void setKey(LinkedHashMap<String, String> map, int index, String str) {
        LinkedList<String> keys = new LinkedList<String>(map.keySet());
        keys.set(index, str);


    }

    public String getKey(LinkedHashMap<String, String> map, int index) {
        return String.valueOf(map.keySet().toArray()[index]);
    }

    public String getValue(LinkedHashMap<String, String> map, int index) {
        return String.valueOf(map.values().toArray()[index]);
    }
}
