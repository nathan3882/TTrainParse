package me.nathan3882.ttrainparse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class DebugManager {

    private File file;
    private long runtime;
    private String fullFileName = "";
    private BufferedWriter writer;

    public DebugManager(long runtime) {
        this.runtime = runtime;

        Date date = new Date(runtime);
        fullFileName += date.getDay() + "." + date.getMonth() + "  " + date.getHours() + "." + date.getMinutes() + "." + date.getSeconds();
        fullFileName += ".log";
        this.file = new File(fullFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                TTrainParser.getDebugManager().handle(e);
                System.out.println("Can't write to directory, is jar in restricted environment? Did you run as administrator?");
                e.printStackTrace();
            }
        }
    }

    public BufferedWriter startWriting() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            startWriting(writer);
            return writer;
        } catch (IOException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return null;
    }

    public BufferedWriter startWriting(BufferedWriter writer) {
        this.writer = writer;
        return writer;
    }

    public void closeWriter() {
        try {
            this.writer.close();
            this.writer = null;
        } catch (IOException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
    }

    public BufferedWriter getWriter() {
        return this.writer;
    }

    public void handle(Exception e, String header) {
        writeLine(header);
        handle(e);
    }

    public void handle(Exception e) {
        StackTraceElement[] st = e.getStackTrace();
        writeLine("----------");
        for (StackTraceElement ste : st) {
            System.out.println("-");
            writeLine(String.valueOf(e.getCause()));
            writeLine("was caused at line number '" + ste.getLineNumber() + "'");
            writeLine("in class " + ste.getClassName());
        }
        writeLine("----------");
        closeWriter();
    }

    private void writeLine(String string) {
        try {
            this.writer = getWriter() == null ? startWriting() : getWriter();
            this.writer.write(string);
            this.writer.newLine();
        } catch (IOException e) {
            TTrainParser.getDebugManager().handle(e);
            System.out.println("Can't write to directory, is jar in restricted environment? Did you run as administrator?");
            e.printStackTrace();
        }
    }

    private void write(String string) {
        try {
            this.writer = getWriter() == null ? startWriting() : getWriter();
            writer.append(string);
            closeWriter();
        } catch (IOException e) {
            TTrainParser.getDebugManager().handle(e);
            System.out.println("Can't write to directory, is jar in restricted environment? Did you run as administrator?");
            e.printStackTrace();
        }
    }

}
