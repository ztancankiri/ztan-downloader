import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class FileMerger {

    private String destPath;
    private List<String> fileList;
    private int bufferSize;

    public FileMerger(String destPath, int bufferSize) {
        this.fileList = new ArrayList<>();
        this.destPath = destPath;
        this.bufferSize = bufferSize;
    }

    public void add(String filePath) {
        fileList.add(filePath);
    }

    public void merge() {
        try {
            destPath = URLDecoder.decode(destPath, "UTF-8");
            FileOutputStream fileOutputStream = new FileOutputStream(destPath);

            for (String filePath : fileList) {
                FileInputStream fileInputStream = new FileInputStream(filePath);
                BufferedInputStream in = new BufferedInputStream(fileInputStream);

                byte[] dataBuffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, bufferSize)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                fileInputStream.close();
                in.close();

                new File(filePath).delete();
            }

            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
