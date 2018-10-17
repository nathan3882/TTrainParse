package me.nathan.ttrainparse;

import me.nathan.ttrainparse.ComparisonOutput.Response;

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

    private BufferedImage firstImage;
    private TTrainParser main;

    //Integer = X Coordinate of the correlating list of pixels
    private Map<Integer, List<String>> XYPixels = new HashMap<>();
    private List<ComparisonOutput> comparisonOutputs = new ArrayList<>();
    private static List<Response> responsesSoFar = new ArrayList<>(); //I could just iterate through previously stored comparison outputs and

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

        /**
         * Following code is to determine left and right side coordinates of the table
         */

        System.out.println("Determining left & right border coordinates...");
        List<String> currentYPixelsStoredOnXPixel = new ArrayList<>();
        List<String> currentXPixelsStoredOnYPixel = new ArrayList<>();
        Map<Integer, Integer> borderCoordinates = new HashMap<>();

        for (int currentXPixel = 0; currentXPixel < width; currentXPixel++) { //going from left to right
            for (int currentYPixel = 0; currentYPixel < height; currentYPixel++) {  //then going from bottom to top
                String currentPixelString = main.pixelRGBToString(new Color(firstImage.getRGB(currentXPixel, currentYPixel)));

                if (currentPixelString.equals("211, 211, 211"))
                    borderCoordinates.put(currentXPixel, currentYPixel); //Is a border

                if (currentYPixelsStoredOnXPixel.size() == height) { //Reached bottom of the pixels, iteration is top -> bottom
                    XYPixels.put(currentXPixel, new ArrayList<>(currentYPixelsStoredOnXPixel));
                    currentYPixelsStoredOnXPixel.clear();
                }
                currentYPixelsStoredOnXPixel.add(currentPixelString);
                if (getCondition(XYPixels.size(), width - 2)) { //analyses third of all pixels for left and right border
                    ComparisonOutput.leftRightInstantiations++;
                    //New comparison output for previous 1/3rd of pixel data
                    comparisonOutputs.add(new ComparisonOutput(main, this, new HashMap<Integer, List<String>>(XYPixels), true, new HashMap<Integer, Integer>(borderCoordinates)));
                    XYPixels.clear(); //Clears previous 1/3 of pixel data
                }
            }
        }
        borderCoordinates.clear();
        XYPixels.clear();
        for (int currentYPixel = 0; currentYPixel < height; currentYPixel++) {  //going from bottom to top
            for (int currentXPixel = 0; currentXPixel < width; currentXPixel++) { //then going from left to right
                String currentPixelString = main.pixelRGBToString(new Color(firstImage.getRGB(currentXPixel, currentYPixel)));

                if (currentPixelString.equals("211, 211, 211"))
                    borderCoordinates.put(currentXPixel, currentYPixel); //Is a border

                if (currentXPixelsStoredOnYPixel.size() == width) {
                    XYPixels.put(currentYPixel, new ArrayList<>(currentXPixelsStoredOnYPixel));
                    currentXPixelsStoredOnYPixel.clear();
                }
                currentXPixelsStoredOnYPixel.add(currentPixelString);

                if (getCondition(XYPixels.size(), height)) {
                    ComparisonOutput.topBottomInstantiations++;
                    //New comparison output for previous 1/3rd of pixel data
                    comparisonOutputs.add(new ComparisonOutput(main, this, new HashMap<Integer, List<String>>(XYPixels), false, new HashMap<Integer, Integer>(borderCoordinates)));
                    XYPixels.clear(); //Clears previous 1/3 of pixel data
                }
            }
        }
    }

    public BufferedImage getSuccessfullyParsedImage() {
        if (successfullyParsed()) { //Top, bottom, left AND right sides of border all found
            for (ComparisonOutput comparisonOutput : comparisonOutputs) {
                Response response = comparisonOutput.getResponse();
                switch (response) {
                    case VALID_TOP_BORDER:
                        this.yValueTopBorder = comparisonOutput.getValue();
                        break;
                    case VALID_BOTTOM_BORDER:
                        this.yValueBottomBorder = comparisonOutput.getValue();
                        break;
                    case VALID_LEFT_BORDER:
                        this.xValueLeftBorder = comparisonOutput.getValue();
                        break;
                    case VALID_RIGHT_BORDER:
                        this.xValueRightBorder = comparisonOutput.getValue();
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
        return size >= (hOrW / 3) - 1; //Dividing integers gives absolout value, ie 9.9 will be 9 so just take 1 away to be accurate
    }

    public boolean successfullyParsed() {
        return getResponses().size() == 4;
    }

    public BufferedImage getStartingImage() {
        return this.firstImage;
    }


    public List<Response> getResponses() {
        return responsesSoFar;
    }

    public void addNewResponse(Response response) {
        responsesSoFar.add(response);
    }

}
