package me.nathan3882.ttrainparse;

import me.nathan3882.ttrainparse.data.DataFileInfo;
import me.nathan3882.ttrainparse.data.SqlConnection;
import me.nathan3882.ttrainparse.gui.management.CoreForm;
import me.nathan3882.ttrainparse.gui.management.LoginRegisterForm;
import me.nathan3882.ttrainparse.gui.management.WelcomeForm;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TTrainParser extends MessageDisplay {

    public static final Calendar GLOBAL_CALENDAR = Calendar.getInstance();
    public static final String USER_DIRECTORY = System.getProperty("user.dir");
    public static final String WELCOME_PANEL = "welcomePanel";
    public static final String CORE_PANEL = "corePanel";
    public static final String LOGIN_REGISTER_PANEL = "loginRegisterPanel";
    public static final String USER_DIRECTORY_FILE_SEP = USER_DIRECTORY + File.separator;
    public static final String TEACHERS_FILE_NAME = "Teacher Names.txt";
    public static final int DEFAULT_FORCE_UPDATE_QUANTITY = 3;
    public static final String BREAK = "<br>";
    public static final String DOUBLE_BREAK = BREAK + BREAK;
    private static TTrainParser tTrainParser = new TTrainParser();
    private static String activePanel;
    private static ITesseract tesseractInstance = new Tesseract();
    private static DebugManager debugManager;
    public BufferedImage allDayCroppedImage;
    public WelcomeForm welcomeForm;
    public LoginRegisterForm loginRegisterForm;
    public CoreForm coreForm;

    private JPanel cards = null;

    private SqlConnection sqlConnection;
    private User user;
    private boolean hasInternet = false;

    public static void main(String[] args) {

        GLOBAL_CALENDAR.setTime(new Date());

        TTrainParser.debugManager = new DebugManager(System.currentTimeMillis());

        instance().sqlConnection = instance().getNewSqlConnection();

        fetchIp();

        instance().getSqlConnection().openConnection();

        String timetableLessons = SqlConnection.SqlTableName.TIMETABLE_LESSONS;
        boolean hasLocallyStoredEmail = instance().hasLocallyStoredEmail();

        if (hasLocallyStoredEmail) {
            instance().setUser(new User(tTrainParser, instance().getLocallyStoredEmail()));
        } else {
            instance().setUser(new User(tTrainParser, ""));
        }

        WelcomeForm wForm = new WelcomeForm(tTrainParser);
        addPanelToCard(wForm.getPanel(), WELCOME_PANEL);

        LoginRegisterForm reg = new LoginRegisterForm(tTrainParser);
        addPanelToCard(reg.getPanel(), LOGIN_REGISTER_PANEL);

        boolean hasSql = instance().getUser().hasSqlEntry(timetableLessons);
        boolean hasCroppedTimetableFileAlready = instance().hasCroppedTimetableFileAlready(true);
        if (hasCroppedTimetableFileAlready || hasLocallyStoredEmail || hasSql) {
            instance().openPanel(LOGIN_REGISTER_PANEL);
        } else {
            instance().setUser(new User(tTrainParser, ""));
            instance().openPanel(WELCOME_PANEL);
        }
        initFrame("TTrainParser");
    }

    private static File generateTeachersFile() {
        File newTeacherFile = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream is = classLoader.getResourceAsStream(TTrainParser.TEACHERS_FILE_NAME);
            newTeacherFile = new File(USER_DIRECTORY_FILE_SEP + TTrainParser.TEACHERS_FILE_NAME);
            Files.copy(is, newTeacherFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newTeacherFile;
    }

    private static void generateDataFile() {
        DataFileInfo info = new DataFileInfo();
        info.setDefaults();
        YamlWriter writer = null;
        try {
            writer = new YamlWriter(new FileWriter(USER_DIRECTORY_FILE_SEP + "data.yml"));
            writer.write(info); //writes previously collected data about jpg & pdf file names
            writer.close();
        } catch (IOException | YamlException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
    }

    private static void addPanelToCard(JPanel panel, String welcomePanel) {
        if (instance().getCards() == null) {
            instance().setCards(new JPanel(new CardLayout()));
        }
        instance().cards.add(panel, welcomePanel);
    }

    public static Map<String, String[]> getSubjectNamesWithMultipleTeachers() {
        Map<String, String[]> subjectNamesWithMultipleTeachers = new HashMap<>();
        String fileName = TTrainParser.TEACHERS_FILE_NAME;
        File file = new File(USER_DIRECTORY_FILE_SEP + fileName);
        if (!hasTeachersFile()) {
            generateTeachersFile();
        }
        if (!hasTeachersFile()) return null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }

        while (true) {
            String subjectWithTeachers = null; //Biology --- name1, name2, name3
            try {
                subjectWithTeachers = reader.readLine();
                if (subjectWithTeachers == null) {
                    reader.close();
                    break;
                }
            } catch (IOException e) {
                TTrainParser.getDebugManager().handle(e);
                e.printStackTrace();
            }
            /**Following Strings are for Readability**/
            String subjectName = subjectWithTeachers;
            String[] value = new String[3];
            String midString = " --- ";
            if (subjectWithTeachers.contains(midString)) {
                String[] mainSplit = subjectWithTeachers.split(midString);
                subjectName = mainSplit[0];
                String potentialTeachers = mainSplit[1];

                if (potentialTeachers.contains(", ")) {
                    value = potentialTeachers.split(", ");
                } else value[0] = potentialTeachers;
            } else {
                value[0] = "UNKNOWN";
            }
            subjectNamesWithMultipleTeachers.put(subjectName, value);
        }

        return subjectNamesWithMultipleTeachers;
    }

    private static void initFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setContentPane(instance().getCards());

        DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        int frameHeight = 500;
        int frameWidth = 750;
        frame.setLocation(new Point(mode.getWidth() / 2 - (frameWidth / 2), mode.getHeight() / 2 - (frameHeight / 2)));
        frame.setPreferredSize(new Dimension(frameWidth, frameHeight));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static String fetchIp() {
        BufferedReader reader = null;
        try {
            URL amazonWS = new URL("http://checkip.amazonaws.com"); //GET web service that just returns plaintext of users ip
            reader = new BufferedReader(new InputStreamReader(
                    amazonWS.openStream()));
            instance().hasInternet = true;
            return reader.readLine();
        } catch (Exception e) {
            TTrainParser.getDebugManager().handle(e, "No Internet???");
            instance().hasInternet = false;
        }
        return null;
    }

    public static BufferedImage getNewImage(BufferedImage firstImage, int topLeftX, int topLeftY, int bottomRightX,
                                            int bottomRightY) {
        int subImageHeight = bottomRightY - topLeftY;
        int subImageWidth = bottomRightX - topLeftX;
        return firstImage.getSubimage(topLeftX, topLeftY, subImageWidth, subImageHeight);
    }

    public static ITesseract getTesseractInstance() {
        return tesseractInstance;
    }

    public static DebugManager getDebugManager() {
        return debugManager;
    }

    public static boolean hasTeachersFile() {
        File file = new File(USER_DIRECTORY_FILE_SEP + TTrainParser.TEACHERS_FILE_NAME);
        return file.exists();
    }

    public static TTrainParser instance() {
        return tTrainParser;
    }

    private SqlConnection getNewSqlConnection() {
        return new SqlConnection(tTrainParser);
    }

    public TablePart getTableType(String rgbString) {
        TablePart tableType = TablePart.UNKNOWN;
        int[] rgbArray = new int[3];
        String[] split = rgbString.split(", ");

        for (int i = 0; i < split.length; i++) rgbArray[i] = Integer.parseInt(split[i]);
        int red = rgbArray[0];
        int green = rgbArray[1];
        int blue = rgbArray[2];
        if (wb(red, 211, 10) && wb(green, 211, 10) && wb(blue, 211, 10)) {
            return TablePart.BORDER; //light grey
        } else if (wb(red, (255 + 249) / 2, 5) && wb(green, (255 + 249) / 2, 5) && wb(blue, (255 + 249) / 2, 5)) {
            return TablePart.EXTERIOR_OR_INTERIOR;
        } else if (wb(red, 51, 5) && wb(green, 51, 5) && wb(blue, 51, 5)) {
            return TablePart.IMPORTANT_WRITING;
        }
        return tableType;

    }

    private boolean wb(int number, int ofThisNumber, int within) {
        int lower = ofThisNumber - within;
        int upper = ofThisNumber + within;
        return (number >= lower) && (number <= upper);
    }

    public String pixelRGBToString(Color colour) {
        return colour.getRed() + ", " + colour.getGreen() + ", " + colour.getBlue();
    }

    public String getFileSuffix(File selected) {
        return selected.getName().split("\\.")[1];
    }

    public void createDataFileIfNotPresent() {
        boolean fileExists = new File(TTrainParser.USER_DIRECTORY_FILE_SEP + "data.yml").exists();
        if (!fileExists) generateDataFile();
    }

    public String getLocallyStoredEmail() {
        createDataFileIfNotPresent();
        DataFileInfo info = getYamlReadDatafile();
        return info.email;
    }

    public boolean hasLocallyStoredEmail() {
        createDataFileIfNotPresent();
        return !getLocallyStoredEmail().equals("{NOT BEEN SET}");
    }

    public File getCroppedTimetableFileName(boolean trueForPNG) {
        createDataFileIfNotPresent();
        DataFileInfo info = getYamlReadDatafile();
        return info == null ? null : (new File(USER_DIRECTORY_FILE_SEP + (trueForPNG ? info.timetableCroppedPngFileName : info.timetableCroppedPdfFileName)));
    }

    //Recursive
    public boolean hasCroppedTimetableFileAlready(boolean trueForPNG) {
        createDataFileIfNotPresent();
        File file = getCroppedTimetableFileName(trueForPNG);

        if (file == null) return false;

        String setFilenameInDataFile = getCroppedTimetableFileName(trueForPNG).getName();

        File[] files = new File(USER_DIRECTORY).listFiles();

        for (File aFile : files) {
            if (aFile.getName().equals(setFilenameInDataFile)) return true;
        }
        //No set filename, pdf file has been relocated
        return false;
    }

    public String getActivePanel() {
        return activePanel;
    }

    private void setActivePanel(String panelName) {
        activePanel = panelName;
    }

    /**
     * One of the more fundamental functions to the program. The function, in order, removes...
     * - class unique identifier names, lesson type ie A level and student registration symbols for example / and ?
     * - duplicate whitespace and new line characters
     * - teacher names
     * - room names with support for tutorial sessions for example "Yr2 in SO1" or A2 Tutorial "in MO1"
     * - any random Optically read text that would ruin the parsing process.
     */
    public String depleteFutileInfo(String ocrResult) {
        String depletedText = "";
        ocrResult = ocrResult/*class names or numbers**/
                .replaceAll("[\\[\\(].*[\\]\\)]", "")//[ or a ( followed by anything then a ] or ) room names
                .replaceAll("\\s?/\\s?", "") //Removing the / for being present.
                .replaceAll("\\s?M\\s?", "") //Removing the M for being medical
                .replaceAll("\\s?L\\s?", "") //Removing the L for being late
                .replaceAll("\\?", "") //Removing the ? for not being present
                .replaceAll("\\.", ":") //Has been a time where string has contained this "09.00 - 10:05"
                .replaceAll("A(([Ll])?)((\\s)?)evel", "") //the 'l' has been lost sometimes so "Aevel" and "A evel" will both still work
                .replaceAll("A [Ll]evel", ""); //Some subjects viz Computer Science have lower case l for some reason?

        String[] words = ocrResult.split("\\s+"); //one or more spaces

        List<Integer> removeStrings = new ArrayList<>();
        int dsLog = 0;
        List<Integer> indexesOfA2sToReplaceWithYr = new ArrayList<>();
        /*
        Example input string:
        Wednesday  A2 Tutorial in 830 10:05 - 11:10na Wendy Shem mell  Direct Study - A2 in LRC Directed Study bd11:30 - 12:35 Directed Study  swt = Business studies --- Fiona Davies
         */
        for (String[] teachers : TTrainParser.getSubjectNamesWithMultipleTeachers().values()) {
            for (String scanningForThisTeacher : teachers) {
                if (scanningForThisTeacher == null) continue;
                int index = 0;
                for (String wordInOcr : words) {
                    for (String teacherFirstOrLastName : scanningForThisTeacher.split(" ")) {
                        if (teacherFirstOrLastName.equalsIgnoreCase("UNKNOWN")) continue;
                        boolean wordSimilarToTeacher = calculateDistance(wordInOcr, teacherFirstOrLastName.toLowerCase().startsWith("direct") ? "Directe" : teacherFirstOrLastName) < 3; //"directe" is imbetween Directed and Direct, so good compromise
                        if (scanningForThisTeacher.equals("Directed Study")) {
                            if (wordSimilarToTeacher) { //Word similar to DirectedStudy
                                if (dsLog < 2) { //Disregard the Lesson Identifier "Directed Study"
                                    dsLog++;
                                    continue;
                                } else if (dsLog == 3 || dsLog == 4 || dsLog == 5 || dsLog == 6) { //"Directed" found x2 and "study" been found x2
                                    removeStrings.add(index); //Remove them
                                    dsLog++; //is now 4, 5, 6 or 7
                                    if (dsLog == 7) {
                                        dsLog = 0;  //when 5, has done something with all 3 of the directed studies (why there is 3 i do not know)
                                    }
                                }
                            } else if (dsLog == 2 && wordInOcr.equals("A2")) { //Has come accross first "Directed Study" and is now on "-" or "A2"
                                dsLog++;
                                indexesOfA2sToReplaceWithYr.add(index); // the A2
                                removeStrings.add(index - 1); //removes the dash before from timetable
                            }

                        } else if (wordSimilarToTeacher) { //less than three characters changed between word & a teacher name
                            removeStrings.add(index); //"adding " + wordInOcr + " that was at index " + index + " to removals because teacher wasnt directed study & was part of a teachers name
                        }
                    }
                    index++;
                }
            }
        }

        indexesOfA2sToReplaceWithYr.forEach(index -> {
            //Replace index with yr2 in the ocrResult strnig
            words[index] = "Yr2";
        });

        for (int wordToRemove : removeStrings) {
            words[wordToRemove] = "";
        }

        for (String word : words) {
            if (word.equals("")) continue;
            depletedText += word + " ";
        }

        String pastSevenChars = "       "; //7 spaces
        int beforeYr = -1;
        int beforeColon;
        boolean tutorialCondition = false;
        boolean setTutorBound = false;
        for (int j = 0; j < depletedText.length(); j++) {
            char charAt = depletedText.charAt(j);
            pastSevenChars = pastSevenChars.substring(1) + charAt; //removes pastSeven[0], adds charAt to end
            String pastThreeChars = pastSevenChars.substring(4);

            if (!setTutorBound) { //false, dont potentially update to false when waiting for the colon
                Matcher m = Pattern.compile("\\s(in)\\s").matcher(pastSevenChars);
                tutorialCondition = !pastSevenChars.contains("Yr") && m.find() && pastThreeChars.startsWith("in");
            }
            if (pastThreeChars.startsWith("Yr")) { //will only begin with in if it's tutor
                beforeYr = j - 2;
            } else if (tutorialCondition && !setTutorBound) {
                beforeYr = j - 3;
                setTutorBound = true;
            }

            if (charAt == ':' && beforeYr != -1) { //will be -1 when it doesnt contain Yr, disregard & continue search
                beforeColon = j - 2;
                depletedText = depletedText.substring(0, beforeYr) + (tutorialCondition ? " " : "") + depletedText.substring(beforeColon);
                if (tutorialCondition) {
                    beforeYr = -1;
                    tutorialCondition = false;
                }
            }
        }
        return depletedText;
    }
    /**
     * Recursive levenshtein distance following 3 functions
     **/
    private int calculateDistance(String x, String y) {
        if (x.isEmpty()) return y.length();
        if (y.isEmpty()) return x.length();

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

    public void openPanel(String panelName) {
        if (panelName.equals(CORE_PANEL)) { //Latest tesseractInstance
            coreForm = new CoreForm(tTrainParser);
            addPanelToCard(coreForm.getPanel(), TTrainParser.CORE_PANEL);
        }
        CardLayout cardLayout = (CardLayout) (cards.getLayout());
        cardLayout.show(cards, panelName);
        setActivePanel(panelName);
        cards.revalidate();
    }

    public boolean hasInternet() {
        return fetchIp() != null;
    }

    public SqlConnection getSqlConnection() {
        return sqlConnection;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void updateTimetableUpload() {
        welcomeForm.setUpdating(true);
        openPanel(TTrainParser.WELCOME_PANEL);
    }

    public DataFileInfo getYamlReadDatafile() {
        DataFileInfo info;
        try {
            YamlReader reader = new YamlReader(new FileReader(TTrainParser.USER_DIRECTORY_FILE_SEP + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException exception) {
            exception.printStackTrace();
            info = null; //for easier comparison in future, if null then something has gone wrong & regenerate
        }
        return info;
    }

    public void configureCrsComboBox(JComboBox selectHomeCrsBox) {
        selectHomeCrsBox.addItem("POO / Poole");
        selectHomeCrsBox.addItem("PKS / Parkstone");
        selectHomeCrsBox.addItem("BSM / Branksome");
        selectHomeCrsBox.addItem("BMH / Bournemouth");
        selectHomeCrsBox.addItem("POK / Pokesdown");
        selectHomeCrsBox.addItem("CHR / Christchurch");
        selectHomeCrsBox.addItem("HNA / Hinton Admiral");
        selectHomeCrsBox.addItem("NWM / New Milton");
        selectHomeCrsBox.addItem("SWY / Sway");
        selectHomeCrsBox.addItem("AHS / Ashurst");
        selectHomeCrsBox.addItem("ANF / Ashurst New Forest");
        selectHomeCrsBox.addItem("TTN / Totton");
        selectHomeCrsBox.addItem("LYT / Lymington Town");
    }

    public void changeCrsComboBoxToCurrentCrs(JComboBox selectHomeCrsBox) {
        String homeCrs = user.getHomeCrs();
        if (homeCrs == null) return;
        for (int i = 0; i < selectHomeCrsBox.getItemCount(); i++) {
            String item = (String) selectHomeCrsBox.getItemAt(i);
            if (item.startsWith(homeCrs)) {
                selectHomeCrsBox.setSelectedItem(item);
                break;
            }
        }
    }

    public JPanel getCards() {
        return cards;
    }

    public void setCards(JPanel cards) {
        this.cards = cards;
    }

}