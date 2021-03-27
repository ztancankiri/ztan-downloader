public class Application {

    public static void main(String[] args) throws InterruptedException {
        FileDownloader fileDownloader = new FileDownloader("https://releases.ubuntu.com/20.04.2.0/ubuntu-20.04.2.0-desktop-amd64.iso", 8);
        fileDownloader.start();
        fileDownloader.join();
    }

}