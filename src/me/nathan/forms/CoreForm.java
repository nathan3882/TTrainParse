package me.nathan.forms;

import me.nathan.ttrainparse.TTrainParser;

import javax.swing.*;
import java.time.DayOfWeek;
import java.util.Date;

public class CoreForm {
    private final TTrainParser main;
    private JPanel coreFormPanel;
    private JLabel currentDayLabel;
    private JLabel mainInfoLabel;

    public CoreForm(TTrainParser main) {
        this.main = main;
        DayOfWeek currentDay = DayOfWeek.of(new Date().getDay());
        String cap = currentDay.name();
        currentDayLabel.setText(currentDayLabel.getText().replace("{DAY}", cap.substring(0, 1).toUpperCase() + cap.substring(1)));
        mainInfoLabel.setText("<html></html>");
        /**
         * Get OCR string as ocrString
         *
         * ocrString.split[" "]
         *    <Subject, Array Position> list of integers is the position in split array the subject is
         * Map<String, List<Integer>
         * Iterate through splitting, if come across subject name add it to map
         * times for the X lesson are between X lesson and the next lowest number that's been stored referencing the split array
         *
         *
         *
         * Friday   Biology 09:00 - 10:05     Biology 10:05 - 11:10   ;   Business studies 11:30 - 12:35
         *
         *
         */
    }
}
