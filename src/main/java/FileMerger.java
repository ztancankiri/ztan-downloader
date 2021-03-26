import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileMerger {

    private String destPath;
    private List<String> fileList;

    public FileMerger(String destPath) {
        this.fileList = new ArrayList<>();
        this.destPath = destPath;
    }

    public void add(String filePath) {
        fileList.add(filePath);
    }

    public void merge() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(destPath);

            for (String filePath : fileList) {
                FileInputStream fileInputStream = new FileInputStream(filePath);
                BufferedInputStream in = new BufferedInputStream(fileInputStream);

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                fileInputStream.close();
                in.close();
            }

            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
