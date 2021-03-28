import java.io.File;
import java.net.NetworkInterface;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileDownloader extends Thread {

    private String url;
    private String filename;
    private int chunkCount;
    private NetworkInterface networkInterface;
    private String directory;

    public FileDownloader(String url, String filename, int chunkCount) {
        this.url = url;
        this.filename = filename;
        this.chunkCount = chunkCount;
        this.networkInterface = null;
        this.directory = null;
    }

    public FileDownloader(String url, int chunkCount) {
        this(url, url.substring(url.lastIndexOf('/') + 1), chunkCount);
    }

    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public void run() {
        DownloaderThread downloaderThread = new DownloaderThread(url, filename, length -> {
            long part = (long) Math.floor(length / chunkCount);

            FileMerger fileMerger = new FileMerger(Paths.get(directory, filename).toString());
            List<DownloaderThread> downloaders = new ArrayList<>();

            for (int i = 0; i < chunkCount; i ++) {
                File file = new File(Paths.get(directory, filename + "." + i).toString());
                long offset = file.exists() ? file.length() : 0;
                offset = offset == part ? -1 : offset;

                DownloaderThread downloader;

                if (i == chunkCount - 1) {
                    offset = offset == length - part * i ? -1 : offset;
                    downloader = new DownloaderThread(url, filename + "." + i, part * i, length - 1, offset);
                }
                else {
                    downloader = new DownloaderThread(url, filename + "." + i, part * i, part * (i + 1) - 1, offset);
                }

                downloaders.add(downloader);
                downloader.setNetworkInterface(networkInterface);
                downloader.setDirectory(directory);
                downloader.start();

                fileMerger.add(Paths.get(directory, filename + "." + i).toString());
            }

            Speedometer speedometer = new Speedometer(filename, length, downloaders);
            speedometer.start();

            for (DownloaderThread downloader : downloaders) {
                try {
                    downloader.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                speedometer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            fileMerger.merge();
        });
        downloaderThread.setNetworkInterface(networkInterface);
        downloaderThread.start();
        try {
            downloaderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
