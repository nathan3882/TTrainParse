package me.nathan3882.ttrainparse;

import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreForm extends MessageDisplay {

    private final TTrainParser mainInstance;
    private JPanel coreFormPanel;
    private JLabel mainInfoLabel;

    public CoreForm(TTrainParser main) {
        this.mainInstance = main;
        mainInstance.coreForm = this;
        int currentDay = new Date().getDay();
        int[] showThese = new int[2];
        /* Allow user configuration */
        if (currentDay == 6 || currentDay == 7) { //Weekend, show monday
            showThese[0] = 1;
            showThese[1] = 2;
        } else if (currentDay == 5) { //Friday, show friday and monday
//            showThese[0] = 5;
            showThese[0] = 4;
            showThese[1] = 5;
        } else { //Show today and tomorrow
            showThese[0] = currentDay;
            showThese[1] = currentDay + 1;
        }

        StringBuilder mainString = new StringBuilder("<html><center>Here are all of your lessons + train times :)<br><br>");

        //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

        if (main.hasCroppedTimetableFileAlready(true)) { //true = check for png
//           valid cropped png, isn't just an ordinary pdf with loads of weird info"
//            Has a valid cropped jpg file already, set ImageIO allDayCroppedImage

            Segmentation segmentation = new Segmentation(main);

            List<LessonInfo> info = new LinkedList<>();
            int count = 0;
            for (int dayInt : showThese) {
                DayOfWeek day = DayOfWeek.of(dayInt);

                ManipulableFile mFile = new ManipulableFile(main, segmentation.getDay(day));

                //More defensive remaking pdf each time from segmentation, say if timetable changed wouldnt use previous day pdf
                File pdfFile = mFile.toPdf(day.name() + ".pdf", false); //Convert specific day mFile toPdf

                String ocrText = null;
                try {
                    ocrText = TTrainParser.getTesseractInstance().doOCR(pdfFile);
                } catch (TesseractException e) {
                    e.printStackTrace();
                }

                assert ocrText != null;

                if (ocrText.length() < 30) {
                    displayMessage("You don't have any lessons on " + upperFirst(day));
                    continue; /*No Lessons*/
                }
                ocrText = ocrText.replaceAll("/", "");
                ocrText = depleteFutileInfo(ocrText, true);

                /**TODO - will need to add support for including tutor in the displayment of times
                 * however reference to some more timetables to allow compatibility with all users will be required
                 */

                List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));
                info.add(new LessonInfo(words, day));
                count++;
                mFile.deleteAllMade();
            }

            for (int i = 0; i < info.size(); i++) {
                LessonInfo newCollegeDay = info.get(i);
                if (i != 0) mainString.append("<br>");
                mainString.append(upperFirst(newCollegeDay.getDayOfWeek())).append(":<br>");
                List<String> lessons = newCollegeDay.getLessons();
                for (int j = 0; j < lessons.size(); j++) {
                    //For example day i = 0 and lessons"Computer Science"
                    String lessonName = lessons.get(j);
                    List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                    List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);

                    //System.out.println("New lesson iteration j=" + j);
                    for (int k = 0; k < startTimes.size(); k++) {
                        //System.out.println("Start times size for " + lessonName + " is " + startTimes.size());
                        LocalTime startTime = startTimes.get(k);
                        LocalTime finishTime = finishTimes.get(k);

                        int startMinute = startTime.getMinute();
                        String startString = String.valueOf(startMinute);
                        if (startMinute < 10) startString = "0" + startMinute;

                        int endMinute = finishTime.getMinute();
                        String endString = String.valueOf(endMinute);
                        if (endMinute < 10) endString = "0" + endMinute;


                        mainString.append(lessonName).append(" lesson number ").append(k + 1).append(" starts at ").append(startTime.getHour()).append(":").append(startString).append(" and ends at< ").append(finishTime.getHour()).append(" ").append(endString).append("<br>");
                    }
                }
            }
        }
        mainString.append("</center></html>");
        mainInfoLabel.setText(mainString.toString());
    }

    private String upperFirst(DayOfWeek dayOfWeek) {
        String string = dayOfWeek.name();
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    private String depleteFutileInfo(String ocrResult, boolean oneSpaceBetweenAllInfo) {
        /**Following Code removes teacher names from OCR string**/
        ocrResult = ocrResult/**class names or numbers**/
                .replaceAll("[\\[\\(].*[\\]\\)]", "") //"\\(.*\\)"
                .replaceAll("/", "")
                .replaceAll("\\.", ":") //Has been a time where string has contained this "Computer Science Yr2 in 818 09.00 - 10:05"
                /**A Level or BTEC?**/
                .replace("A Level", "")
                .replaceAll("A level", ""); //Computer science is lower case l for some reason? Charlie??
        if (oneSpaceBetweenAllInfo) ocrResult = ocrResult.replaceAll("\\s{2,}", " ").trim();
        String[] words = ocrResult.split("\\s+"); //one or more spaces
        for (String str : words) {
            System.out.print(str + " ");
        }
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
        String pastSeven = "       "; //7 spaces
        int beforeYr = -1;
        int beforeColon;
        boolean tutorialCondition = false;
        boolean setTutorBound = false;
        for (int j = 0; j < ocrResult.length(); j++) {
            char charAt = ocrResult.charAt(j);
            pastSeven = pastSeven.substring(1) + charAt;
            String pastThree = pastSeven.substring(4);

            if (!setTutorBound) { //false, dont potentially update to false when waiting for the colon
                Matcher m = Pattern.compile("\\s(in)\\s").matcher(pastSeven);
                tutorialCondition = !pastSeven.contains("Yr") && m.find() && pastThree.startsWith("in");
            }
            if (pastThree.startsWith("Yr")) { //will only begin with in if it's tutor
                beforeYr = j - 2;
            } else if (tutorialCondition && !setTutorBound) {
                beforeYr = j - 3;
                setTutorBound = true;
            }

            if (charAt == ':' && beforeYr != -1) { //will be -1 when it doesnt contain Yr, disregard & continue search
                beforeColon = j - 2;
                ocrResult = ocrResult.substring(0, beforeYr) + (tutorialCondition ? " " : "") + ocrResult.substring(beforeColon);
                if (tutorialCondition) {
                    beforeYr = -1;
                    tutorialCondition = false;
                }
            }
        }
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

    @Override
    public JPanel getPanel() {
        return this.coreFormPanel;
    }
}
