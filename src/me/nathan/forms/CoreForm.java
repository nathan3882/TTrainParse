package me.nathan.forms;

import me.nathan.ttrainparse.LessonInfo;
import me.nathan.ttrainparse.ManipulableFile;
import me.nathan.ttrainparse.Segmentation;
import me.nathan.ttrainparse.TTrainParser;
import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

import static me.nathan.ttrainparse.TTrainParser.getTesseractInstance;

public class CoreForm {
    private final TTrainParser main;
    private JPanel coreFormPanel;
    private JLabel mainInfoLabel;

    public CoreForm(TTrainParser main) {
        this.main = main;
        int currentDay = new Date().getDay();
        int[] showThese = new int[2];
        /* Allow user configuration */
        if (currentDay == 6 || currentDay == 7) { //Weekend, show monday
            showThese[0] = 1;
        } else if (currentDay == 5) {
            showThese[0] = 5;
            showThese[1] = 1;
        } else {
            showThese[0] = currentDay;
            showThese[1] = currentDay + 1;
        }

        String mainString = "<html><center>Here are all of your lessons + train times :)<br><br>";

        Map<DayOfWeek, String> opticallyReadText = new HashMap<>();
        //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

        if (main.hasCroppedTimetableFileAlready(true)) { //true = check for png
//           valid cropped png, isn't just an ordinary pdf with loads of weird info"
//            Has a valid cropped jpg file already, set ImageIO allDayCroppedImage

            Segmentation segmentation = new Segmentation(main);

            LinkedList<LessonInfo> infos = new LinkedList<>();
            int i = 0;
            for (int dayInt : showThese) {
                DayOfWeek day = DayOfWeek.of(dayInt);

                ManipulableFile mFile = new ManipulableFile(main, segmentation.getDay(day));

                //More defensive remaking pdf each time from segmentation, say if timetable changed wouldnt use previous day pdf
                File pdfFile = mFile.toPdf(day.name() + ".pdf", false); //Convert specific day mFile toPdf

                String ocrText = null;
                try {
                    ocrText = getTesseractInstance().doOCR(pdfFile);
                } catch (TesseractException e) {
                    e.printStackTrace();
                }

                if (ocrText.length() < 30) {
                    continue; /*No Lessons*/
                }

                ocrText = depleteFutileInfo(ocrText, true).replace("Thursday A2 Tutorial in M01 09:00 - 10:05", "Thursday");
//TODO
                List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));
                infos.add(new LessonInfo(words, day));
                i++;
            }

            for (int l = 0; l < infos.size(); l++) {
                LessonInfo newCollegeDay = infos.get(l);
                if (l != 0) mainString += "<br>";
                mainString += upperFirst(newCollegeDay.getDayOfWeek()) + ":<br>";
                LinkedList lessons = newCollegeDay.getLessons();
                for (int k = 0; k < (lessons.size() / 2); k++) {
                    //Cmp sci, cmp sci, b studies, bstudies  each iteration the lesson times for both lessonss will be shown, if I divide by 2, it will only get one of each thus iterating twice for the one iteration, not four times for 2 iterations
                    String lessonName = newCollegeDay.getLessons().get(k);
                    List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                    List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);
                    for (int j = 0; j < startTimes.size(); j++) {
                        LocalTime startTime = startTimes.get(j);
                        LocalTime finishTime = finishTimes.get(j);
                        mainString += lessonName + " lesson number " + (j + 1) + " starts at "
                                + startTime.getHour() + ":" + startTime.getMinute() + " and ends at< " + finishTime.getHour() + " " + finishTime.getMinute() + "<br>";
                    }
                }
            }
        }
        mainString += "</center></html>";
        mainInfoLabel.setText(mainString);
    }

    private String upperFirst(DayOfWeek dayOfWeek) {
        String string = dayOfWeek.name();
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    private String depleteFutileInfo(String ocrResult, boolean oneSpaceBetweenAllInfo) {

        /**Following Code removes teacher names from OCR string**/
        ocrResult = ocrResult/**class names or numbers**/
                .replaceAll("\\(.*\\)", "")
                /**A Level or BTEC?**/
                .replace("A Level", "")
                .replaceAll("A level", ""); //Computer science is lower case l for some reason? Charlie??
        String[] words = ocrResult.split("\\s+");
        List<String> removeStrings = new ArrayList<>();

        for (String[] teachers : TTrainParser.getSubjectNamesWithMultipleTeachers().values()) {
            for (String teacher : teachers) {
                if (teacher == null) continue;
                for (String wordInOcr : words) {
                    for (String teacherFirstOrLastName : teacher.split(" ")) {
                        if (teacherFirstOrLastName.equalsIgnoreCase("UNKNOWN")) continue;
                        if (calculateDistance(wordInOcr, teacherFirstOrLastName) < 3) { //less than two characters changed
                            removeStrings.add(wordInOcr);
                        }
                    }
                }
            }
        }
        ocrResult = ocrResult.replaceAll("\n", " ");
        for (String wordToRemove : removeStrings) {
            ocrResult = ocrResult.replace(wordToRemove, "");
        }

        /**Following code gets rid of "Yr2 in XXX"*/
        String pastThree = "   ";
        int beforeYr = -1;
        int beforeColon; //No need to initialise & waste memory
        for (int i1 = 0; i1 < ocrResult.length(); i1++) {
            char charAt = ocrResult.charAt(i1);
            pastThree = pastThree.substring(1) + charAt;

            if (pastThree.startsWith("Yr"))
                beforeYr = i1 - 2;

            if (charAt == ':' && beforeYr != -1) { //will be -1 when it doesnt contain Yr, disregard & continue search
                beforeColon = i1 - 2;
                ocrResult = ocrResult.substring(0, beforeYr) + ocrResult.substring(beforeColon);
            }
        }
        ocrResult = ocrResult.replace("?", "").replace("/", "");
        if (oneSpaceBetweenAllInfo) ocrResult = ocrResult.replaceAll("\\s{2,}", " ").trim();

        return ocrResult;
    }

    private int calculateDistance(String x, String y) {
        if (x.isEmpty()) {
            return y.length();
        }

        if (y.isEmpty()) {
            return x.length();
        }


        int substitution = calculateDistance(x.substring(1), y.substring(1)) + cost(x.charAt(0), y.charAt(0));
        int insertion = calculateDistance(x, y.substring(1)) + 1;
        int deletion = calculateDistance(x.substring(1), y) + 1;

        return min(substitution, insertion, deletion);
    }

    private int cost(char first, char last) {
        return first == last ? 0 : 1;
    }

    private int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    public JPanel getWelcomePanel() {
        return this.coreFormPanel;
    }
}
