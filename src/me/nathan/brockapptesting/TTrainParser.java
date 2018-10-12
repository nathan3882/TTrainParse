package me.nathan.brockapptesting;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;

public class TTrainParser {

    /*
     * display live departure board via digital screens, where the information is self-refreshing;
     * , must say Powered by National Rail Enquiries
     *
     * Sending off a SOAP request to "https://lite.realtime.nationalrail.co.uk/OpenLDBWS/wsdl.aspx?ver=yyyy-mm-dd"
     * to retrieve WSDL/Info in XML format to manipulate and get train times
     */

    public static final String USER_DIRECTORY = System.getProperty("user.dir");
    public static TTrainParser mainInstance;
    public static JFrame frame;
    public static MainForm mainForm;
    private static String pathBefore = USER_DIRECTORY + File.separator + "Parsed Timetable Data" + File.separator;
    private static ITesseract instance = new Tesseract();

    public static void main(String[] args) throws IOException {
        mainInstance = new TTrainParser();
        frame = new JFrame("TTrainParser");

        mainForm = new MainForm(mainInstance);

        DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        int frameHeight = 500;
        int frameWidth = 500;

        frame.setLocation(new Point(mode.getWidth() / 2 - (frameWidth / 2), mode.getHeight() / 2 - (frameHeight / 2)));
        frame.setPreferredSize(new Dimension(frameWidth, frameHeight));

        frame.setContentPane(mainForm.getWelcomePanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);

    }

    public String depleteFutileInfo(String ocrResult) {

        /**Following Code removes teacher names from OCR string**/
        ocrResult = ocrResult/**class names or numbers**/
                .replaceAll("\\(.*\\)", "")
                /**A Level or BTEC?**/
                .replace("A Level", "")
                .replaceAll("A level", ""); //Computer science is lower case l for some reason? Charlie??
        String[] words = ocrResult.split("\\s+");
        List<String> removeStrings = new ArrayList<>();

        for (String[] teachers : getSubjectNamesWithMultipleTeachers().values()) {
            for (String teacher : teachers) {
                if (teacher == null) continue;
                for (String wordInOcr : words) {
                    for (String teacherFirstOrLastName : teacher.split(" ")) {
                        if (teacherFirstOrLastName.equalsIgnoreCase("UNKNOWN")) continue;
                        int dis = Distance.calculateDistance(wordInOcr, teacherFirstOrLastName);
                        if (dis < 3) { //left than two characters changed
                            removeStrings.add(wordInOcr);
                        }
                    }
                }
            }
        }
        ocrResult = ocrResult.replaceAll("\n", " ");
        for (String wordToRemove : removeStrings) {
            ocrResult = ocrResult.replace(wordToRemove, "");
        }

        /**Following code gets rid of "Yr2 in XXX"*/
        String pastThree = "   ";
        int beforeYr = -1;
        int beforeColon; //No need to initialise & waste memory
        for (int i1 = 0; i1 < ocrResult.length(); i1++) {
            char charAt = ocrResult.charAt(i1);
            pastThree = pastThree.substring(1) + charAt;

            if (pastThree.startsWith("Yr"))
                beforeYr = i1 - 2;

            if (charAt == ':' && beforeYr != -1) { //will be -1 when it doesnt contain Yr, disregard & continue search
                beforeColon = i1 - 2;
                ocrResult = ocrResult.substring(0, beforeYr) + ocrResult.substring(beforeColon);
            }
        }
        return ocrResult;
    }

    private static Map<String, String[]> getSubjectNamesWithMultipleTeachers() {
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


    /*
     * From Stack Overflow
     */
    public void jpgToPdf(File startImageFile, String outputFileName, boolean deleteJpgs) {
        Image image = null;
        try {
            Image.getInstance(startImageFile.getName());
        } catch (BadElementException | IOException e) {
            e.printStackTrace();
        }
        Document document = new Document();

        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(outputFileName));
        } catch (FileNotFoundException | DocumentException e) {
            e.printStackTrace();
        }
        Rectangle rec = new Rectangle(0, 0, image.getWidth(), image.getHeight()); //Document size is always same size as image being inserted, minimizes blurryness
        document.setPageSize(rec);
        document.open();


        image.setAbsolutePosition(0, 0);
        try {
            document.add(image);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        writer.close();
        if (deleteJpgs) startImageFile.delete();
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
        JOptionPane.showMessageDialog(mainForm.getWelcomePanel(), string);
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
        return selected.getName().split(".")[1];
    }

    /**
     * //TODO For the following 2 methods-TODO can I call reader.read() to get latest version of file, or do I have to instantiate YamlReader each time?
     */

    public File getCroppedTimetableFileName(boolean trueForJPG) {

        YamlReader reader = null;
        Map map = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
            map = (Map) reader.read();
        } catch (FileNotFoundException | YamlException e) {
            displayError("An error occurred whilst reading from data.yml file!");
            e.printStackTrace();
        }
        if (reader == null) {
            displayError("There's no data.yml file");
            return null;
        }

        String pdfOrJpg = trueForJPG ? "jpg" : "pdf";
        return new File(USER_DIRECTORY + File.separator + String.valueOf(map.get("timetable-cropped-" + pdfOrJpg + "-file-name")));
    }

    public boolean hasCroppedTimetableFileAlready(boolean trueForJPG) {
        YamlReader reader = null;
        Map map = null;
        try {
            reader = new YamlReader(new FileReader(USER_DIRECTORY + File.separator + "data.yml"));
            map = (Map) reader.read();
        } catch (FileNotFoundException | YamlException e) {
            displayError("An error occurred whilst reading from data.yml file!");
            e.printStackTrace();
        }
        if (reader == null) {
            displayError("There's no data.yml file");
            return false;
        }

        String pdfOrJpg = trueForJPG ? "jpg" : "pdf";
        String setFilenameInDataFile = String.valueOf(map.get("timetable-cropped-" + pdfOrJpg + "-file-name"));
        File[] files = new File(USER_DIRECTORY).listFiles();
        for (File aFile : files) {
            if (aFile.getName().equals(setFilenameInDataFile)) {
                return true;
            }
        }
        //No set filename, pdf file has been relocated
        return false;
    }
}
