package me.nathan3882.ttrainparse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nathan Allanson
 * @purpose Gets a timetable/buffered image that's able to be parsed by OCR, cancelling out every piece of futile external information.
 */
public class ParsedTimetable {

    private static List<ComparisonOutput.Response> responsesSoFar = new ArrayList<>(); //I could just iterate through previously stored comparison outputs and
    private static DecimalFormat decimalFormat = new DecimalFormat();

    static {
        decimalFormat.setMaximumFractionDigits(2);
    }

    private int prevDone;
    private BufferedImage firstImage;
    private TTrainParser main;
    private List<ComparisonOutput> comparisonOutputs = new ArrayList<>();
    private int yValueBottomBorder = -1;
    private int yValueTopBorder = -1;
    private int xValueLeftBorder = -1;
    private int xValueRightBorder = -1;
    private BufferedImage newImage = null;

    public ParsedTimetable(TTrainParser main, MessageDisplay messageDisplay, BufferedImage firstImage, int previousPrevDone) {
        this.prevDone = previousPrevDone + 1;
        ComparisonOutput.topBottomInstantiations = 0;
        ComparisonOutput.leftRightInstantiations = 0;
        responsesSoFar.clear();
        this.main = main;
        this.firstImage = firstImage;

        int height = firstImage.getHeight();
        int width = firstImage.getWidth();

        List<String> storedPixels = new ArrayList<>();
        Map<Integer, Integer> borderCoordinates = new HashMap<>();
        Map<Integer, List<String>> XYPixels = new HashMap<>(); //Integer = X Coordinate of the correlating list of pixels

        int decByEveryTime = (ComparisonOutput.OCCURENCES_DECREASE_BY * this.getPrevDone());
        float percentage = ((float) decByEveryTime / (float) ComparisonOutput.OCCURENCES_START_CHECK) * 100;
        if (getPrevDone() > 0) {
            messageDisplay.displayMessage("Finding with the chance being force increased by %" + decimalFormat.format(percentage));
        }
        for (AnalysisType type : AnalysisType.values()) {
            doTimetableAnalysis(type,
                    width, height, storedPixels, borderCoordinates, XYPixels);
            storedPixels.clear();
            borderCoordinates.clear();
            XYPixels.clear();
        }
    }

    public int getPrevDone() {
        return this.prevDone;
    }

    /**
     * Following code determines left and right side border coordinates of the timetable
     */
    private void doTimetableAnalysis(AnalysisType analysisType, int width, int height, List<String> storedXorYPixels, Map<Integer, Integer> borderCoordinates, Map<Integer, List<String>> XYPixels) {
        if (analysisType.equals(AnalysisType.LEFT_RIGHT_BORDERS)) {
            for (int currentXPixel = 0; currentXPixel < width; currentXPixel++) { //going from left to right
                for (int currentYPixel = 0; currentYPixel < height; currentYPixel++) {  //then going from bottom to top
                    String currentPixelString = main.pixelRGBToString(new Color(firstImage.getRGB(currentXPixel, currentYPixel)));

                    if (main.getTableType(currentPixelString).equals(TablePart.BORDER))
                        borderCoordinates.put(currentXPixel, currentYPixel);

                    if (storedXorYPixels.size() == height) { //Reached bottom of the pixels, iteration is top -> bottom
                        XYPixels.put(currentXPixel, new ArrayList<>(storedXorYPixels));
                        storedXorYPixels.clear();
                    }
                    storedXorYPixels.add(currentPixelString);
                    if (getCondition(XYPixels.size(), width - 2)) { //analyses third of all pixels for left and right border
                        ComparisonOutput.leftRightInstantiations++;
                        ComparisonOutput output = //New comparison output for previous 1/3rd of pixel data
                                new ComparisonOutput(main, this, new HashMap<>(XYPixels), true, new HashMap<>(borderCoordinates));
                        addComparisonOutput(output);
                        XYPixels.clear(); //Clears previous 1/3 of pixel data
                    }
                }
            }
        } else if (analysisType.equals(AnalysisType.TOP_BOTTOM_BORDERS)) {
            for (int currentYPixel = 0; currentYPixel < height; currentYPixel++) {  //going from bottom to top
                for (int currentXPixel = 0; currentXPixel < width; currentXPixel++) { //then going from left to right
                    String currentPixelString = main.pixelRGBToString(new Color(firstImage.getRGB(currentXPixel, currentYPixel)));

                    if (main.getTableType(currentPixelString).equals(TablePart.BORDER))
                        borderCoordinates.put(currentXPixel, currentYPixel); //Is a border

                    if (storedXorYPixels.size() == width) {
                        XYPixels.put(currentYPixel, new ArrayList<>(storedXorYPixels));
                        storedXorYPixels.clear();
                    }
                    storedXorYPixels.add(currentPixelString);

                    if (getCondition(XYPixels.size(), height)) {
                        ComparisonOutput.topBottomInstantiations++;
                        //New comparison output for previous 1/3rd of pixel data
                        ComparisonOutput output = new ComparisonOutput(main, this, new HashMap<>(XYPixels), false, new HashMap<>(borderCoordinates));
                        addComparisonOutput(output);
                        XYPixels.clear(); //Clears previous 1/3 of pixel data
                    }
                }
            }
        }
    }

    public void addComparisonOutput(ComparisonOutput output) {
        if (output.getResponse() == null) return;
        comparisonOutputs.add(output);
    }

    private void logInstantiation(boolean leftRightTopBottom) {
        if (leftRightTopBottom) {
            ComparisonOutput.leftRightInstantiations++;
        } else {
            ComparisonOutput.topBottomInstantiations++;
        }
    }

    public BufferedImage getSuccessfullyParsedImage() {
        if (successfullyParsed()) { //Top, bottom, left AND right sides of border all found
            for (ComparisonOutput comparisonOutput : comparisonOutputs) {

                ComparisonOutput.Response response = comparisonOutput.getResponse();

                switch (response) {
                    case VALID_TOP_BORDER:
                        this.yValueTopBorder = comparisonOutput.getValue();
                        break;
                    case VALID_BOTTOM_BORDER:
                        this.yValueBottomBorder = comparisonOutput.getValue();
                        break;
                    case VALID_LEFT_BORDER:
                        this.xValueLeftBorder = comparisonOutput.getValue() + 10; // take five away so in segmentation
                        break;
                    case VALID_RIGHT_BORDER:
                        this.xValueRightBorder = comparisonOutput.getValue();
                        break;
                    case MIDDLE_NOT_A_BORDER:
                        break;
                    default:
                        break;
                }
            }
        }
        return this.newImage != null ? this.newImage :
                TTrainParser.getNewImage(firstImage,
                        xValueLeftBorder,
                        yValueTopBorder,
                        xValueRightBorder,
                        yValueBottomBorder);
    }

    private boolean getCondition(int size, int heightOrWidth) {
        return size >= (heightOrWidth / 3) - 1; //Dividing integers gives absoloute value, ie 9.9 will be 9 so just take 1 away to be accurate
    }

    public boolean successfullyParsed() {
        return getResponsesSoFar().size() == 4;
    }

    public BufferedImage getStartingImage() {
        return this.firstImage;
    }

    public List<ComparisonOutput.Response> getResponsesSoFar() {
        return responsesSoFar;
    }

    public void addNewResponse(ComparisonOutput.Response response) {
        if (response == null) return;
        responsesSoFar.add(response);
    }

    public enum AnalysisType {
        LEFT_RIGHT_BORDERS,
        TOP_BOTTOM_BORDERS
    }

}
