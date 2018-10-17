package me.nathan.ttrainparse;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public class LessonInfo {

    private int textLength;
    private DayOfWeek dayOfWeek;
    private int lessonCount;

    private LinkedList<String> orderedLessons = new LinkedList<>();
    private Map<String, LinkedList<LocalTime>> orderedSubjectStartTimes = new LinkedHashMap<>();
    private Map<String, LinkedList<LocalTime>> orderedSubjectFinishTimes = new LinkedHashMap<>();
    private String lastLesson;
    private String firstLesson;

    public LessonInfo(List<String> words, DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        Map<String, List<String>> subjectAndBounds = new LinkedHashMap<String, List<String>>();

        /**
         * The point of this class is to get all lesson info for a specific day, called from CoreForm.java
         *
         * In subjectAndBounds map, first string is subject name,
         * The List is all the bounds of a subject if it's taught more than once that day
         * For example Business Studies, | "1-2", "6-7"
         */

        int endBoundIndex = 0;

        for (int i = 0; i < words.size(); i++) {
            String currentWord = words.get(i);
            for (String aSubject : TTrainParser.getSubjectNamesWithMultipleTeachers().keySet()) {
                if (aSubject.contains(" ")) {
                    String firstWord = aSubject.split(" ")[0];
                    if (firstWord.equals(currentWord)) {
                        List<String> split = Arrays.asList(aSubject.split(" ")); //started = true because it contains " ", list = "Business", "studies", "two"

                        int lowerBound = i; //If first iteration, endBoundIndex is 0 anyways so its i - 0 which is just i

                        endBoundIndex = lowerBound + (split.size() - 1);

                        String boundString = String.valueOf(lowerBound + ", " + endBoundIndex);

                        List<String> bounds = subjectAndBounds.containsKey(aSubject) ? subjectAndBounds.get(aSubject) : new ArrayList<>(Arrays.asList(boundString));
                        if (subjectAndBounds.containsKey(aSubject)) {
                            bounds.add(boundString);
                        }
                        subjectAndBounds.put(aSubject, bounds);

                    }
                } else if (aSubject.equals(currentWord)) {
                    String boundString = String.valueOf(i + ", " + i);
                    List<String> bounds = subjectAndBounds.containsKey(aSubject) ? subjectAndBounds.get(aSubject) : new ArrayList<>(Arrays.asList(boundString));
                    if (subjectAndBounds.containsKey(aSubject)) {
                        bounds.add(boundString);
                    }
                    subjectAndBounds.put(aSubject, bounds);
                }
            }
        }
        int previousLowerBound = -1;
        for (String subject : subjectAndBounds.keySet()) {
            String[] split = subject.split(" ");
            List<String> allBounds = subjectAndBounds.get(subject);
//            System.out.println(allBounds.size());
            for (String oneBound : allBounds) {
                orderedLessons.add(subject);
                String[] valueSplit = oneBound.split(", ");
                int lowerOrDifferenceBound = Integer.parseInt(valueSplit[0]);
                if (previousLowerBound != -1) {
                    lowerOrDifferenceBound = previousLowerBound + lowerOrDifferenceBound;
                } else {
                    previousLowerBound = lowerOrDifferenceBound - 1;
                }

                int subjectNameLowerBound = lowerOrDifferenceBound;
                int subjectNameUpperBound = subjectNameLowerBound + (split.length - 1);

                int lowerBoundForJustTimes = subjectNameUpperBound + 1;
                int upperBoundForJustTimes = subjectNameUpperBound + 3;

                String timeString = "";
                for (int i = lowerBoundForJustTimes; i <= upperBoundForJustTimes; i++) {
                    timeString += words.get(i);
//                    System.out.println("timeString += words.get(" + i + ") which is " + words.get(i));
                }
                String[] startFinish = timeString.split("-"); //"10:05-11:10" left is start, right is finish
                LocalTime startTime = LocalTime.parse(startFinish[0]);
                LocalTime finishTime = LocalTime.parse(startFinish[1]);
//              System.out.println("time string = " + timeString);
//              System.out.println("Start time for " + subject + " is " + startTime.getHour() + " and " + startTime.getMinute());
//              System.out.println("finish time for " + subject + " is " + finishTime.getHour() + " and " + finishTime.getMinute());

                LinkedList<LocalTime> startTimes = orderedSubjectStartTimes.containsKey(subject) ? new LinkedList<>(orderedSubjectStartTimes.get(subject)) : new LinkedList<>();
                if (subjectAndBounds.containsKey(subject)) startTimes.add(startTime);

                orderedSubjectStartTimes.put(subject, startTimes);

                LinkedList<LocalTime> finishTimes = orderedSubjectFinishTimes.containsKey(subject) ? new LinkedList<>(orderedSubjectFinishTimes.get(subject)) : new LinkedList<>();
                if (subjectAndBounds.containsKey(subject)) {
                    finishTimes.add(finishTime);
                }

                orderedSubjectFinishTimes.put(subject, finishTimes);
            }
        }
        try {
            lastLesson = orderedLessons.get(orderedLessons.size() - 1);
            firstLesson = orderedLessons.get(0);
            lessonCount = orderedLessons.size();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    private int prs(String s) {
        return Integer.parseInt(s);
    }

    public LinkedList<LocalTime> getStartTimes(String lesson) {
        return this.orderedSubjectStartTimes.get(lesson);
    }

    public LinkedList<LocalTime> getFinishTimes(String lesson) {

        return this.orderedSubjectFinishTimes.get(lesson);
    }

    public int getLessonCount() {
        return this.lessonCount;
    }

    public String getFirstLesson() {
        return this.firstLesson;
    }

    public String getLastLesson() {
        return this.lastLesson;
    }

    public LinkedList<String> getLessons() {
        return this.orderedLessons;
    }

    public DayOfWeek getDayOfWeek() {
        return this.dayOfWeek;
    }

    public int getTextLength() {
        return textLength;
    }

}
