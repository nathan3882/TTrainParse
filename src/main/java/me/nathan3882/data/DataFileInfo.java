package me.nathan3882.data;

/**
 * @author Nathan Allanson
 * @purpose Allows data.yml to be manipulated in order to store user's email and current stored timetable file
 */
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

    public void setDefaults() {
        setTimetableCroppedPngFileName("");
        setEmail("");
        setTimetableCroppedPdfFileName("");
    }
}
