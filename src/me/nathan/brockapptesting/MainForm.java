package me.nathan.brockapptesting;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;


public class MainForm {

    public JPanel welcomePanel;
    public JLabel welcomeLabel;
    public JFileChooser timetableFileChooser;
    private JButton selectFile;
    private JCheckBox confirmValidTimetable;
    private JButton advanceToTrainButton;
    private TTrainParser main;

    private boolean isValidFile = false;

    public MainForm(TTrainParser main) {
        this.main = main;
        advanceToTrainButton.setEnabled(false);
        confirmValidTimetable.setEnabled(false);
        String welcomeText = "<html><center>Welcome!<br>You've already added your timetable to the app!<br>Press 'Show Trains'</center></html>";
        if (!main.hasTimetablePdfAlready()) {
            welcomeText = "<html><center>Welcome!<br>We've sensed you haven't got a timetable stored already...<br>Add one below along with some basic info about yourself!</center></html>";
        }

        welcomeLabel.setText(welcomeText);

        selectFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timetableFileChooser = new JFileChooser(System.getProperty("user.dir")); //opens current directory it's being run in
                timetableFileChooser.setMultiSelectionEnabled(false); //must select one timetable
                int value = timetableFileChooser.showOpenDialog(welcomePanel); //When clicked, open dialog on the welcomePanel JPanel
                File selected = timetableFileChooser.getSelectedFile();
                if (value == JFileChooser.APPROVE_OPTION) {
                    isValidFile = main.passesPreliminaryChecks(selected);
                    if (isValidFile) {
                        confirmValidTimetable.setEnabled(true);
                        return;
                    }
                    JOptionPane.showMessageDialog(welcomePanel, "We've detected that's an invalid file!");
                    //When click Submit, use isValidFile to condition whether to advance
                }
            }
        });
        //when checkbox for clarifying entered file is checked, advanceToTrainButton.setEnabled(true)
        confirmValidTimetable.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (confirmValidTimetable.isSelected()) {
                    advanceToTrainButton.setEnabled(true);
                }
            }
        });
    }

    public JPanel getWelcomePanel() {
        return welcomePanel;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
