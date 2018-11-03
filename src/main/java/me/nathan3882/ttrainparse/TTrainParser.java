package me.nathan3882.ttrainparse;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TTrainParser extends MessageDisplay {

    /*
     * display live departure board via digital screens, where the information is self-refreshing;
     * , must say Powered by National Rail Enquiries
     *
     * Sending off a SOAP request to "https://lite.realtime.nationalrail.co.uk/OpenLDBWS/wsdl.aspx?ver=yyyy-mm-dd"
     * to retrieve WSDL/Info in XML format to manipulate and get train times
     */

    public static final String USER_DIRECTORY = System.getProperty("user.dir");

    public BufferedImage allDayCroppedImage;

    public static TTrainParser mainInstance;
    public static JFrame frame;
    public WelcomeForm welcomeForm;
    public LoginRegisterForm loginRegisterForm;
    public CoreForm coreForm;

    public static final String WELCOME_PANEL = "welcomePanel";
    public static final String CORE_PANEL = "corePanel";
    public static final String LOGIN_REGISTER_PANEL = "loginRegisterPanel";

    public JPanel cards = null;

    private static JPanel activePanel;

    public CardLayout cardLayout;

    public static final String USER_DIRECTORY_FILE_SEP = USER_DIRECTORY + File.separator;
    private static ITesseract instance = new Tesseract();

    /**
     * Have embedded web browser in side
     */

    public static void main(String[] args) throws IOException {

        BufferedReader reader = null;
        File teachersFile;

        if (!teachersFileExists()) {
            try {
                teachersFile = makeDefaultTeachersFile();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        mainInstance = new TTrainParser();
        frame = new JFrame("TTrainParser");

        WelcomeForm wForm = new WelcomeForm(mainInstance);
        LoginRegisterForm reg = new LoginRegisterForm(mainInstance);
        addPanelToCard(wForm.getPanel(), WELCOME_PANEL);
        addPanelToCard(reg.getPanel(), LOGIN_REGISTER_PANEL);


        frame.setContentPane(mainInstance.cards);

        boolean hasStoredTimetable = mainInstance.hasCroppedTimetableFileAlready(true);

        if (hasStoredTimetable) { //have a png done already
            FileInputStream fis = new FileInputStream(mainInstance.getCroppedTimetableFileName(true));
            mainInstance.allDayCroppedImage = ImageIO.read(fis);
            if (mainInstance.getCurrentEmail() != null) {
                CoreForm cForm = new CoreForm(mainInstance);
                addPanelToCard(cForm.getPanel(), CORE_PANEL);
                mainInstance.openPanel(CORE_PANEL);
            } else {
                mainInstance.openPanel(LOGIN_REGISTER_PANEL);
            }
        } else {
            mainInstance.openPanel(WELCOME_PANEL);
        }

        DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        int frameHeight = 500;
        int frameWidth = 500;
        frame.setLocation(new Point(mode.getWidth() / 2 - (frameWidth / 2), mode.getHeight() / 2 - (frameHeight / 2)));
        frame.setPreferredSize(new Dimension(frameWidth, frameHeight));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);

    }

    private static boolean teachersFileExists() {
        return new File(USER_DIRECTORY_FILE_SEP + "Teacher Names.txt").exists();
    }

    private static void addPanelToCard(JPanel panel, String welcomePanel) {
        if (mainInstance.cards == null) {
            mainInstance.cards = new JPanel(new CardLayout());
        }
        mainInstance.cards.add(panel, welcomePanel);
    }


    public static Map<String, String[]> getSubjectNamesWithMultipleTeachers() {
        Map<String, String[]> subjectNamesWithMultipleTeachers = new HashMap<>();
        String fileName = "Teacher Names.txt";
        File file = new File(USER_DIRECTORY_FILE_SEP + fileName);
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (true) {
            String subjectWithTeachers = null; //Biology - name1, name2, name3
            try {
                subjectWithTeachers = reader.readLine();
                if (subjectWithTeachers == null) {
                    reader.close();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            /**Following Strings are for Readability**/
            String subjectName = subjectWithTeachers;
            String[] value = new String[3];
            if (subjectWithTeachers.contains(" - ")) {
                String[] mainSplit = subjectWithTeachers.split(" - ");
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

    private static File makeDefaultTeachersFile() throws IOException {
        ClassLoader classLoader = TTrainParser.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream("Teacher Names.txt");
        File newTeacherFile = new File(USER_DIRECTORY_FILE_SEP + "Teacher Names.txt");

        if (!newTeacherFile.exists()) newTeacherFile.createNewFile();

        IOUtils.copy(is, new FileOutputStream(newTeacherFile));

        return newTeacherFile;
    }

    public static BufferedImage getNewImage(BufferedImage firstImage, int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
        int subImageHeight = bottomRightY - topLeftY;
        int subImageWidth = bottomRightX - topLeftX;
        return firstImage.getSubimage(topLeftX, topLeftY, subImageWidth, subImageHeight);
    }

    public static ITesseract getTesseractInstance() {
        return instance;
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

    public String getCurrentEmail() {
        YamlReader reader = null;
        DataFileInfo info = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY_FILE_SEP + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException e) {

            e.printStackTrace();
        }
        if (reader == null || info == null) {
            return null;
        }

        return info.email;
    }

    public File getCroppedTimetableFileName(boolean trueForPNG) {

        YamlReader reader = null;
        DataFileInfo info = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY_FILE_SEP + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException e) {
            e.printStackTrace();
        }
        if (reader == null) {
            return null;
        }

        return new File(USER_DIRECTORY_FILE_SEP + (trueForPNG ? info.timetableCroppedPngFileName : info.timetableCroppedPdfFileName));
    }

    public boolean hasCroppedTimetableFileAlready(boolean trueForPNG) {
        YamlReader reader;
        DataFileInfo info;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY_FILE_SEP + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException e) {
            return false;
        }

        String setFilenameInDataFile = (trueForPNG ? info.timetableCroppedPngFileName : info.timetableCroppedPdfFileName);

        File[] files = new File(USER_DIRECTORY).listFiles();

        for (File aFile : files) {
            if (aFile.getName().equals(setFilenameInDataFile)) return true;
        }
        //No set filename, pdf file has been relocated
        return false;
    }

    public static JPanel getActivePanel() {
        return activePanel;
    }

    public void openPanel(String panelName) {
        CardLayout cardLayout = (CardLayout) (cards.getLayout());
        cardLayout.show(cards, panelName);
        cards.revalidate();
    }
}
