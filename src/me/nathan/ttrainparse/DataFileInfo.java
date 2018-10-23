package me.nathan.ttrainparse;


public class DataFileInfo {

    /**
     * Have to be public in order for YAML library to work
     */
    public String timetableCroppedPdfFileName;
    public String email;
    public String timetableCroppedPngFileName;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTimetableCroppedPdfFileName(String fileNameWithExtension) {
        this.timetableCroppedPdfFileName = fileNameWithExtension;
    }

    public void setTimetableCroppedPngFileName(String fileNameWithExtension) {
        this.timetableCroppedPngFileName = fileNameWithExtension;
    }
}
