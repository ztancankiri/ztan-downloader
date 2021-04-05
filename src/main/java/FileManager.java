import java.io.File;
import java.net.NetworkInterface;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileManager extends Thread implements ContentLengthCallback, ErrorReceiveCallback {

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

    private boolean partFileExists() {
        File folder = new File(directory);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().contains(filename + ".part-")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void run() {
        File file = new File(Paths.get(directory, filename).toString());

        if (file.exists() && !partFileExists()) {
            return;
        }

        Downloader downloader = new Downloader(networkInterface, this, null, url, null, null, null, null);
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
        FileMerger fileMerger = new FileMerger(filePath, 1024 * 1024 * 50);

        for (int i = 0; i < chunkCount; i ++) {
            Downloader downloader;
            String chunkFilename = String.format("%s.part-%d", filename, i);
            String chunkPath = Paths.get(directory, chunkFilename).toString();
            fileMerger.add(chunkPath);

            if (i == chunkCount - 1) {
                downloader = new Downloader(networkInterface, null, this, url, directory, chunkFilename, chunkSize * i, length - 1);
            }
            else {
                downloader = new Downloader(networkInterface, null, this, url, directory, chunkFilename, chunkSize * i, chunkSize * (i + 1) - 1);
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

    @Override
    public void onErrorReceived(Downloader downloader) {
        downloaders.remove(downloader);

        Downloader newDownloader = new Downloader(networkInterface, null,this,  url, directory, downloader.getFilename(), downloader.getStartPosition(), downloader.getEndPosition());
        downloaders.add(newDownloader);
        newDownloader.start();
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
}
