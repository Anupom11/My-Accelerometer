package util;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteCSVFile {

    public String getCsvFileName() {
        Long tsLong = System.currentTimeMillis()/1000;

        String csvFileName = "MyNotes"+"_"+tsLong+".csv";
        File filePathName = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/MyNotes/");

        if(!filePathName.exists()) {
            filePathName.mkdir();
        }

        String finalFilePathVal = filePathName.getAbsolutePath() + "/"+ csvFileName;

        return finalFilePathVal;
    }

    public void writeCsvFile(Context context, String[] csvData) {
        String csvHeader = "id, lat, long, x, y, z, heading, body, speed, date, stime, uid";

        // sample data set
        /*String[] csvData = {
                "1, 2.2, 2.2, 1, 2, 3, John Doe,30,john@example.com, 1",
                "1, 2.2, 2.2, 1, 2, 3, John Doe,30,john@example.com, 1"
        };*/

        //---------------------------------------------------------
        Long tsLong = System.currentTimeMillis()/1000;

        String csvFileName = "MyAccelerometer"+"_"+tsLong+".csv";
        //---------------------------------------------------------

        //String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + csvFileName;
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + csvFileName;
        File file = new File(filePath);

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.append(csvHeader);
            writer.append("\n");
            for (String row : csvData) {
                writer.append(row);
                writer.append("\n");
            }
            writer.flush();
            System.out.println("CSV file written successfully to " + filePath);
            Toast.makeText(context, "CSV file written successfully to " + filePath, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
