public interface DownloaderCallback {

    void onContentLengthReceived(long length);

    void onErrorReceived(Downloader downloader);

}
