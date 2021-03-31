import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class Application {

    public static List<NetworkInterface> listNetworkInterfaces() throws SocketException {
        List<NetworkInterface> networkInterfaces = new ArrayList<>();

        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        int i = 0;
        while (nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();

            if (nif.isUp() && !nif.isLoopback()) {
                System.out.println(i + " - " + nif.getDisplayName() + ": " + nif.getInetAddresses().nextElement().getHostAddress());
                networkInterfaces.add(nif);
                i++;
            }
        }

        return networkInterfaces;
    }

    public static NetworkInterface getNetworkInterfaceByName(String name) throws SocketException {
        NetworkInterface networkInterface = null;

        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();

            if (nif.isUp() && !nif.isLoopback() && nif.getDisplayName().equals(name)) {
                networkInterface = nif;
                break;
            }
        }

        return networkInterface;
    }

    public static void main(String[] args) throws InterruptedException {
        Options options = new Options();

        options.addOption(Option.builder("d")
                .longOpt("directory")
                .argName("download directory")
                .hasArg()
                .desc("The path for the download directory.")
                .required()
                .build());

        options.addOption(Option.builder("b")
                .longOpt("batch")
                .argName("download list path")
                .hasArg()
                .desc("The path for the list file which has the download links.")
                .build());

        options.addOption(Option.builder("c")
                .longOpt("chunks")
                .argName("number of chunks")
                .hasArg()
                .desc("The number of chunks for each download connection.")
                .build());

        options.addOption(Option.builder("u")
                .longOpt("url")
                .argName("url for download")
                .hasArg()
                .desc("The url of the file which is going to be downloaded.")
                .build());

        options.addOption(Option.builder("i")
                .longOpt("interface")
                .argName("network interface to bind")
                .hasArg()
                .desc("The network interface to bind.")
                .build());

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.getOptions().length < 2) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("zDownloader", options);
                return;
            }

            NetworkInterface nif;

            if (cmd.hasOption("interface")) {
                String interfaceName = cmd.getOptionValue("interface");
                nif = getNetworkInterfaceByName(interfaceName);

                if (nif == null) {
                    System.err.println("There is no network interface with the name '" + interfaceName + "'");
                    System.exit(1);
                }
            }
            else {
                List<NetworkInterface> networkInterfaces = listNetworkInterfaces();
                Scanner scanner = new Scanner(System.in);
                System.out.println();
                System.out.print("Choose Network Interface:  ");
                int idx = scanner.nextInt();
                nif = networkInterfaces.get(idx);
            }

            int chunkCount = 8;

            System.out.println("Selected Network Interface: " + nif.getDisplayName() );

            if (cmd.hasOption("chunks")) {
                chunkCount = Integer.parseInt(cmd.getOptionValue("chunks"));
            }

            String directory = cmd.getOptionValue("directory");

            if (cmd.hasOption("batch")) {
                String batchFilePath = cmd.getOptionValue("batch");
                BufferedReader reader = new BufferedReader(new FileReader(batchFilePath));

                for (String line; (line = reader.readLine()) != null;) {
                    String filename = line.substring(line.lastIndexOf('/') + 1);
                    FileManager fileManager = new FileManager(nif, line, directory, filename, chunkCount);
                    fileManager.start();
                    fileManager.join();
                }
                reader.close();
            }
            else if (cmd.hasOption("url")) {
                String downloadUrl = cmd.getOptionValue("url");

                String filename = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
                FileManager fileManager= new FileManager(nif, downloadUrl, directory, filename, chunkCount);
                fileManager.start();
                fileManager.join();
            }
            else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("zDownloader", options);
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("zDownloader", options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}