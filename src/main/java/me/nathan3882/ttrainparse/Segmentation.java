package me.nathan3882.ttrainparse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class Segmentation {

    private final BufferedImage allDayImage;
    private final int iterativeY;

    private LinkedHashMap<Integer, Integer> leftsAndRights;
    private Map<DayOfWeek, BufferedImage> images = new HashMap<>();
    private TTrainParser main;
    private int mondayLeft = -1;


    public Segmentation(TTrainParser main) {
        this.main = main;
        for (int i = 1; i <= 5; i++) images.put(DayOfWeek.of(i), null);
        this.allDayImage = main.allDayCroppedImage;
        this.iterativeY = 5;
        this.leftsAndRights = getLeftsAndRights();
    }

    private LinkedHashMap<Integer, Integer> getLeftsAndRights() {
        LinkedHashMap<Integer, Integer> leftsAndRightXValues = new LinkedHashMap<>(); //Left most x value and then right most x value
        int previouslyRetainedRight = -1;
        int previousLeftGot = -1;
        int width = allDayImage.getWidth();
        for (int xValue = 0; xValue < width; xValue++) {
            String currentPixelString = main.pixelRGBToString(new Color(allDayImage.getRGB(xValue, iterativeY)));
            if (main.getTableType(currentPixelString) == TablePart.BORDER) {
                if (previousLeftGot == -1) {
                    previousLeftGot = xValue; //previousLeftGot is now first occurrence of border (to the left of monday)
                    this.mondayLeft = previousLeftGot;
                } else if (xValue < (previousLeftGot + 25)) { //pixel belonging to same border position, a thick border
                    previouslyRetainedRight = xValue;
                } else { //came across new border
                    previousLeftGot = xValue;
                    previouslyRetainedRight = xValue; //Make the right pixel same as left incase xValue is never smaller than previousLeftGot + 25
                }
                leftsAndRightXValues.put(previousLeftGot, previouslyRetainedRight);
            }
        }
        return leftsAndRightXValues;
    }

    public BufferedImage getDay(DayOfWeek day) {
        LinkedList<Integer> keys = new LinkedList<>(leftsAndRights.keySet());
        int rightMostBound = 0;
        int leftMostBound = this.mondayLeft; //should be the first occurence of border, ie disregarding the times 9 through 16
        /**1 before is right, 2 before is left*/
        try {
            int dayValue = day.getValue();
            rightMostBound = keys.get(dayValue - 1);  //For monday, it's index 1 - 1
            if (day != DayOfWeek.MONDAY) {
                leftMostBound = keys.get(dayValue - 2); //For tuesday's left most bound = dayValue ie 2 - 2 so is first entry for tuesdays left most bound
            }
        } catch (IndexOutOfBoundsException e) {
            //System.out.println("Error getting bounds for day " + day.name() + "!");
            e.printStackTrace();
        }
        images.put(day, TTrainParser.getNewImage(allDayImage, leftMostBound, 0, rightMostBound, allDayImage.getHeight()));
        return images.get(day);
    }
}
