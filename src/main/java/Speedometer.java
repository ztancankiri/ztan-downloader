import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalTime;
import java.util.List;

public class Speedometer extends Thread {

    private String filename;
    private Long totalSize;
    private Long downloaded;
    private Long previous;
    private List<Downloader> downloaders;

    public Speedometer(String filename, long totalSize, List<Downloader> downloaders) {
        this.filename = filename;
        this.totalSize = totalSize;
        this.downloaders = downloaders;
        this.downloaded = 0L;
        this.previous = 0L;
    }

    @Override
    public void run() {
        System.out.println();

        while (!downloaded.equals(totalSize)) {
            downloaded = 0L;

            for (Downloader downloader : downloaders) {
                downloaded += downloader.getDownloaded();
            }

            double size = totalSize;
            size /= 1024 * 1024 * 1024; // GB

            double percentage = downloaded * 100;
            percentage /= totalSize;

            double speed = downloaded - previous;

            double speedMB = speed;
            speedMB /= 1024 * 1024; // MB

            long remainingSize = totalSize - downloaded;
            long remainingTime = (long) (remainingSize / speed);

            String time = "";

            if (remainingTime >= 0 && remainingTime <= 86399) {
                LocalTime timeOfDay = LocalTime.ofSecondOfDay(remainingTime);
                time = timeOfDay.toString();
            }

            try {
                filename = URLDecoder.decode(filename, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String info = String.format("\u001B[32mFile: %s \t Size: %.2f GB \t Speed: %.2f MB/s \t Status: %.2f %% \t Remaining: %s\r", filename, size, speedMB, percentage, time);
            System.out.print(info);

            previous = downloaded;

            sleep(1);
        }
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
