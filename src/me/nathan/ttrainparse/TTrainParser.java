package me.nathan.ttrainparse;

import me.nathan.forms.CoreForm;
import me.nathan.forms.LoginRegisterForm;
import me.nathan.forms.WelcomeForm;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TTrainParser {

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

    public JPanel cards;
    public JPanel welcomePanel;
    public JPanel loginRegisterPanel;
    public JPanel corePanel;

    public CardLayout cardLayout;

    private static String pathBefore = USER_DIRECTORY + File.separator + "Parsed Timetable Data" + File.separator;
    private static ITesseract instance = new Tesseract();

    /**
     * GetNextDeparturesWithDetails() returns DeparturesBoardWithDetails  which closest thing is "getDepBoardWithDetails"
     * getDepBoardWithDetails returns StationBoardWithDetails which could potentnailly be what it returns in the first place
     *
     * StationBoardWithDetails object member 'trainServices'
     * returns a list of "ServiceItemWithLocations"
     *
     * ServiceItemWithLocations could be list of ServiceItem
     *
     * ServiceItem list can be used to check whether "currentDestinations"
     * contains required train station, brock
     * if it does, get object "rid" to getServiceDetailsByRID
     *
     * getServiceDetailsByRID returns ServiceDetails
     *
     * ServiceDetails can be used to get 'locations' which returns
     * a list of "ServiceLocation" objects
     *
     * Have embedded web browser in side
     */

    public static void main(String[] args) throws IOException {
        mainInstance = new TTrainParser();
        frame = new JFrame("TTrainParser");

        mainInstance.cards = new JPanel(new CardLayout());

        WelcomeForm wForm = new WelcomeForm(mainInstance);
        mainInstance.welcomeForm = wForm;
        mainInstance.welcomePanel = wForm.getPanel();

        LoginRegisterForm reg = new LoginRegisterForm(mainInstance);
        mainInstance.loginRegisterForm = reg;
        mainInstance.loginRegisterPanel = reg.getPanel();

        mainInstance.cards.add(mainInstance.welcomePanel, WELCOME_PANEL);
        mainInstance.cards.add(mainInstance.loginRegisterPanel, LOGIN_REGISTER_PANEL);

        frame.setContentPane(mainInstance.cards);
        boolean hasStoredTimetable = mainInstance.hasCroppedTimetableFileAlready(true);

        if (hasStoredTimetable) { //have a png done already
            FileInputStream fis = new FileInputStream(mainInstance.getCroppedTimetableFileName(true));
            mainInstance.allDayCroppedImage = ImageIO.read(fis);
            if (mainInstance.getCurrentEmail() != null) {
                CoreForm cForm = new CoreForm(mainInstance);
                mainInstance.coreForm = cForm;
                mainInstance.corePanel = cForm.getWelcomePanel();
                mainInstance.cards.add(mainInstance.corePanel, TTrainParser.CORE_PANEL);
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


    public static Map<String, String[]> getSubjectNamesWithMultipleTeachers() {
        Map<String, String[]> subjectNamesWithMultipleTeachers = new HashMap<>();
        String fileName = "Teacher Names.txt";
        File file = new File(fileName);
        BufferedReader reader = null;
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                    bw.write("Business studies - Fiona Davis, Another oneOfYourTeachers");
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
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

        if (wb(rgbArray[0], 211, 10) && wb(rgbArray[1], 211, 10) && wb(rgbArray[2], 211, 10)) {
            return TablePart.BORDER; //light grey
        } else if (wb(rgbArray[0], (255 + 249) / 2, 5) && wb(rgbArray[1], (255 + 249) / 2, 5) && wb(rgbArray[2], (255 + 249) / 2, 5)) {
            return TablePart.EXTERIOR_OR_INTERIOR;
        } else if (wb(rgbArray[0], 51, 5) && wb(rgbArray[1], 51, 5) && wb(rgbArray[2], 51, 5)) {
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

    /**
     * //TODO For the following 2 methods-TODO can I call reader.read() to get latest version of file, or do I have to instantiate YamlReader each time?
     */

    public String getCurrentEmail() {
        YamlReader reader = null;
        DataFileInfo info = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
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
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException e) {
            e.printStackTrace();
        }
        if (reader == null) {
            return null;
        }

        return new File(USER_DIRECTORY + File.separator + (trueForPNG ? info.timetableCroppedPngFileName : info.timetableCroppedPdfFileName));
    }

    public boolean hasCroppedTimetableFileAlready(boolean trueForPNG) {
        YamlReader reader;
        DataFileInfo info;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
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

    public void openPanel(String panelName) {
        CardLayout cardLayout = (CardLayout) (cards.getLayout());
        cardLayout.show(cards, panelName);
        cards.revalidate();
    }

    public enum TablePart {
        BORDER,
        UNKNOWN,
        IMPORTANT_WRITING,
        EXTERIOR_OR_INTERIOR
    }

    public interface IMessageDisplay {
        void displayMessage(JPanel panel, String message);

        JPanel getPanel();
    }
}
