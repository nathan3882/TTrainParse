package me.nathan3882.ttrainparse;

import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.awt.image.BufferedImage;
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
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(now);
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int[] showThese = getDaysToShow(currentDay);

        StringBuilder mainString = new StringBuilder("<html><center>Here are all of your lessons + train times :)<br><br>");
        //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

        if (main.hasCroppedTimetableFileAlready(true)) { //true = check for png, has a valid cropped jpg file already

            Segmentation segmentation = new Segmentation(main);

            List<LessonInfo> info = getLessonInformation(segmentation, showThese);


            for (int i = 0; i < info.size(); i++) {
                LessonInfo newCollegeDay = info.get(i);
                if (i != 0) mainString.append("<br>");
                mainString.append(upperFirst(newCollegeDay.getDayOfWeek())).append(":<br>");
                List<String> lessons = newCollegeDay.getLessons();
                for (int j = 0; j < lessons.size(); j++) {
                    String lessonName = lessons.get(j);
                    List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                    List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);
                    for (int k = 0; k < startTimes.size(); k++) {

                        LocalTime startTime = startTimes.get(k);
                        LocalTime finishTime = finishTimes.get(k);

                        int startMinute = startTime.getMinute();
                        String startString = String.valueOf(startMinute);

                        int endMinute = finishTime.getMinute();
                        String endString = getPrettyMinute(endMinute);

                        mainString.append(lessonName).append(" lesson number ").append(k + 1).append(" starts at ").append(startTime.getHour()).append(":").append(startString).append(" and ends at< ").append(finishTime.getHour()).append(" ").append(endString).append("<br>");
                    }
                }
            }
        }
        mainString.append("</center></html>");
        mainInfoLabel.setText(mainString.toString());
    }

    private List<LessonInfo> getLessonInformation(Segmentation segmentation, int[] showThese) {
        List<LessonInfo> info = new LinkedList<>();
        int count = 0;
        for (int dayInt : showThese) {
            DayOfWeek day = DayOfWeek.of(dayInt);

            ManipulableObject<BufferedImage> mFile = new ManipulableObject<>(BufferedImage.class);

            mFile.setInitialUpload(segmentation.getDay(day));

            File pdfFile = mFile.toPdf(day.name() + ".pdf", false); //Convert specific day mFile toPdf, defensively making pdf each time, if timetable changed wouldnt use previous day pdf

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
            ocrText = depleteFutileInfo(ocrText, true);

            List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));
            info.add(new LessonInfo(words, day));
            mFile.deleteAllMade();
        }
        return info;
    }


    private int[] getDaysToShow(int currentDay) {
        int[] showThese = new int[2];
        // TODO Allow user configuration
        System.out.println(currentDay);
        if (currentDay == 6 || currentDay == 7) { //Weekend, show monday
            showThese[0] = 1;
            showThese[1] = 2;
        } else if (currentDay == 5) { //Friday, show friday and monday
            showThese[0] = 5;
            showThese[1] = 1;
        } else { //Show today and tomorrow
            showThese[0] = currentDay;
            showThese[1] = currentDay + 1;
        }
        return showThese;
    }

    private String getPrettyMinute(int minute) {
        String prettyMinute = String.valueOf(minute);
        if (minute < 10) prettyMinute = "0" + prettyMinute;
        return prettyMinute;
    }

    private String upperFirst(DayOfWeek dayOfWeek) {
        String string = dayOfWeek.name();
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    /**
     * One of the most fundamental functions to the program. The function, in order, removes...
     * - class unique identifier names, lesson type ie A level and student registration symbols for example / and ?
     * - duplicate whitespace and new line characters
     * - teacher names
     * - room names with support for tutorial sessions for example "Yr2 in SO1" or A2 Tutorial "in MO1"
     */
    private String depleteFutileInfo(String ocrResult, boolean oneSpaceBetweenAllInfo) {
        ocrResult = ocrResult/**class names or numbers**/
                .replaceAll("[\\[\\(].*[\\]\\)]", "") //"\\(.*\\)"
                .replaceAll("/", "")
                .replaceAll("\\?", "") //If you don't turn up to lesson, '?' appears
                .replaceAll("\\.", ":") //Has been a time where string has contained this "09.00 - 10:05"
                .replaceAll("A [Ll]evel", ""); //Some subjects viz Computer Science have lower case l for some reason?

        String[] words = ocrResult.split("\\s+"); //one or more spaces

        for (String word : words) {
            System.out.print(word + " ");
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
        for (String wordToRemove : removeStrings) {
            ocrResult = ocrResult.replace(wordToRemove, "");
        }
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
        if (oneSpaceBetweenAllInfo) ocrResult = ocrResult.replaceAll("\n", "").replaceAll("\\s{2,}", " ").trim();
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
