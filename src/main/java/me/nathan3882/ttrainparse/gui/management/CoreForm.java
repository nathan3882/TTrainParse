package me.nathan3882.ttrainparse.gui.management;

import me.nathan3882.idealtrains.IdealTrains;
import me.nathan3882.idealtrains.Service;
import me.nathan3882.ttrainparse.*;
import me.nathan3882.ttrainparse.data.SqlConnection;

import javax.swing.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.soap.SOAPException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigInteger;
import java.time.*;
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
    private JComboBox updateHomeCrsComboBox;
    private JLabel updateHomeCrsHelpLabel;
    private String displayString;
    private List<LessonInfo> differentDayInfo;
    private boolean showTrainsForEveryLesson;

    public CoreForm(TTrainParser main) {

        this.mainInstance = main;

        mainInstance.coreForm = this;
        updateHomeCrsHelpLabel.setText("<html><center>^ New home? Update it above ^</center></html>");
        mainInstance.configureCrsComboBox(updateHomeCrsComboBox);
        mainInstance.changeCrsComboBoxToCurrentCrs(updateHomeCrsComboBox);

        updateHomeCrsComboBox.addItemListener(getUpdateHomeCrsComboBoxListener());
        this.user = mainInstance.getUser();
        boolean hasInternet = user.hasInternet();

        int left;
        Date renewDate;

        if (hasInternet && main.getSqlConnection().connectionEstablished()) {
            mainInstance.getSqlConnection().openConnection();
            if (!user.hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_RENEWAL)) {
                user.generateDefaultRenewValues();
            }
            left = getUser().getTableUpdatesLeft();
            renewDate = getUser().getTableRenewDate(false);
            updateTimetableButton.addActionListener(getUpdateTimetableListener(main));
            updateTimetableInfoLabel.setText("<html><center>" + left + " timetable update/s available until..." + TTrainParser.DOUBLE_BREAK + renewDate + "..." + TTrainParser.BREAK + TTrainParser.BREAK + "when it will refresh :)</center></html>");
        } else {
            updateTimetableInfoLabel.setText("<html><center>You either don't have internet or no sql connection has been established.<br>Timetable updates disabled until fixed...</center></html>");
            updateTimetableButton.setEnabled(false);
        }
        int currentDay = TTrainParser.GLOBAL_CALENDAR.get(Calendar.DAY_OF_WEEK);
        DayOfWeek[] showThese = getDaysToShow(currentDay);

        StringBuilder mainString = new StringBuilder("<html><center>Here are all of your lessons + train times :)" + TTrainParser.DOUBLE_BREAK);

        boolean hasOcrTextStored = user.hasOcrTextStored(showThese);
        boolean isUpdating = mainInstance.welcomeForm.isUpdatingTimetable();
        boolean segment = true;
        boolean store = false;
        if (hasInternet) {
            if (hasOcrTextStored) {
                List<LessonInfo> info = user.getLessonInformation(showThese); //From stored ocr text
                mainString = getStringToDisplay(info, false, 2);
                segment = false;
            } else store = true;
        }
        if (TTrainParser.hasTeachersFile()) {
            if (isUpdating || segment) {
                Segmentation segmentation = new Segmentation(main);
                List<LessonInfo> info = getLessonInformation(segmentation, showThese, store);
                mainString = getStringToDisplay(info, false, 2);
            }
        } else
            mainString.append("There has been an issue with your teachers file configuration").append("does the file exist?"); //TODO
        mainString.append("</center></html>");
        setDisplayString(mainString.toString());
        mainInfoLabel.setText(getDisplayString(user.getHomeCrs()));
    }

    public static Date toDate(LocalTime localTime) {
        Instant instant = localTime.atDate(LocalDate.now())
                .atZone(ZoneId.systemDefault()).toInstant();
        return toDate(instant);
    }

    public static Date toDate(Instant instant) {
        BigInteger milis = BigInteger.valueOf(instant.getEpochSecond()).multiply(
                BigInteger.valueOf(1000));
        milis = milis.add(BigInteger.valueOf(instant.getNano()).divide(
                BigInteger.valueOf(1_000_000)));
        return new Date(milis.longValue());
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
        return this.getStringToDisplay(this.differentDayInfo, this.showTrainsForEveryLesson, 2).toString().replace("{crs}", crs);
    }

    private StringBuilder getStringToDisplay(List<LessonInfo> differentDayInfo, boolean showTrainsForEveryLesson, int maxTrainsPerLesson) {
        this.differentDayInfo = differentDayInfo;
        this.showTrainsForEveryLesson = showTrainsForEveryLesson;

        StringBuilder mainString = new StringBuilder();
        mainString.append("<html><center>");

        Date currentDate = new Date(System.currentTimeMillis());

        int totalDaysToShow = differentDayInfo.size();
        for (int i = 0; i < totalDaysToShow; i++) {

            LessonInfo newCollegeDay = differentDayInfo.get(i);

            if (i != 0 || i != totalDaysToShow - 1)
                mainString.append(TTrainParser.BREAK); //when its not the first or last day starting, break to avoid the last lesson for prev day
            mainString.append(upperFirst(newCollegeDay.getDayOfWeek())).append(":"); //Title the data with "{DAY}:\n"

            newCollegeDay.getLessons().forEach(lessonName -> { //This iteration allows 1+ lessons to be accounted for
                List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);

                int startTimesSize = startTimes.size();
                int lastLesson = startTimesSize - 1;

                for (int k = 0; k < startTimesSize; k++) { //This iteration goes through the single/multiple lesson times
                    mainString.append(TTrainParser.BREAK);
                    LocalTime aLessonsStartTime = startTimes.get(k);
                    Date aLessonsStartDate = toDate(aLessonsStartTime);

                    LocalTime finishTime = finishTimes.get(k);

                    String startString = getPrettyMinute(aLessonsStartTime.getMinute());
                    String endString = getPrettyMinute(finishTime.getMinute());

                    mainString.append(lessonName + " lesson number " + (k + 1) + " starts at " + aLessonsStartTime.getHour() + ":" + startString + " and ends at " + finishTime.getHour() + ":" + endString);
                    if (showTrainsForEveryLesson || k == 0 || k == lastLesson) {

                        List<Service> idealTrains = null;
                        try {

                            idealTrains = IdealTrains.getHomeToLessonServices(
                                    user.getHomeCrs(), "BCU", currentDate, 8 * 60, aLessonsStartDate, 120);
                        } catch (SOAPException e) {
                            e.printStackTrace();
                        }
                        if (idealTrains != null) {
                            mainString.append(TTrainParser.BREAK + "From your home station, catch the:" + TTrainParser.BREAK);
                            if (!idealTrains.isEmpty()) {
                                int done = 0;
                                for (int l = (idealTrains.size() - 1); l >= 0; l--) { //this iteration goes through fastest [len - 1] to slowest [0]
                                    if (done == maxTrainsPerLesson) break;
                                    Service service = idealTrains.get(l);
                                    XMLGregorianCalendar xmlEta = service.getEta();
                                    XMLGregorianCalendar xmlEtd = service.getEtd();

                                    XMLGregorianCalendar xmlSta = service.getSta();
                                    XMLGregorianCalendar xmlSdt = service.getSdt();

                                    Calendar departCal = getCalendarFromDate(check(xmlEtd, xmlSdt).getTime());
                                    Calendar arrivalCal = getCalendarFromDate(check(xmlEta, xmlSta).getTime());

                                    if (arrivalCal == null)
                                        continue; //API sometimes doesn't have arrival data & is null from national rail

                                    long departMillis = departCal.getTimeInMillis();
                                    long arriveMillis = arrivalCal.getTimeInMillis();

                                    if (arriveMillis <= departMillis) {
                                        continue; //Small quantity of times say that arrival time is prior to departure time?
                                    }

                                    String arrivalHoursString = fixHours(arrivalCal.get(Calendar.HOUR_OF_DAY));
                                    String departureHoursString = fixHours(departCal.get(Calendar.HOUR_OF_DAY));

                                    String departureMinsString = getPrettyMinute(xmlEtd.getMinute());
                                    String arrivalMinsString = getPrettyMinute(xmlSta.getMinute());

                                    long difFromLesson = service.getToSpareMinutes();
                                    mainString.append(departureHoursString + ":" + departureMinsString
                                            + " from " + getUser().getHomeCrs() +
                                            " that arrives @ " + arrivalHoursString + ":" + arrivalMinsString +
                                            ". (" + difFromLesson + "mins before lesson)" + TTrainParser.BREAK);
                                    done++;
                                }
                            } else {
                                mainString.append("No trains found for this lesson...");
                            }
                        }
                    }
                    //Here is where the end of a lesson's train times iteration breaks,
                    //checks whether it's the last lesson for that day, if so TTrainParser.BREAK
                    if (k == lastLesson) {
                        mainString.append(TTrainParser.BREAK);
                    }
                }

            });
        }
        mainString.append("</center></html>");
        return mainString;
    }

    //Services for example an actual service that departs at 12:50 in the afternoon (not at night) says 00 instead of 12
    private String fixHours(int hour) {
        String str = String.valueOf(hour);
        if (str.startsWith("0")) {
            str = "12" + str.substring(1);
        }
        return str;
    }

    private Calendar getCalendarFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    private GregorianCalendar check(XMLGregorianCalendar estimated, XMLGregorianCalendar scheduled) {
        GregorianCalendar date = null;
        if (estimated != null) {
            date = estimated.toGregorianCalendar();
        } else if (scheduled != null) { //Eta not in xml response, was null, try sta
            date = scheduled.toGregorianCalendar();
        }
        return date;
    }

    private ActionListener getUpdateTimetableListener(TTrainParser main) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                SqlConnection sqlCon = main.getSqlConnection();

                sqlCon.openConnection();

                long currentTime = System.currentTimeMillis();
                long previousUploadTime = getUser().getPreviousUploadTime();
                long currentTimeTakePrevUpload = currentTime - previousUploadTime;
                long updateAfterThisTime = TimeUnit.DAYS.toMillis(User.DEFAULT_RENEW_COOLDOWN_DAYS);

                if (getUser().hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_RENEWAL)) { //just incase
                    int forceableUpdatesLeft = getUser().getTableUpdatesLeft();
                    if (currentTimeTakePrevUpload >= updateAfterThisTime) {//7 day period, reset force timetable update number.
                        getUser().setTableUpdatesLeft(TTrainParser.DEFAULT_FORCE_UPDATE_QUANTITY);
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

            if (ocrText.length() < 30) { //Would assume 30 chars or less means no lessons
                displayMessage("You don't have any lessons on " + upperFirst(day));
                continue; /*No Lessons*/
            }

            ocrText = mainInstance.depleteFutileInfo(ocrText);

            if (store)
                user.storeOcrText(ocrText, day, hasInternet); //store depleted text, for example Tuesday Computer Science 12:00 13:00
            else
                doTask = true;

            texts.put(day, ocrText);

            List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));

            LessonInfo lessonInfo = new LessonInfo(words, day);
            if (!lessonInfo.isParsedSuccessfully()) {
                displayMessage("Sorry, timetable can not be parsed. Did your configure " + TTrainParser.TEACHERS_FILE_NAME + "? Perhaps the screenshot came from another monitor, or was too small");
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