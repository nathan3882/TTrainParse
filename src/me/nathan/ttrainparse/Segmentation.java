package me.nathan.ttrainparse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class Segmentation {

    private final BufferedImage allDayImage;

    private LinkedHashMap<Integer, Integer> leftsAndRights;
    private Map<DayOfWeek, BufferedImage> images = new HashMap<>();


    public Segmentation(TTrainParser main) {
        for (int i = 1; i <= 5; i++) images.put(DayOfWeek.of(i), null);

        this.allDayImage = main.allDayCroppedImage;
        int iterativeY = 5;
        LinkedHashMap<Integer, Integer> leftsAndRightXValues = new LinkedHashMap<>(); //Left most x value and then right most x value
        int previouslyRetainedRight = 0;
        int previousLeftGot = -1;
        int width = allDayImage.getWidth();
        for (int xValue = 0; xValue < width; xValue++) {
            String currentPixelString = main.pixelRGBToString(new Color(allDayImage.getRGB(xValue, iterativeY)));
            if (main.getTableType(currentPixelString) == TTrainParser.TablePart.BORDER) {
                if (previousLeftGot == -1) {
                    previousLeftGot = xValue; //previousLeftGot is now first occurrence of border
                } else {
                    if (xValue < (previousLeftGot + 25)) {
                        previouslyRetainedRight = xValue;
                    } else {
                        //came across new border
                        previousLeftGot = xValue;
                    }
                    leftsAndRightXValues.put(previousLeftGot, previouslyRetainedRight);
                }
            }
        }
        this.leftsAndRights = leftsAndRightXValues;
    }

    public BufferedImage getDay(DayOfWeek day) {
        LinkedList<Integer> keys = new LinkedList<>(leftsAndRights.keySet());
        int rightMostBound = 0;
        int leftMostBound = 0;
        /**1 before is right, 2 before is left*/
        try {
            rightMostBound = keys.get(day.getValue() - 1);
            if (day != DayOfWeek.MONDAY) {
                leftMostBound = keys.get(day.getValue() - 2);
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Error getting bounds for day " + day.name() + "!");
            e.printStackTrace();
        }
        images.put(day, TTrainParser.getNewImage(allDayImage, leftMostBound, 0, rightMostBound, allDayImage.getHeight()));
        return images.get(day);
    }
}
