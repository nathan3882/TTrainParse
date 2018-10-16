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

    public static BufferedImage allDayCroppedImage;

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

    public static void main(String[] args) throws IOException {
        mainInstance = new TTrainParser();
        frame = new JFrame("TTrainParser");
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }
        mainInstance.cards = new JPanel(new CardLayout());

        mainInstance.welcomeForm = new WelcomeForm(mainInstance);
        mainInstance.welcomePanel = mainInstance.welcomeForm.getWelcomePanel();

        mainInstance.loginRegisterForm = new LoginRegisterForm(mainInstance);
        mainInstance.loginRegisterPanel = mainInstance.loginRegisterForm.getLoginRegisterPanel();

        mainInstance.cards.add(mainInstance.welcomePanel, WELCOME_PANEL);
        mainInstance.cards.add(mainInstance.loginRegisterPanel, LOGIN_REGISTER_PANEL);

        frame.setContentPane(mainInstance.cards);

        boolean hasStoredTimetable = mainInstance.hasCroppedTimetableFileAlready(true);

        if (hasStoredTimetable) { //have a pdf done already

            /****
             *
             *
             * This LOADING of the pre croppe timetable is where the program will no longer work
             * Try using PNG -> pdf
             * if that doesnt work parsing will have to occur each time, no storing.
             *
             *
             */
            allDayCroppedImage = ImageIO.read(new File(path));

            if (mainInstance.getCurrentEmail() != null) {
                mainInstance.coreForm = new CoreForm(mainInstance);
                mainInstance.corePanel = mainInstance.coreForm.getWelcomePanel();
                mainInstance.cards.add(mainInstance.corePanel, mainInstance.CORE_PANEL);
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
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
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

    public static BufferedImage getNewImage(BufferedImage firstImage, int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
        int subImageHeight = bottomRightY - topLeftY;
        int subImageWidth = bottomRightX - topLeftX;
        BufferedImage newImage = firstImage.getSubimage(topLeftX, topLeftY, subImageWidth, subImageHeight);
        return newImage;
    }

    public static ITesseract getTesseractInstance() {
        return instance;
    }

    public void displayError(String string) {
        JOptionPane.showMessageDialog(welcomeForm.getWelcomePanel(), string);
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
            displayError("An error occurred whilst reading from data.yml file!");
            e.printStackTrace();
        }
        if (reader == null) {
            displayError("There's no data.yml file");
            return null;
        }

        return info.email;
    }

    public File getCroppedTimetableFileName(boolean trueForJPG) {

        YamlReader reader = null;
        DataFileInfo info = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException e) {
            displayError("An error occurred whilst reading from data.yml file!");
            e.printStackTrace();
        }
        if (reader == null) {
            displayError("There's no data.yml file");
            return null;
        }

        return new File(USER_DIRECTORY + File.separator + (trueForJPG ? info.timetableCroppedJpgFileName : info.timetableCroppedPdfFileName));
    }

    public boolean hasCroppedTimetableFileAlready(boolean trueForJPG) {
        YamlReader reader = null;
        DataFileInfo info = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
            info = reader.read(DataFileInfo.class);
        } catch (FileNotFoundException | YamlException e) {
            displayError("An error occurred whilst reading from data.yml file!");
            e.printStackTrace();
        }
        if (reader == null) {
            displayError("There's no data.yml file");
            return false;
        }

        String pdfOrJpg = trueForJPG ? "Jpg" : "Pdf";
        String setFilenameInDataFile = (trueForJPG ? info.timetableCroppedJpgFileName : info.timetableCroppedPdfFileName);

//        System.out.println("PDF FILE NAME APAPRENTLY STORED = " + info.timetableCroppedPdfFileName);
//        System.out.println("FILE checker = " + setFilenameInDataFile);
        File[] files = new File(USER_DIRECTORY).listFiles();

        for (File aFile : files) {
            if (aFile.getName().equals(setFilenameInDataFile)) {
//                System.out.println("equal");
                return true;
            } else {
//                System.out.println(aFile.getName() + " != " + setFilenameInDataFile);
            }
        }
//        No set filename, pdf file has been relocated
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


    public TTrainParser gI() {
        return mainInstance;
    }

}
