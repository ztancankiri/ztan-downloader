import java.time.LocalTime;
import java.util.List;

public class Speedometer extends Thread {

    private long totalLength;
    private long downloadedLength;
    private long previousLength;
    private List<DownloaderThread> downloaders;

    public Speedometer(long totalLength, List<DownloaderThread> downloaders) {
        this.totalLength = totalLength;
        this.downloaders = downloaders;
        this.downloadedLength = 0;
        this.previousLength = 0;
    }

    @Override
    public void run() {
        while (downloadedLength != totalLength) {
            downloadedLength = 0;

            for (DownloaderThread downloader : downloaders) {
                downloadedLength += downloader.getDownloadedBytes();
            }

            double size = totalLength;
            size /= 1024 * 1024 * 1024; // GB

            double percentage = downloadedLength * 100;
            percentage /= totalLength;

            double speed = downloadedLength - previousLength;

            double speedMB = speed;
            speedMB /= 1024 * 1024; // MB

            long remainingSize = totalLength - downloadedLength;
            long remainingTime = (long) (remainingSize / speed);

            String time = "";

            if (remainingTime >= 0 && remainingTime <= 86399) {
                LocalTime timeOfDay = LocalTime.ofSecondOfDay(remainingTime);
                time = timeOfDay.toString();
            }

            String info = String.format("Size: %.2f GB \t Speed: %.2f MB/s \t Status: %.2f %% \t Remaining: %s", size, speedMB, percentage, time);
            System.out.println(info);

            previousLength = downloadedLength;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Speedometer is done!");
    }
}
