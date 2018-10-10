package me.nathan.brockapptesting;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;

public class TTrainParser {

    /*
     * Do File path selection etc here
     *
     * display live departure board via digital screens, where the information is self-refreshing;
     * , must say Powered by National Rail Enquiries
     *
     * Sending off a SOAP request to "https://lite.realtime.nationalrail.co.uk/OpenLDBWS/wsdl.aspx?ver=yyyy-mm-dd"
     * to retrieve WSDL/Info in XML format to manipulate and get train times
     */

    public static TTrainParser mainInstance;
    private static ITesseract instance = new Tesseract();

    public static void main(String[] args) throws IOException {
        mainInstance = new TTrainParser();
        String pathBefore = System.getProperty("user.dir") + File.separator + "Parsed Timetable Data" + File.separator;
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

        String potentialTimetableString = pathBefore + "My Timetable.pdf";
        String potentialTimetableStringJpeg = pathBefore + "My Timetable.jpg";

        boolean parsedAlready = new File(potentialTimetableString).exists();

        BufferedImage selectedFileImage;

        long now = System.currentTimeMillis();

        File selectedFile = new File("");
        //DO FILE SELECTION HERE & SET 'selectedFile' to something
        try {
            selectedFileImage = ImageIO.read(selectedFile); //Get BufferedImage object from File
        } catch (Exception e) {
            e.printStackTrace();
            displayError("Your provided image of timetable could not be found");
            return;
        }

        BufferedImage allDayCroppedImage = null;
        boolean successfulParse = false;

        if (!parsedAlready) {
            ParsedTimetable timetable = new ParsedTimetable(mainInstance, selectedFileImage);
            successfulParse = timetable.successfullyParsed();
            allDayCroppedImage = timetable.getSuccessfullyParsedImage(); //Updates the variable to the parsed one for sementation to do its thing
            File start = new File(potentialTimetableStringJpeg);
            jpgToPdf(start, potentialTimetableString, true); //making a My Timetable.pdf
        } else {
            successfulParse = true;
        }

        Map<DayOfWeek, String> opticallyReadText = new HashMap<>();

        if (!successfulParse) {
            displayError("Parsing was not successful! Does the provided image contain timetable borders?");
            return; //Terminate
        }

        Segmentation segmentation = new Segmentation(mainInstance, allDayCroppedImage); //allday image is the cropped image

        String ocrText = "";
        for (int i = 1; i <= 5; i++) {
            DayOfWeek day = DayOfWeek.of(i);

            /*Make a temp JPG version*/
            File file = new File(day.name() + ".jpg");
            ImageIO.write(segmentation.getDay(day), "jpg", file);
            /*Make a temp JPG version*/

            try {
                jpgToPdf(file, day.name() + ".pdf", true);

                ocrText = getTesseractInstance().doOCR(new File(day.name() + ".pdf"));
            } catch (TesseractException e) {
                displayError("An error occured whilst doing OCR on PDF");
                e.printStackTrace();
            }
            if (ocrText.length() < 30) {
                continue; /*No Lessons*/
            }

            opticallyReadText.put(day, depleteFutileInfo(ocrText));

        }
        for (DayOfWeek day : opticallyReadText.keySet()) {
            if (day == DayOfWeek.THURSDAY) {
                System.out.println(opticallyReadText.get(day));
            }
        }
        System.out.println("Timetable parsed successfully in " + (System.currentTimeMillis() - now) + "ms");

    }

    private static String attemptLoadFileName(String property) {
        return null;
    }

    private static String depleteFutileInfo(String ocrResult) throws IOException {

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

    private static Map<String, String[]> getSubjectNamesWithMultipleTeachers() throws IOException {
        Map<String, String[]> subjectNamesWithMultipleTeachers = new HashMap<>();
        String fileName = "Teacher Names.txt";
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        while (true) {

            String subjectWithTeachers = reader.readLine(); //Biology - name1, name2, name3
            if (subjectWithTeachers == null) {
                reader.close();
                break;
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
    private static void jpgToPdf(File startImageFile, String outputFileName, boolean deleteJpgs) {
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

    public static BufferedImage getNewImage(BufferedImage firstImage, int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
        int subImageHeight = bottomRightY - topLeftY;
        int subImageWidth = bottomRightX - topLeftX;
        BufferedImage newImage = firstImage.getSubimage(topLeftX, topLeftY, subImageWidth, subImageHeight);
        return newImage;
    }

    public static ITesseract getTesseractInstance() {
        return instance;
    }

    public String pixelRGBToString(Color colour) {
        return colour.getRed() + ", " + colour.getGreen() + ", " + colour.getBlue();
    }

    private static void displayError(String string) {
        System.out.println("Error -> " + string);
    }
}
