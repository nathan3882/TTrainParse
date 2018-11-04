package me.nathan3882.ttrainparse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Nathan Allanson
 * @purpose Gets a timetable/buffered image that's able to be parsed by OCR, cancelling out every piece of futile external information.
 */
public class ParsedTimetable {

    private enum AnalysisType {
        LEFT_RIGHT_TOP_BOTTOM,
        TOP_BOTTOM_LEFT_RIGHT
    }

    private BufferedImage firstImage;
    private TTrainParser main;

    private List<ComparisonOutput> comparisonOutputs = new ArrayList<>();
    private static List<ComparisonOutput.Response> responsesSoFar = new ArrayList<>(); //I could just iterate through previously stored comparison outputs and

    private int yValueBottomBorder = -1;
    private int yValueTopBorder = -1;
    private int xValueLeftBorder = -1;
    private int xValueRightBorder = -1;

    private BufferedImage newImage = null;

    public ParsedTimetable(TTrainParser main, BufferedImage firstImage) {
        this.main = main;
        this.firstImage = firstImage;

        int height = firstImage.getHeight();
        int width = firstImage.getWidth();

        List<String> storedPixels = new ArrayList<>();
        Map<Integer, Integer> borderCoordinates = new HashMap<>();
        Map<Integer, List<String>> XYPixels = new HashMap<>(); //Integer = X Coordinate of the correlating list of pixels

        doTimetableAnalysis(AnalysisType.LEFT_RIGHT_TOP_BOTTOM,
                width, height, storedPixels, borderCoordinates, XYPixels);

        storedPixels.clear();
        borderCoordinates.clear();
        XYPixels.clear();

        doTimetableAnalysis(AnalysisType.TOP_BOTTOM_LEFT_RIGHT,
                width, height, storedPixels, borderCoordinates, XYPixels);
    }


    /**
     * Following code determines left and right side border coordinates of the timetable
     */
    private void doTimetableAnalysis(AnalysisType analysisType, int width, int height, List<String> storedXorYPixels, Map<Integer, Integer> borderCoordinates, Map<Integer, List<String>> XYPixels) {
        if (analysisType.equals(AnalysisType.LEFT_RIGHT_TOP_BOTTOM)) {
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
                        comparisonOutputs.add( //New comparison output for previous 1/3rd of pixel data
                                new ComparisonOutput(main, this, new HashMap<Integer, List<String>>(XYPixels), true, new HashMap<Integer, Integer>(borderCoordinates)));
                        XYPixels.clear(); //Clears previous 1/3 of pixel data
                    }
                }
            }
        } else if (analysisType.equals(AnalysisType.TOP_BOTTOM_LEFT_RIGHT)) {
            for (int currentYPixel = 0; currentYPixel < height; currentYPixel++) {  //going from bottom to top
                for (int currentXPixel = 0; currentXPixel < width; currentXPixel++) { //then going from left to right
                    String currentPixelString = main.pixelRGBToString(new Color(firstImage.getRGB(currentXPixel, currentYPixel)));

                    if (currentPixelString.equals("211, 211, 211"))
                        borderCoordinates.put(currentXPixel, currentYPixel); //Is a border

                    if (storedXorYPixels.size() == width) {
                        XYPixels.put(currentYPixel, new ArrayList<>(storedXorYPixels));
                        storedXorYPixels.clear();
                    }
                    storedXorYPixels.add(currentPixelString);

                    if (getCondition(XYPixels.size(), height)) {
                        ComparisonOutput.topBottomInstantiations++;
                        //New comparison output for previous 1/3rd of pixel data
                        comparisonOutputs.add(new ComparisonOutput(main, this, new HashMap<Integer, List<String>>(XYPixels), false, new HashMap<Integer, Integer>(borderCoordinates)));
                        XYPixels.clear(); //Clears previous 1/3 of pixel data
                    }
                }
            }
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

    private boolean getCondition(int size, int hOrW) {
        return size >= (hOrW / 3) - 1; //Dividing integers gives absoloute value, ie 9.9 will be 9 so just take 1 away to be accurate
    }

    public boolean successfullyParsed() {
        return getResponses().size() == 4;
    }

    public BufferedImage getStartingImage() {
        return this.firstImage;
    }


    public List<ComparisonOutput.Response> getResponses() {
        return responsesSoFar;
    }

    public void addNewResponse(ComparisonOutput.Response response) {
        responsesSoFar.add(response);
    }

}
