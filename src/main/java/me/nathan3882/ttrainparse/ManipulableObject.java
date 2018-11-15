package me.nathan3882.ttrainparse;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nathan Allanson
 * @purpose Allows easy manipulation of a BufferedImage, Pdf, Png or Jpeg file.
 */
public class ManipulableObject<T> {

    private Object initialUpload;
    private final Class<T> clazz;
    private List<File> activeFiles;

    private static final String FULL_STOP = "\\.";
    
    public ManipulableObject(Class<T> clazz) {
        this.clazz = clazz;
        this.activeFiles = new ArrayList<>();
    }

    public void setInitialUpload(Object initialUpload) {
        this.initialUpload = initialUpload;
    }

    /**
     * @returns initialUpload as an object
     * @apiNote, Cast to java.io.File | java.awt.image.BufferedImage | another potential initialUpload to manipulate accordingly
     */
    public Object getInitialUpload() {
        return initialUpload;
    }

    /**
     * @param newName is the name of the file to be generated from initialUpload
     * @returns new Pdf as a file
     */
    public File toPdf(String newName, boolean deleteJpgIfMade) { //day.name() + ".pdf";
        File file = null;
        if (uploadCastableTo(BufferedImage.class)) {
            File pngOutputFile = new File(TTrainParser.USER_DIRECTORY_FILE_SEP + newName.split(FULL_STOP)[0] + ".png");
            try {
                ImageIO.write((BufferedImage) getInitialUpload(), "png", pngOutputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String newOne = newName.split(FULL_STOP)[0] + ".pdf";
            jpgToPdf(pngOutputFile, newOne, deleteJpgIfMade);
            file = new File(TTrainParser.USER_DIRECTORY_FILE_SEP + newOne);
        } else if (uploadCastableTo(File.class)) {
            File asFile = (File) getInitialUpload();
            String name = asFile.getName();
            String fileSuffix = name.split(FULL_STOP)[1];
            if (fileSuffix.equals(".pdf")) { //Tried to get a pdf file from a pdf file???
                return asFile;
            } else if (fileSuffix.equals("jpg")) {
                jpgToPdf(asFile, newName.split(FULL_STOP)[0] + ".pdf", deleteJpgIfMade);

            }
        }
        return file;
    }

    /*
     * From Stack Overflow
     */
    private void jpgToPdf(File startImageFile, String outputFileName, boolean deleteJpgs) {
        Image image = null;
        try {
            image = Image.getInstance(startImageFile.getName());
        } catch (BadElementException | IOException e) {
            e.printStackTrace();

        }
        Document document = new Document();

        PdfWriter writer = null;
        try {
            File outputFile = new File(outputFileName);
            writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            activeFiles.add(outputFile);
        } catch (FileNotFoundException | DocumentException e) {
            e.printStackTrace();
        }
        Rectangle rec = new Rectangle(0, 0, image.getWidth(), image.getHeight()); //Document size is always same size as image being inserted, minimizes blurryness
        document.setPageSize(rec);
        document.open();

        image.setAbsolutePosition(0, 0); //top left
        try {
            document.add(image);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        writer.close();

        if (deleteJpgs) {
            startImageFile.delete();
        } else {
            activeFiles.add(startImageFile);
        }
    }

    private boolean uploadCastableTo(Class clazz) { 
        return this.clazz == clazz;
    }

    public void deleteAllMade() {
        for (File activeFile : activeFiles) activeFile.delete();
    }
}