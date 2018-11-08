package me.nathan3882.gui.management;

import me.nathan3882.data.SqlConnection;
import me.nathan3882.ttrainparse.*;
import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CoreForm extends MessageDisplay {

    private final TTrainParser mainInstance;
    private User user;
    private JPanel coreFormPanel;
    private JLabel mainInfoLabel;
    private JButton updateTimetableButton;
    private JLabel updateTimetableInfoLabel;

    public static final int DEFAULT_FORCE_UPDATE_QUANTITY = 3;

    public CoreForm(TTrainParser main) {
        this.mainInstance = main;
        this.user = mainInstance.getUser();

        mainInstance.getSqlConnection().openConnection();
        int left = getUser().getTableUpdatesLeft();
        Date renewDate = getUser().getTableRenewDate(false);
        updateTimetableInfoLabel.setText("<html><center>" + left + " timetable update/s available until...<br><br>" + renewDate + "...<br><br>when it will refresh :)</center></html>");

        mainInstance.coreForm = this;
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(now);
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        DayOfWeek[] showThese = getDaysToShow(currentDay);

        updateTimetableButton.addActionListener(getUpdateTimetableListener(main));

        StringBuilder mainString = new StringBuilder("<html><center>Here are all of your lessons + train times :)<br><br>");
        //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

        if (user.hasOcrTextStored(showThese)) {
            List<LessonInfo> info = user.getLessonInformation(showThese); //From stored ocr text
            mainString = getStringToDisplay(info);
        } else if (main.hasCroppedTimetableFileAlready(true)) { //didnt have internet, will store if possible

            Segmentation segmentation = new Segmentation(main);

            List<LessonInfo> info = getLessonInformation(segmentation, showThese);

            mainString = getStringToDisplay(info);

            mainInstance.getSqlConnection().closeConnection();
        }
        mainString.append("</center></html>");
        mainInfoLabel.setText(mainString.toString());
    }


    private StringBuilder getStringToDisplay(List<LessonInfo> info) {
        StringBuilder mainString = new StringBuilder();
        mainString.append("<html><center>");
        for (int i = 0; i < info.size(); i++) {

            LessonInfo newCollegeDay = info.get(i);
            if (i != 0) mainString.append("<br>");
            mainString.append(upperFirst(newCollegeDay.getDayOfWeek())).append(":<br>");

            newCollegeDay.getLessons().forEach(lessonName -> {

                List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);
                for (int k = 0; k < startTimes.size(); k++) {

                    LocalTime startTime = startTimes.get(k);
                    LocalTime finishTime = finishTimes.get(k);

                    String startString = getPrettyMinute(startTime.getMinute());
                    String endString = getPrettyMinute(finishTime.getMinute());

                    mainString.append(lessonName).append(" lesson number ").append(k + 1).append(" starts at ").append(startTime.getHour()).append(":").append(startString).append(" and ends at< ").append(finishTime.getHour()).append(" ").append(endString).append("<br>");
                }
            });
        }
        mainString.append("</center></html>");
        return mainString;
    }

    private ActionListener getUpdateTimetableListener(TTrainParser main) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                SqlConnection sqlCon = main.getSqlConnection();

                sqlCon.openConnection();

                long currentTime = System.currentTimeMillis();
                long previousUploadTime = getUser().getPreviousUploadTime();
                long difference = currentTime - previousUploadTime;
                long updateAfterThisTime = TimeUnit.DAYS.toMillis(User.DEFAULT_RENEW_COOLDOWN_DAYS);

                if (getUser().hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_RENEWAL)) { //just incase
                    int forceableUpdatesLeft = getUser().getTableUpdatesLeft();

                    if (difference >= updateAfterThisTime) {//7 day period, reset force timetable update number.
                        getUser().setTableUpdatesLeft(DEFAULT_FORCE_UPDATE_QUANTITY);
                    } else if (forceableUpdatesLeft > 0) {
                        getUser().setTableUpdatesLeft(forceableUpdatesLeft - 1);
                    } else {
                        displayMessage("Sorry, you've run out of timetable updates for this period!");
                        return;
                    }

                    main.welcomeForm.setUpdating(true);
                    main.openPanel(TTrainParser.WELCOME_PANEL);
                } else {
                    displayMessage("Are you on a different IP?!");
                }
                sqlCon.closeConnection();
            }
        };
    }


    private List<LessonInfo> getLessonInformation(Segmentation segmentation, DayOfWeek[] showThese) {
        List<LessonInfo> info = new LinkedList<>();
        for (DayOfWeek day : showThese) {

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

            ocrText = mainInstance.depleteFutileInfo(ocrText, true);

            user.storeOcrText(ocrText, day); //Store before depleted, raw form

            List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));
//            for (String word : words) {
//                System.out.print(word + " ");
//            }
            info.add(new LessonInfo(words, day));
            mFile.deleteAllMade();
        }
        return info;
    }

    private DayOfWeek[] getDaysToShow(int currentDay) {
        DayOfWeek[] showThese = new DayOfWeek[2];
        // TODO Allow user configuration
        if (currentDay == 6 || currentDay == 7) { //Weekend, show monday
            showThese[0] = DayOfWeek.MONDAY;
            showThese[1] = DayOfWeek.TUESDAY;
        } else if (currentDay == 5) { //Friday, show friday and monday
            showThese[0] = DayOfWeek.FRIDAY;
            showThese[1] = DayOfWeek.MONDAY;
        } else { //Show today and tomorrow
            showThese[0] = DayOfWeek.of(currentDay);
            showThese[1] = DayOfWeek.of(currentDay + 1);
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

    @Override
    public JPanel getPanel() {
        return this.coreFormPanel;
    }

    public User getUser() {
        return user;
    }
}
