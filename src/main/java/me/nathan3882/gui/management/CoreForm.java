package me.nathan3882.gui.management;

import me.nathan3882.data.SqlConnection;
import me.nathan3882.ttrainparse.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CoreForm extends MessageDisplay {

    private final TTrainParser mainInstance;
    private TaskManager task;
    private User user;
    private JPanel coreFormPanel;
    private JLabel mainInfoLabel;
    private JButton updateTimetableButton;
    private JLabel updateTimetableInfoLabel;

    public static final int DEFAULT_FORCE_UPDATE_QUANTITY = 3;

    public CoreForm(TTrainParser main) {

        this.mainInstance = main;

        mainInstance.coreForm = this;
        this.user = mainInstance.getUser();
        int left = -1;
        Date renewDate = null;
        boolean hasInternet = user.hasInternet();
        if (hasInternet && main.getSqlConnection().connectionEstablished()) {
            mainInstance.getSqlConnection().openConnection();
            if (!user.hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_RENEWAL)) {
                user.generateDefaultRenewValues();
            }
            left = getUser().getTableUpdatesLeft();
            renewDate = getUser().getTableRenewDate(false);
            updateTimetableButton.addActionListener(getUpdateTimetableListener(main));
            updateTimetableInfoLabel.setText("<html><center>" + left + " timetable update/s available until...<br><br>" + renewDate + "...<br><br>when it will refresh :)</center></html>");
        } else {
            updateTimetableInfoLabel.setText("<html><center>You either don't have internet or no sql connection has been established.<br>Timetable updates disabled until fixed...</center></html>");
            updateTimetableButton.setEnabled(false);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        DayOfWeek[] showThese = getDaysToShow(currentDay);

        StringBuilder mainString = new StringBuilder("<html><center>Here are all of your lessons + train times :)<br><br>");
        //TODO in writeup mention it was between storing all days once, or doing a new segmentation object each time and just extracting one day

        boolean hasOcrTextStored = user.hasOcrTextStored(showThese);
        boolean isUpdating = mainInstance.welcomeForm.isUpdating();
        boolean segment = true;
        boolean store = false;
        if (hasInternet) {
            if (hasOcrTextStored) {
                List<LessonInfo> info = user.getLessonInformation(showThese); //From stored ocr text
                mainString = getStringToDisplay(info);
                segment = false;
            } else store = true;
        }

        if (isUpdating || segment) {
            Segmentation segmentation = new Segmentation(main);
            List<LessonInfo> info = getLessonInformation(segmentation, showThese, store);
            mainString = getStringToDisplay(info);
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
                        //removal when has successfully been parsed, not here, when the button is clicked
                    } else {
                        displayMessage("Sorry, you've run out of timetable updates for this period!");
                        return;
                    }

                    main.welcomeForm.setUpdating(true);
                    main.openPanel(TTrainParser.WELCOME_PANEL);
                } else {
                    displayMessage("Are you on a different IP?!");
                }
            }
        };
    }


    private List<LessonInfo> getLessonInformation(Segmentation segmentation, DayOfWeek[] showThese, boolean store) {
        List<LessonInfo> info = new LinkedList<>();
        Map<DayOfWeek, String> texts = new HashMap<>();
        boolean hasInternet = user.hasInternet();
        boolean doTask = false;
        for (DayOfWeek day : showThese) {

            ManipulableObject<BufferedImage> mFile = new ManipulableObject<>(BufferedImage.class);

            BufferedImage oneSeg = segmentation.getDay(day);
            mFile.setInitialUpload(oneSeg);

            File pdfFile = mFile.toPdf(day.name() + ".pdf", false); //Convert specific day mFile toPdf, defensively making pdf each time, if timetable changed wouldnt use previous day pdf

            String ocrText = null;
            try {
                ocrText = TTrainParser.getTesseractInstance().doOCR(pdfFile);
            } catch (Exception e) {
                TTrainParser.getDebugManager().handle(e);
                e.printStackTrace();
            }

            assert ocrText != null;

            if (ocrText.length() < 30) {
                displayMessage("You don't have any lessons on " + upperFirst(day));
                continue; /*No Lessons*/
            }

            ocrText = mainInstance.depleteFutileInfo(ocrText, true);

            if (store)
                user.storeOcrText(ocrText, day, hasInternet); //Store before depleted, raw form
            else
                doTask = true;

            texts.put(day, ocrText);

            List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));

            info.add(new LessonInfo(words, day));
            mFile.deleteAllMade();
        }
        if (doTask) {
            this.task = new TaskManager(new Timer()) {
                @Override
                public void run() {
                    if (user.hasInternet() && mainInstance.getSqlConnection().connectionEstablished()) {
                        mainInstance.getSqlConnection().openConnection();
                        for (DayOfWeek day : showThese) {
                            if (!user.hasOcrTextStored(showThese)) {
                                user.storeOcrText(texts.get(day), day, true);
                            }
                        }
                        terminate();
                    }
                }
            };
            task.runTaskSynchronously(task, 5000, 5000);
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