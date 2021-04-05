import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.NetworkInterface;
import java.nio.file.Paths;

public class Downloader extends Thread {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36";
    public static final int BUFFER_SIZE = 16384;

    private NetworkInterface networkInterface;
    private ContentLengthCallback contentLengthCallback;
    private ErrorReceiveCallback errorReceiveCallback;
    private String url;
    private String directory;
    private String filename;
    private Long startPosition;
    private Long endPosition;

    private CloseableHttpClient httpClient;
    private HttpGet request;
    private Long downloaded;

    public Downloader(NetworkInterface networkInterface, ContentLengthCallback contentLengthCallback, ErrorReceiveCallback errorReceiveCallback, String url, String directory, String filename, Long startPosition, Long endPosition) {
        this.networkInterface = networkInterface;
        this.contentLengthCallback = contentLengthCallback;
        this.errorReceiveCallback = errorReceiveCallback;
        this.url = url;
        this.directory = directory;
        this.filename = filename;
        this.startPosition = startPosition;
        this.endPosition = endPosition;

        this.httpClient = HttpClients.createDefault();
        this.request = new HttpGet(url);

        if (directory != null && filename != null) {
            this.downloaded = getFileLength(Paths.get(directory, filename).toString());
        }

        if (networkInterface != null) {
            this.request.setConfig(RequestConfig.custom().setLocalAddress(networkInterface.getInetAddresses().nextElement()).build());
        }

        this.request.setHeader("User-Agent", USER_AGENT);

        if (startPosition != null && endPosition != null) {
            if (downloaded == null) {
                this.request.setHeader("Range", String.format("bytes=%d-%d", startPosition, endPosition));
            }
            else {
                this.request.setHeader("Range", String.format("bytes=%d-%d", startPosition + downloaded, endPosition));
            }
        }

        if (downloaded == null) {
            this.downloaded = 0L;
        }
    }

    private void download() throws Exception {
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        if (contentLengthCallback != null) {
            contentLengthCallback.onContentLengthReceived(entity.getContentLength());
        }

        if (directory != null && filename != null) {
            saveFile(entity.getContent());
        }

        httpClient.close();
    }

    private void saveFile(InputStream inputStream) throws Exception {
        String filePath = Paths.get(directory, filename).toString();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        FileOutputStream fileOutputStream = new FileOutputStream(filePath, true);

        byte[] dataBuffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = bufferedInputStream.read(dataBuffer, 0, BUFFER_SIZE)) != -1) {
            fileOutputStream.write(dataBuffer, 0, bytesRead);
            this.downloaded += bytesRead;
        }

        fileOutputStream.close();
        bufferedInputStream.close();
    }

    private Long getFileLength(String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            return file.length();
        }
        else {
            return null;
        }
    }

    @Override
    public void run() {
        if (startPosition != null && endPosition != null && downloaded == endPosition - startPosition + 1) {
            return;
        }

        try {
            download();
        } catch (Exception e) {
            errorReceiveCallback.onErrorReceived(this);
        }
    }

    public String getFilename() {
        return filename;
    }

    public Long getStartPosition() {
        return startPosition;
    }

    public Long getEndPosition() {
        return endPosition;
    }

    public Long getDownloaded() {
        return downloaded;
    }
}
