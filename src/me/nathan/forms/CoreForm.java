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
                ocrText = main.depleteFutileInfo(ocrText, true);

                List<String> words = Arrays.asList(ocrText.split(" "));
                Map<String, String> subjectAndBounds = new LinkedHashMap<String, String>();
                //Business Studies, "1-3"
                //Cmp Sci, 3, 5 //3 words after previous subjects upper bound, end bound is 5

                int endBoundIndex = 0;

                for (int i = 0; i < words.size(); i++) {
                    String currentWord = words.get(i);
                    for (String aSubject : Arrays.asList("Biology", "Computer Science", "Government & Politics")) {
                        if (aSubject.contains(" ")) {
                            String firstWord = aSubject.split(" ")[0];
                            if (firstWord.equals(currentWord)) {
                                List<String> splitted = Arrays.asList(aSubject.split(" ")); //started = true because it contains " ", list = "Business", "studies", "two"

                                int lowerBound = i; //If first iteration, endboundindex is 0 anyways so its i - 0 which is just i

                                endBoundIndex = lowerBound + (splitted.size() - 1);

                                subjectAndBounds.put(aSubject, String.valueOf(lowerBound + ", " + endBoundIndex));
                            }
                        } else if (aSubject.equals(currentWord)) {
                            subjectAndBounds.putIfAbsent(aSubject, String.valueOf(i + ", " + i));
                        }
                    }
                }
                System.out.println(ocrText);
                int previousLowerBound = -1;
                for (String subject : subjectAndBounds.keySet()) {
                    String[] split = subject.split(" ");
                    String[] valueSplit = subjectAndBounds.get(subject).split(", ");
                    int lowerOrDifferenceBound = Integer.parseInt(valueSplit[0]);
                    if (previousLowerBound != -1) {
                        lowerOrDifferenceBound = previousLowerBound + lowerOrDifferenceBound;
                    } else {
                        previousLowerBound = lowerOrDifferenceBound - 1;
                    }


                    int subjectNameLowerBound = lowerOrDifferenceBound;
                    int subjectNameUpperBound = subjectNameLowerBound + (split.length - 1);

                    int lowerBoundForJustTimes = subjectNameUpperBound;
                    int upperBoundForJustTimes = subjectNameUpperBound + 3;


                }
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
