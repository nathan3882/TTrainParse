package me.nathan3882.ttrainparse.gui.management;

import me.nathan3882.idealtrains.*;
import me.nathan3882.ttrainparse.*;
import me.nathan3882.ttrainparse.data.SqlConnection;
import me.nathan3882.ttrainparse.data.SqlQuery;
import me.nathan3882.ttrainparse.data.SqlUpdate;
import org.json.JSONObject;

import javax.swing.*;
import javax.xml.soap.SOAPException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class CoreForm extends MessageDisplay {

    private static final int DAY_TRAIN_RELEARN_COUNT = 30;
    private final TTrainParser tTrainParser;
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

    public CoreForm(TTrainParser tTrainParser) {

        this.tTrainParser = tTrainParser;
        gettTrainParser().getSqlConnection().openConnection();
        gettTrainParser().coreForm = this;
        updateHomeCrsHelpLabel.setText("<html><center>^ New home? Update it above ^</center></html>");
        gettTrainParser().configureCrsComboBox(updateHomeCrsComboBox);
        gettTrainParser().changeCrsComboBoxToCurrentCrs(updateHomeCrsComboBox);

        updateHomeCrsComboBox.addItemListener(getUpdateHomeCrsComboBoxListener());
        this.user = gettTrainParser().getUser();
        boolean hasInternet = user.hasInternet();

        int left;
        Date renewDate;

        if (hasInternet && tTrainParser.getSqlConnection().connectionEstablished()) {
            gettTrainParser().getSqlConnection().openConnection();
            if (!user.hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_RENEWAL)) {
                user.generateDefaultRenewValues();
            }
            left = getUser().getTableUpdatesLeft();
            renewDate = getUser().getTableRenewDate();
            updateTimetableButton.addActionListener(getUpdateTimetableListener(tTrainParser));
            updateTimetableInfoLabel.setText("<html><center>" + left + " timetable update/s available until..." + TTrainParser.DOUBLE_BREAK + renewDate + "..." + TTrainParser.BREAK + TTrainParser.BREAK + "when it will refresh :)</center></html>");
        } else {
            updateTimetableInfoLabel.setText("<html><center>You either don't have internet or no sql connection has been established.<br>Timetable updates disabled until fixed...</center></html>");
            updateTimetableButton.setEnabled(false);
        }
        DayOfWeek[] showThese = getDaysToShow(TTrainParser.instance().getCurrentDay());

        StringBuilder mainString = new StringBuilder("<html><center>Here are all of your lessons + train times :)" + TTrainParser.DOUBLE_BREAK);

        boolean hasOcrTextStored = user.hasOcrTextStored(showThese);
        boolean isUpdating = gettTrainParser().welcomeForm.isUpdatingTimetable();
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
                Segmentation segmentation = new Segmentation(tTrainParser);
                List<LessonInfo> info = getLessonInformation(segmentation, showThese, store);
                mainString = getStringToDisplay(info, false, 2);
            }
        } else
            mainString.append("There has been an issue with your teachers file configuration").append("does the file exist?"); //TODO
        mainString.append("</center></html>");
        setDisplayString(mainString.toString());
        mainInfoLabel.setText(getDisplayString(user.getHomeCrs()));
    }

    public static Date toDate(LocalTime localTime, LocalDate date) {
        Instant instant = localTime.atDate(date)
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

    public TTrainParser gettTrainParser() {
        return tTrainParser;
    }

    public User getUser() {
        return user;
    }

    @Override
    public JPanel getPanel() {
        return this.coreFormPanel;
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
        long currentMillis = System.currentTimeMillis();
        StringBuilder mainString = new StringBuilder();
        mainString.append("<html><center>");

        Date currentDate = new Date();

        int totalDaysToShow = differentDayInfo.size();

        Calendar lessonTimeCal = Calendar.getInstance();
        lessonTimeCal.setTime(currentDate);

        for (int i = 0; i < totalDaysToShow; i++) {

            LessonInfo newCollegeDay = differentDayInfo.get(i);

            if (i != 0 || i != totalDaysToShow - 1) {
                mainString.append(TTrainParser.BREAK); //when its not the first or last day starting, break to avoid the last lesson for prev day
            }
            mainString.append(upperFirst(newCollegeDay.getDayOfWeek().name())).append(":"); //Title the data with "{DAY}:\n"

            newCollegeDay.getLessons().forEach(lessonName -> { //This iteration allows 1+ lessons to be accounted for
                List<LocalTime> startTimes = newCollegeDay.getStartTimes(lessonName);
                List<LocalTime> finishTimes = newCollegeDay.getFinishTimes(lessonName);

                int startTimesSize = startTimes.size();
                int lastLesson = startTimesSize - 1;

                for (int k = 0; k < startTimesSize; k++) { //This iteration goes through the single/multiple lesson times
                    mainString.append(TTrainParser.BREAK);
                    LocalTime aLessonsStartTime = startTimes.get(k);

                    DayOfWeek today = DayOfWeek.of(TTrainParser.GLOBAL_CALENDAR.get(Calendar.DAY_OF_WEEK));
                    DayOfWeek dayOfLesson = newCollegeDay.getDayOfWeek();
                    int dif = dayOfLesson.getValue() - today.getValue();

                    updateLessonCalendar(TTrainParser.GLOBAL_CALENDAR, lessonTimeCal, aLessonsStartTime, dif);

                    Date aLessonsStartDate = lessonTimeCal.getTime();

                    LocalTime finishTime = finishTimes.get(k);

                    String prettyStartMinutesString = IdealTrains.getPrettyMinute(aLessonsStartTime.getMinute());
                    String prettyEndMinutesString = IdealTrains.getPrettyMinute(finishTime.getMinute());

                    String startsAtPrettyString = aLessonsStartTime.getHour() + ":" + prettyStartMinutesString;
                    String finishesAtPrettyString = finishTime.getHour() + ":" + prettyEndMinutesString;

                    mainString.append(lessonName + " lesson number " + (k + 1) + " starts at " + startsAtPrettyString + " and ends at " + finishesAtPrettyString + TTrainParser.BREAK);
                    boolean hasTrains = false;
                    if (showTrainsForEveryLesson || k == 0 || k == lastLesson) {
                        LinkedList<LinkedList<JSONObject>> learned = getPotentialTwoBestLearnedTrains(user.getHomeCrs(), aLessonsStartDate);
                        if (learned.isEmpty()) {
                            List<Service> idealTrains = null;
                            try {
                                idealTrains = IdealTrains.getHomeToLessonServices(//
                                        user.getHomeCrs(), "BCU", currentDate, 8 * 60, aLessonsStartDate, 120);
                                Collections.reverse(idealTrains); //Get trains and reverse
                            } catch (SOAPException e) {
                                e.printStackTrace();
                            }
                            hasTrains = idealTrains != null && !idealTrains.isEmpty();
                            if (hasTrains) {
                                mainString.append(TTrainParser.BREAK + "From your home station, catch the:" + TTrainParser.BREAK);

                                String columnToReference = startsAtPrettyString.replace(":", "");

                                String servicesAsJson = appendBestTrains(maxTrainsPerLesson, mainString, idealTrains);

                                try {
                                    performTrainSql(currentMillis, columnToReference, servicesAsJson);
                                } catch (SQLException e) {
                                    displayMessage("An error occurred whilst inserting train data to learn for the future!");
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            hasTrains = performTrainAnalysis(maxTrainsPerLesson, mainString, learned);
                        }
                        if (!hasTrains) {
                            mainString.append("<br>No trains found for this lesson...");
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

    private String appendBestTrains(int maxTrainsPerLesson, StringBuilder mainString, List<Service> idealTrains) {
        String servicesAsJson = "";
        int done = 0;
        for (Service service : idealTrains) { //this iteration goes through fastest [len - 1] to slowest [0]
            if (done == maxTrainsPerLesson) break;
            Departure departure = service.getDeparture();
            Arrival arrival = service.getArrival();
            if (departure.isNullSingular() || arrival.isNullSingular()) {
                //Both estimated and scheduled are null
                continue;
            }
            TrainDate departureDate = departure.singular();
            TrainDate arrivalDate = arrival.singular();

            long difFromLesson = service.getToSpareMinutes();
            String serviceAsJsonString = service.toString(difFromLesson) + " sep ";

            servicesAsJson += serviceAsJsonString;
            appendTrainToDisplayStr(mainString, departureDate.withColon(), arrivalDate.withColon(), difFromLesson);
            done++;
        }
        return servicesAsJson;
    }

    private void updateLessonCalendar(Calendar calendar, Calendar lessonTimeCal, LocalTime aLessonsStartTime, int dif) {
        lessonTimeCal.set(Calendar.HOUR_OF_DAY, aLessonsStartTime.getHour());
        lessonTimeCal.set(Calendar.MINUTE, aLessonsStartTime.getMinute());
        lessonTimeCal.set(Calendar.SECOND, aLessonsStartTime.getSecond());
        lessonTimeCal.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + dif);
    }

    private boolean performTrainAnalysis(int maxTrainsPerLesson, StringBuilder mainString, LinkedList<LinkedList<JSONObject>> learned) {
        Map<String, Integer> frequencies = new HashMap<>(); //String is "{departure time}, {arrival time}", Integer is amount
        for (LinkedList<JSONObject> oneSqlEntrysTwoBestTrains : learned) {
            oneSqlEntrysTwoBestTrains.forEach(aBestTrain -> {
                String freqString = aBestTrain.get("departure") + ", " + aBestTrain.get("arrival") + ", " + aBestTrain.get("walk");
                int currentFreq = frequencies.getOrDefault(freqString, 0);
                frequencies.put(freqString, currentFreq + 1);
            });
        }
        LinkedList<Entry<String, Integer>> sortedFrequencyEntries = new LinkedList<>(frequencies.entrySet());
        if (!sortedFrequencyEntries.isEmpty()) {
            sortedFrequencyEntries.sort(Comparator.comparing(Entry::getValue));
            for (int m = 0; m < sortedFrequencyEntries.size(); m++) {
                Entry<String, Integer> entry = sortedFrequencyEntries.get(m);
                String[] keySplit = entry.getKey().split(", "); //"{departure time}, {arrival time}, {walk}"
                appendTrainToDisplayStr(mainString, keySplit[0], keySplit[1], Long.parseLong(keySplit[2]));
                if (m + 1 == maxTrainsPerLesson) break;
            }
            return true;
        }
        return false;
    }

    /**
     * {crs: "BMH", arrival: "8:52", departure: "8:25",walk: "8"}; sep {crs: "BMH", arrival: "8:39", departure: "8:10",walk: "21"}; sep
     */
    private void performTrainSql(long currentMillis, String columnToReference, String servicesAsJson) throws SQLException {
        SqlQuery query = new SqlQuery(gettTrainParser().getSqlConnection());
        SqlUpdate update = new SqlUpdate(gettTrainParser().getSqlConnection());
        String checkString = "SELECT " + columnToReference + "" +
                " FROM {table} WHERE insertTimestamp = '" + currentMillis + "'";

        query.executeQuery(checkString, SqlConnection.SqlTableName.TRAINS);

        if (!query.getResultSet().next()) { //don't have an entry

            String insertString = "INSERT INTO {table}" + //create entry
                    "(`insertTimestamp`, `homeCrs`, `900`, `1005`, `1130`, `1305`, `1410`, `1515`) " +
                    "VALUES (" + currentMillis + ",\"" + user.getHomeCrs() + "\",NULL,NULL,NULL,NULL,NULL,NULL)";
            update.executeUpdate(insertString, SqlConnection.SqlTableName.TRAINS);
            //update here to update the 900 etc
        }
        String updateString = ("UPDATE {table} SET" +
                " `" + columnToReference + "`='" + servicesAsJson + "'" +
                " WHERE `insertTimestamp`='" + currentMillis + "'");

        update.executeUpdate(updateString, SqlConnection.SqlTableName.TRAINS);

        query.close();
    }

    private void appendTrainToDisplayStr(StringBuilder mainString, String departureString, String arrivalString, long difFromLesson) {
        mainString.append(departureString
                + " from " + getUser().getHomeCrs() +
                " that arrives @ " + arrivalString +
                ". (" + difFromLesson + "mins before lesson)" + TTrainParser.BREAK);
    }

    /**
     * @returns a list of (lists that contain 2 best trains)
     */
    private LinkedList<LinkedList<JSONObject>> getPotentialTwoBestLearnedTrains(String homeCrs, Date aLessonsStartDate) {
        LinkedList<LinkedList<JSONObject>> learned = new LinkedList<>();
        TrainDate trainDate = new TrainDate(aLessonsStartDate);

        String columnName = trainDate.withoutColon();

        long currentMillis = System.currentTimeMillis();

        long aMonthInMillis = TimeUnit.DAYS.toMillis(DAY_TRAIN_RELEARN_COUNT);

        /**
         * Selects the time column for example 900 for 9am lesson which contains json object like this:
         "{ crs: "BMH" departure: "10:05", arrival: "11:05", walk: "8"};" //crs = homeCrs, d = departure time, a = arrival to brock time, walk: different in mins between arrival and lesson start time
         */
        SqlQuery query = new SqlQuery(gettTrainParser().getSqlConnection());
        String queryString = "SELECT `" + columnName + "`" +
                " FROM {table} WHERE `homeCrs`='" + homeCrs + "'" +
                " AND " + currentMillis + " - insertTimestamp <= " + aMonthInMillis;
        query.executeQuery(queryString, SqlConnection.SqlTableName.TRAINS); //Select if a month hasn't passed
        //The most common train for this lesson is XXX - although our app thinks otherwise, perhaps the train's have changed (XXX that arrives at XXX)
        try {
            int count = 1;
            while (query.getResultSet().next() && !query.getResultSet().wasNull()) { //go through responses containing homeCrs, and the train data for 900 1005 etc
                String string;
                try {
                    string = query.getString(count); //java.sql.SQLException: Column Index out of range, 2 > 1.
                } catch (Exception exception) {
                    break;
                }
                if (string == null) continue;
                String[] listOfTrains = string.split(" sep ");
                LinkedList<JSONObject> temp = new LinkedList<>();
                for (String aTrainJson : listOfTrains) {
                    temp.add(new JSONObject(aTrainJson));
                }

                learned.add(temp);
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return learned;
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
                displayMessage("You don't have any lessons on " + upperFirst(day.name()));
                continue; /*No Lessons*/
            }

            ocrText = gettTrainParser().depleteFutileInfo(ocrText);

            if (store)
                user.storeOcrText(ocrText, day, hasInternet); //store depleted text, for example Tuesday Computer Science 12:00 13:00
            else
                doTask = true;

            texts.put(day, ocrText);

            List<String> words = new LinkedList<>(Arrays.asList(ocrText.split(" ")));

            LessonInfo lessonInfo = new LessonInfo(words, day);
            if (!lessonInfo.isParsedSuccessfully()) {
                displayMessage("Sorry, timetable can not be parsed. Did your configure " + TTrainParser.TEACHERS_FILE_NAME + "? Perhaps the screenshot came from another monitor, or was too small");
                initiateTimetableChange();
            }
            info.add(lessonInfo);
            mFile.deleteAllMade();
        }
        if (doTask) {
            this.task = new TaskManager(new Timer()) {
                @Override
                public void run() {
                    if (user.hasInternet() && gettTrainParser().getSqlConnection().connectionEstablished()) {
                        gettTrainParser().getSqlConnection().openConnection();
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

    private void initiateTimetableChange() {
        gettTrainParser().openPanel(TTrainParser.WELCOME_PANEL);
        gettTrainParser().welcomeForm.setUpdating(true);
    }

    private String upperFirst(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
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
}