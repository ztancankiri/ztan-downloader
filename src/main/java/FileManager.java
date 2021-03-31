import java.io.File;
import java.net.NetworkInterface;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileManager extends Thread implements DownloaderCallback {

    private NetworkInterface networkInterface;
    private String url;
    private String directory;
    private String filename;
    private Integer chunkCount;

    private List<Downloader> downloaders;

    public FileManager(NetworkInterface networkInterface, String url, String directory, String filename, Integer chunkCount) {
        this.networkInterface = networkInterface;
        this.url = url;
        this.directory = directory;
        this.filename = filename;
        this.chunkCount = chunkCount;

        this.downloaders = new ArrayList<>();
    }

    @Override
    public void run() {
        Downloader downloader = new Downloader(networkInterface, this, url, null, null, null, null, null);
        downloader.start();

        try {
            downloader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onContentLengthReceived(long length) {
        double tmp = chunkCount;
        long chunkSize = (long) Math.floor(length / tmp);

        String filePath = Paths.get(directory, filename).toString();
        FileMerger fileMerger = new FileMerger(filePath);

        for (int i = 0; i < chunkCount; i ++) {
            Downloader downloader;
            String chunkFilename = String.format("%s.part-%d", filename, i);
            String chunkPath = Paths.get(directory, chunkFilename).toString();

            fileMerger.add(chunkPath);

            Long chunkLength = getChunkLength(chunkPath);

            if (i == chunkCount - 1) {
                downloader = new Downloader(networkInterface, null, url, directory, chunkFilename, chunkSize * i, length - 1, chunkLength);
            }
            else {
                downloader = new Downloader(networkInterface, null, url, directory, chunkFilename, chunkSize * i, chunkSize * (i + 1) - 1, chunkLength);
            }

            downloaders.add(downloader);
            downloader.start();
        }

        boolean flag = startSpeedometer(filename, length, downloaders);
        flag = flag && waitForDownloaders();

        if (flag) {
            fileMerger.merge();
        }
    }

    private boolean waitForDownloaders() {
        for (Downloader downloader : downloaders) {
            try {
                downloader.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private boolean startSpeedometer(String filename, long length, List<Downloader> downloaders) {
        Speedometer speedometer = new Speedometer(filename, length, downloaders);
        speedometer.start();

        try {
            speedometer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private Long getChunkLength(String chunkPath) {
        File file = new File(chunkPath);

        if (file.exists()) {
            return file.length();
        }
        else {
            return null;
        }
    }
}
