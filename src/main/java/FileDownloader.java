import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileDownloader extends Thread {

    private String url;
    private String filename;
    private int chunkCount;

    public FileDownloader(String url, String filename, int chunkCount) {
        this.url = url;
        this.filename = filename;
        this.chunkCount = chunkCount;
    }

    public FileDownloader(String url, int chunkCount) {
        this.url = url;
        this.chunkCount = chunkCount;
        this.filename = url.substring(url.lastIndexOf('/') + 1);
    }

    @Override
    public void run() {
        DownloaderThread downloaderThread = new DownloaderThread(url, filename, new DownloaderCallback() {
            @Override
            public void onResponseHeadersReceived(Map<String, List<String>> headers) {
//                System.out.println(headers);
            }

            @Override
            public void onContentLengthReceived(long length) {
                long part = (long) Math.floor(length / chunkCount);

                FileMerger fileMerger = new FileMerger(filename);
                List<DownloaderThread> downloaders = new ArrayList<>();

                for (int i = 0; i < chunkCount; i ++) {
                    File file = new File(filename + "." + i);
                    long offset = file.exists() ? file.length() : 0;
                    offset = offset == part ? -1 : offset;

                    if (i == chunkCount - 1) {
                        offset = offset == length - part * i ? -1 : offset;

                        DownloaderThread downloader = new DownloaderThread(url, filename + "." + i, part * i, length - 1, offset, new DownloaderCallback() {

                            @Override
                            public void onResponseHeadersReceived(Map<String, List<String>> headers) {

                            }

                            @Override
                            public void onContentLengthReceived(long length) {

                            }
                        });
                        downloaders.add(downloader);
                        downloader.start();
                    }
                    else {
                        DownloaderThread downloader = new DownloaderThread(url, filename + "." + i, part * i, part * (i + 1) - 1, offset, new DownloaderCallback() {

                            @Override
                            public void onResponseHeadersReceived(Map<String, List<String>> headers) {

                            }

                            @Override
                            public void onContentLengthReceived(long length) {

                            }
                        });
                        downloaders.add(downloader);
                        downloader.start();
                    }

                    fileMerger.add(filename + "." + i);
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

                System.out.println("Merging file...");
                fileMerger.merge();
                System.out.println("File is merged!");

                boolean deleted = true;

                for (int i = 0; i < chunkCount; i++) {
                    File partFile = new File(filename + "." + i);
                    deleted = deleted && partFile.delete();
                }

                if (deleted) {
                    System.out.println("The file is downloaded!");
                }
            }
        });
        downloaderThread.start();
    }
}
