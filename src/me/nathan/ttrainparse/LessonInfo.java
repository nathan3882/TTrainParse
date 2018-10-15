package me.nathan.ttrainparse;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public class LessonInfo {

    private int textLength;
    private DayOfWeek dayOfWeek;
    private int lessonCount;

    private LinkedList<String> orderedLessons;
    private Map<String, LocalTime> orderedSubjectStartTimes;
    private Map<String, LocalTime> orderedSubjectFinishTimes;
    private String lastLesson;
    private String firstLesson;

    public LessonInfo(List<String> wordsForDay, DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        Map<String, String> subjectAndBounds = new LinkedHashMap<String, String>();
        //Business Studies, "1-3"
        //Cmp Sci, 3, 5 //3 words after previous subjects upper bound, end bound is 5

        int endBoundIndex = 0;

        for (int i = 0; i < wordsForDay.size(); i++) {
            String currentWord = wordsForDay.get(i);
            for (String aSubject : TTrainParser.getSubjectNamesWithMultipleTeachers().keySet()) {
                if (aSubject.contains(" ")) {
                    String firstWord = aSubject.split(" ")[0];
                    if (firstWord.equals(currentWord)) {
                        List<String> splitted = Arrays.asList(aSubject.split(" ")); //started = true because it contains " ", list = "Business", "studies", "two"

                        int lowerBound = i; //If first iteration, endboundindex is 0 anyways so its i - 0 which is just i

                        endBoundIndex = lowerBound + (splitted.size() - 1);

                        subjectAndBounds.put(aSubject, String.valueOf(lowerBound + ", " + endBoundIndex));
                    }
                } else if (aSubject.equals(currentWord)) {
                    subjectAndBounds.putIfAbsent(aSubject, String.valueOf(i + ", " + i));
                }
            }
        }

        int previousLowerBound = -1;
        for (String subject : subjectAndBounds.keySet()) {
            String[] split = subject.split(" ");
            String[] valueSplit = subjectAndBounds.get(subject).split(", ");
            int lowerOrDifferenceBound = Integer.parseInt(valueSplit[0]);
            if (previousLowerBound != -1) {
                lowerOrDifferenceBound = previousLowerBound + lowerOrDifferenceBound;
            } else {
                previousLowerBound = lowerOrDifferenceBound - 1;
            }

            int subjectNameUpperBound = lowerOrDifferenceBound + (split.length - 1);

            int lowerBoundForJustTimes = subjectNameUpperBound + 1;
            int upperBoundForJustTimes = subjectNameUpperBound + 3;

            String timeString = "";
            for (int i = lowerBoundForJustTimes; i <= upperBoundForJustTimes; i++)
                timeString += wordsForDay.get(i);
            String[] startFinish = timeString.split("-"); //"10:05-11:10" left is start, right is finish

            orderedLessons.add(subject);
            orderedSubjectStartTimes.put(subject, LocalTime.parse(startFinish[0]));
            orderedSubjectFinishTimes.put(subject, LocalTime.parse(startFinish[1]));
        }
        lastLesson = orderedLessons.get(orderedLessons.size() - 1);
        firstLesson = orderedLessons.get(0);
        lessonCount = orderedLessons.size();


    }

    private int prs(String s) {
        return Integer.parseInt(s);
    }

    public LocalTime getStartTime(String lesson) {
        return this.orderedSubjectStartTimes.get(lesson);
    }

    public LocalTime getFinishTime(String lesson) {
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
