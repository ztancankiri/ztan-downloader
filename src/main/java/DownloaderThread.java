import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class DownloaderThread extends Thread {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36";
    public static final int BUFFER_SIZE = 16384;

    private String url;
    private String filePath;
    private InputStream inputStream;
    private BufferedInputStream bufferedInputStream;
    private DownloaderCallback downloaderCallback;
    private long start;
    private long end;
    private long offset;

    private long downloadedBytes;

    public DownloaderThread(String url, String filePath, DownloaderCallback downloaderCallback) {
        this.url = url;
        this.filePath = filePath;
        this.downloaderCallback = downloaderCallback;
        this.start = -1;
        this.end = -1;
        this.downloadedBytes = 0;
    }

    public DownloaderThread(String url, String filePath, long start, long end, long offset, DownloaderCallback downloaderCallback) {
        this.url = url;
        this.filePath = filePath;
        this.downloaderCallback = downloaderCallback;
        this.start = start;
        this.end = end;
        this.downloadedBytes = offset;
        this.offset = offset;
    }

    @Override
    public void run() {
        try {
            if (offset == -1) {
                downloadedBytes = end - start + 1;
                return;
            }

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            urlConnection.setRequestProperty("User-Agent", USER_AGENT);

            if (start != -1 && end != -1) {
                if (offset != 0) {
                    start += offset;
                }

                urlConnection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            }

            Map<String, List<String>> requestHeaders = urlConnection.getRequestProperties();
            System.out.println(requestHeaders);

            Map<String, List<String>> responseHeaders = urlConnection.getHeaderFields();
            downloaderCallback.onResponseHeadersReceived(responseHeaders);

            long contentLength = Long.parseLong(responseHeaders.get("Content-Length").get(0));
            downloaderCallback.onContentLengthReceived(contentLength);

            if (start != -1 && end != -1) {
                BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(filePath, offset != 0);

                byte[] dataBuffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, BUFFER_SIZE)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                }

                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }
}