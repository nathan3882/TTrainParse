package me.nathan3882.gui.management;

import me.nathan3882.data.SqlConnection;
import me.nathan3882.idealtrains.IdealTrains;
import me.nathan3882.idealtrains.Service;
import me.nathan3882.ttrainparse.*;

import javax.swing.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.soap.SOAPException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.*;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CoreForm extends MessageDisplay {

    public static final int DEFAULT_FORCE_UPDATE_QUANTITY = 3;
    private final TTrainParser mainInstance;
    private TaskManager task;
    private User user;
    private JPanel coreFormPanel;
    private JLabel mainInfoLabel;
    private JButton updateTimetableButton;
    private JLabel updateTimetableInfoLabel;
    private JComboBox updateHomeCrsComboBox;
    private JLabel updateHomeCrsHelpLabel;

    private String displayString;
    private List<LessonInfo> info;
    private boolean showTrainsForEveryLesson;

    public CoreForm(TTrainParser main) {

        this.mainInstance = main;

        mainInstance.coreForm = this;
        updateHomeCrsHelpLabel.setText("<html><center>^ New home? Update it above ^</center></html>");
        mainInstance.configureCrsComboBox(updateHomeCrsComboBox);
        mainInstance.changeCrsComboBoxToCurrentCrs(updateHomeCrsComboBox);

        updateHomeCrsComboBox.addItemListener(getUpdateHomeCrsComboBoxListener());
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
                mainString = getStringToDisplay(info, true);
                segment = false;
            } else store = true;
        }
        if (mainInstance.hasTeachersFile()) {
            if (isUpdating || segment) {
                Segmentation segmentation = new Segmentation(main);
                List<LessonInfo> info = getLessonInformation(segmentation, showThese, store);
                mainString = getStringToDisplay(info, true);
            }
        } else
            mainString.append("There has been an issue with your teachers file configuration").append("does the file exist?"); //TODO
        mainString.append("</center></html>");
        setDisplayString(mainString.toString());
        mainInfoLabel.setText(getDisplayString(user.getHomeCrs()));
    }

    private ItemListener getUpdateHomeCrsComboBoxListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selected = (String) updateHomeCrsComboBox.getSelectedItem();
                    String newCrs = selected.split(" / ")[0];
                    user.updateHomeCrs(newCrs);
                    mainInfoLabel.setText(getDisplayString(newCrs));
                }
            }
        };
    }

    private void setDisplayString(String string) {
        this.displayString = string;
    }

    private String getDisplayString(String crs) {
        return this.getStringToDisplay(this.info, this.showTrainsForEveryLesson).toString().replace("{crs}", crs);
    }


    private StringBuilder getStringToDisplay(List<LessonInfo> info, boolean showTrainsForEveryLesson) {
        this.info = info;
        this.showTrainsForEveryLesson = showTrainsForEveryLesson;
        StringBuilder mainString = new StringBuilder();
        mainString.append("<html><center>");
        Calendar cal = Calendar.getInstance();
        Date currentDate = new Date(System.currentTimeMillis());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < info.size(); i++) {

            LessonInfo newCollegeDay = info.get(i);
            if (i != 0) mainString.append("<br>");
            mainString.append(upperFirst(newCollegeDay.getDayOfWeek())).append(":<br>");

            newCollegeDay.getLessons().forEach(lessonName -> {

                List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);

                int startSize = startTimes.size();
                int lastLesson = startSize - 1;

                for (int k = 0; k < startSize; k++) {
                    mainString.append("<br>");
                    LocalTime startTime = startTimes.get(k);
                    Instant instant = startTime.atDate(LocalDate.of(year, month, day)).
                            atZone(ZoneId.systemDefault()).toInstant();
                    Date lessonTime = Date.from(instant);

                    LocalTime finishTime = finishTimes.get(k);

                    String startString = getPrettyMinute(startTime.getMinute());
                    String endString = getPrettyMinute(finishTime.getMinute());

                    mainString.append(lessonName + " lesson number " + (k + 1) + " starts at " + startTime.getHour() + ":" + startString + " and ends at " + finishTime.getHour() + ":" + endString);
                    if (showTrainsForEveryLesson || k == 0 || k == lastLesson) {

                        List<Service> idealTrains = null;
                        try {
                            idealTrains = IdealTrains.getHomeToLessonServices(
                                    user.getHomeCrs(), "BCU", currentDate, 8, 60, lessonTime);
                        } catch (SOAPException e) {
                            e.printStackTrace();
                        }
                        if (idealTrains != null) {
                            mainString.append("<br>From your home station, catch the:<br>");
                            for (int l = 0; l < idealTrains.size(); l++) {
                                Service service = idealTrains.get(l);
                                XMLGregorianCalendar eta = service.getEta();
                                Date etaDate = eta.toGregorianCalendar().getTime();
                                long etaMillis = etaDate.getTime();
                                long toSpareMinutes = TimeUnit.MILLISECONDS.toMinutes(lessonTime.getTime() - etaMillis);
                                mainString.append(service.getEtd() + " that arrives @ " + eta + ". (" + toSpareMinutes + "mins before lesson)");
                                if (l == 1) break; //terminate after best 2 have been shown
                            }
                        }
                    }
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

            LessonInfo lessonInfo = new LessonInfo(words, day);
            if (!lessonInfo.isParsedSuccessfully()) {
                displayMessage("Sorry, timetable can not be parsed. Did your configure Teacher Names.txt? Perhaps the screenshot came from another monitor, or was too small");
                mainInstance.updateTimetableUpload();
            }
            info.add(lessonInfo);
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