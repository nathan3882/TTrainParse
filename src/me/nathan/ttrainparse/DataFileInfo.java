package me.nathan.ttrainparse;


public class DataFileInfo {

    /**
     * Have to be public in order for YAML library to work
     */
    public String timetableCroppedPdfFileName;
    public String timetableCroppepJpgFileName;

    public DataFileInfo() {

    }

    public void setTimetableCroppedPdfFileName(String fileNameWithExtension) {
        this.timetableCroppedPdfFileName = fileNameWithExtension;
    }

    public void setTimetableCroppepJpgFileName(String fileNameWithExtension) {
        this.timetableCroppepJpgFileName = fileNameWithExtension;
    }

    public String getTimetableCroppedPdfFileName() {
        return timetableCroppedPdfFileName;
    }

    public String getTimetableCroppepJpgFileName() {
        return timetableCroppepJpgFileName;
    }
}
