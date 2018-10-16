package me.nathan.ttrainparse;


public class DataFileInfo {

    /**
     * Have to be public in order for YAML library to work
     */
    public String timetableCroppedPdfFileName;
    public String timetableCroppedJpgFileName;
    public String email;

    public DataFileInfo() {

    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTimetableCroppedPdfFileName(String fileNameWithExtension) {
        this.timetableCroppedPdfFileName = fileNameWithExtension;
    }

    public void setTimetableCroppepJpgFileName(String fileNameWithExtension) {
        this.timetableCroppedJpgFileName = fileNameWithExtension;
    }
}
