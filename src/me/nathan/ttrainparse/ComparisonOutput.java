package me.nathan.ttrainparse;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Nathan Allanson
 * @purpose To determine whether previously iterated over pixels are part of the timetable border
 */
public class ComparisonOutput {

    public static int leftRightInstantiations = 0;
    public static int topBottomInstantiations = 0;

    private Response response;
    private TTrainParser main;
    private Map<Integer, List<String>> xOrYPixels;
    private Map<Integer, Integer> quantitiesOfBorder; //First integer is the x / y value,
    private boolean calculatingLeftRightBorder;
    private int value;

    public ComparisonOutput(TTrainParser main, ParsedTimetable timetable, Map<Integer, List<String>> previousXYPixels, boolean calculatingLeftRightBorder, HashMap<Integer, Integer> borderCoordinates) {
        this.main = main;
        this.calculatingLeftRightBorder = calculatingLeftRightBorder;
        this.quantitiesOfBorder = new HashMap<Integer, Integer>();
        this.xOrYPixels = previousXYPixels;
        this.quantitiesOfBorder = getBorderQuantities(xOrYPixels);

        /*
         * Following code determines whether a high % of the analysed pixel's colour is the same as the table's border colour
         * if it is, there is 99% probability of this xOrYValue being the border of the table
         */
        for (int xOrYValue : xOrYPixels.keySet()) {
            int upperBound = 0;
            BufferedImage startImage = timetable.getStartingImage();
            if (isCalculatingLeftRightBorder()) {
                int oneThird = startImage.getWidth() / 3;
                upperBound = (oneThird * leftRightInstantiations) + oneThird; //Lower bound + one third of height
            } else {
                int oneThird = startImage.getHeight() / 3;
                upperBound = (oneThird * topBottomInstantiations) + oneThird; //Lower bound + one third of height
            }
            if (xOrYValue >= upperBound)
                break; //Going above what this instance is meant to (new instance every third width or height)");

            Map<Integer, Integer> sortedQuantities = sortQuantitiesOfBorder(quantitiesOfBorder);

            for (Integer xOrY : sortedQuantities.keySet()) { //to get the first entry in the map
                int occurences = sortedQuantities.get(xOrY);
                if (occurences <= 300) {
                    this.setResponse(Response.MIDDLE_NOT_A_BORDER);
                    return;
                }

                Response first = calculatingLeftRightBorder ? Response.VALID_LEFT_BORDER : Response.VALID_TOP_BORDER;
                Response last = calculatingLeftRightBorder ? Response.VALID_RIGHT_BORDER : Response.VALID_BOTTOM_BORDER;

                if (!timetable.getResponses().contains(first)) { //doesnt contain left or top border, hasnt been found yet
                    timetable.addNewResponse(first);
                    this.setResponse(first);
                    setValue(xOrY);
                } else if (!timetable.getResponses().contains(last)) { //is left-right or top-bottom so else is equivalent to the right/bottom border
                    timetable.addNewResponse(last);
                    this.setResponse(last);
                    setValue(xOrY);
                }
                return; //terminate on first iteration because we only want the first / most common
            }
        }
    }


    private LinkedHashMap<Integer, Integer> getBorderQuantities(Map<Integer, List<String>> map) { //fed x/y value with all associated colours
        LinkedHashMap<Integer, Integer> aMap = new LinkedHashMap<>();
        for (int xOrYValue : map.keySet()) {
            int amount = 0;
            for (String aColour : map.get(xOrYValue)) {
                if (main.getTableType(aColour) == TTrainParser.TablePart.BORDER) {
                    amount++;
                }
            }
            aMap.put(xOrYValue, amount);
        }
        return aMap;
    }

    /**
     * Algorithm is a modified mergesort
     * in which the merge is omitted if the highest element in the low sublist is less than the lowest element in the high sublist
     * This algorithm offers guaranteed n log(n) performance.
     */
    private LinkedHashMap<Integer, Integer> sortQuantitiesOfBorder(Map<Integer, Integer> toSort) {
        List<Entry<Integer, Integer>> list = new LinkedList<Entry<Integer, Integer>>(toSort.entrySet());
        Collections.sort(list, new Comparator<Entry<Integer, Integer>>() {
            public int compare(Entry<Integer, Integer> entryOne, Entry<Integer, Integer> entryTwo) {
                return entryTwo.getValue().compareTo(entryOne.getValue()); //highest -> lowest
            }
        });
        LinkedHashMap<Integer, Integer> sorted = new LinkedHashMap<>();

        for (Entry<Integer, Integer> singularEntry : list) sorted.put(singularEntry.getKey(), singularEntry.getValue());

        return sorted;
    }

    public boolean isCalculatingLeftRightBorder() {
        return this.calculatingLeftRightBorder;
    }

    public void setResponse(Response output) {
        this.response = output;
    }

    public Response getResponse() {
        return this.response;
    }

    public enum Response {
        VALID_LEFT_BORDER,
        VALID_RIGHT_BORDER,
        VALID_TOP_BORDER,
        VALID_BOTTOM_BORDER,
        MIDDLE_NOT_A_BORDER
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}