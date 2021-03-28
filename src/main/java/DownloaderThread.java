import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.NetworkInterface;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class DownloaderThread extends Thread {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36";
    public static final int BUFFER_SIZE = 16384;

    private String url;
    private String filePath;
    private DownloaderCallback downloaderCallback;
    private long start;
    private long end;
    private long offset;
    private long downloadedBytes;
    private NetworkInterface networkInterface;
    private String directory;

    public DownloaderThread(String url, String filePath, long start, long end, long offset, DownloaderCallback downloaderCallback) {
        this.url = url;
        this.filePath = filePath;
        this.downloaderCallback = downloaderCallback;
        this.start = start;
        this.end = end;
        this.downloadedBytes = offset;
        this.offset = offset;
        this.networkInterface = null;
        this.directory = null;
    }

    public DownloaderThread(String url, String filePath, DownloaderCallback downloaderCallback) {
        this(url, filePath, -1, -1, 0, downloaderCallback);
    }

    public DownloaderThread(String url, String filePath) {
        this(url, filePath, -1, -1, 0, null);
    }

    public DownloaderThread(String url, String filePath, long start, long end, long offset) {
        this(url, filePath, start, end, offset, null);
    }

    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    private static CloseableHttpClient getCloseableHttpClient() {
        CloseableHttpClient httpClient = null;
        try {
            httpClient = HttpClients.custom().
                    setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).
                    setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
                    {
                        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                        {
                            return true;
                        }
                    }).build()).build();
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
        }
        return httpClient;
    }

    private void download() throws IOException {
        try {
            if (offset == -1) {
                downloadedBytes = end - start + 1;
                return;
            }

            CloseableHttpClient client = getCloseableHttpClient();
            HttpGet request = new HttpGet(url);

            if (networkInterface != null) {
                RequestConfig config = RequestConfig.custom().setLocalAddress(networkInterface.getInetAddresses().nextElement()).build();
                request.setConfig(config);
            }

            request.setHeader("User-Agent", USER_AGENT);

            if (start != -1 && end != -1) {
                if (offset != 0) {
                    start += offset;
                }

                request.setHeader("Range", "bytes=" + start + "-" + end);
            }

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

            if (downloaderCallback != null) {
                downloaderCallback.onContentLengthReceived(entity.getContentLength());
            }

            if (directory != null && !filePath.contains(directory)) {
                filePath = Paths.get(directory, filePath).toString();
            }

            if (start != -1 && end != -1) {
                BufferedInputStream in = new BufferedInputStream(entity.getContent());
                FileOutputStream fileOutputStream = new FileOutputStream(filePath, offset != 0);

                byte[] dataBuffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, BUFFER_SIZE)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                }

                fileOutputStream.close();
                in.close();
            }

            client.close();
        }
        catch (SSLException e) {
            download();
        }
    }

    @Override
    public void run() {
        try {
            download();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }
}