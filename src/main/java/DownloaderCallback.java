import java.util.List;
import java.util.Map;

public interface DownloaderCallback {

    void onResponseHeadersReceived(Map<String, List<String>> headers);
    void onContentLengthReceived(long length);

}
