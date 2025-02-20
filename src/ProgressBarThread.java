import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the class responsible for showing the progress bar in the console.
 */
public class ProgressBarThread extends Thread {
    private final float charPercent;
    private long downloadedBytes;
    private List<Long> downloadedBytesPerPart;
    private boolean downloading;
    private long downloadSpeed;
    private List<Long> downloadSpeeds;
    private final List<Long> partSizes;
    private final String fileName;
    private final FileOutputStream fos;
    private long totalDownloadBytes;
    private final int charAmt;
    private final List<Integer> charPercents;
    private final List<FileOutputStream> fileOutputStreams;
    private final boolean isThreadedDownloading;

    public ProgressBarThread(List<FileOutputStream> fileOutputStreams, List<Long> partSizes, String fileName) {
        this.partSizes = partSizes;
        this.fileName = fileName;
        this.fileOutputStreams = fileOutputStreams;
        charPercent = 0;
        fos = null;
        totalDownloadBytes = 0;
        charAmt = 80/fileOutputStreams.size();
        isThreadedDownloading = true;
        downloading = true;
        charPercents = new ArrayList<>(fileOutputStreams.size());
        downloadedBytesPerPart = new ArrayList<>(fileOutputStreams.size());
        downloadSpeeds = new ArrayList<>(fileOutputStreams.size());
        for (int i = 0; i < fileOutputStreams.size(); i++) {
            charPercents.add((int) (partSizes.get(i) / charAmt));
        }
    }

    /**
     * Progress Bar Constructor to initialise data members.
     *
     * @param fos                Output stream for writing contents from the web to the local file.
     * @param totalDownloadBytes Total size of the file in bytes.
     * @param fileName           Filename of the downloaded file.
     */
    public ProgressBarThread(FileOutputStream fos, long totalDownloadBytes, String fileName) {
        this.charAmt = 20; // value to determine length of terminal progressbar
        this.downloading = true;
        this.downloadSpeed = 0;
        this.downloadedBytes = 0;
        this.totalDownloadBytes = totalDownloadBytes;
        this.fileName = fileName;
        this.fos = fos;
        this.charPercent = (int) (this.totalDownloadBytes / charAmt);

        fileOutputStreams = null;
        partSizes = null;
        isThreadedDownloading = false;
        charPercents = null;
    }

    /**
     * @param downloading true if the file is being downloaded and false if it's not.
     */
    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
        if (downloading) {
            System.out.println("Downloading " + fileName + " ...");
        }
    }

    /**
     * Generates progress bar.
     *
     * @param spinner icon
     * @return String object containing the progress bar.
     */
    private String generateProgressBar(String spinner) {
        if (!isThreadedDownloading) {
            float filled = downloadedBytes / charPercent;
            String a = new String(new char[(int) filled]).replace("\0", "=");
            String b = new String(new char[charAmt - (int) filled]).replace("\0", ".");
            String bar = a + b;
            bar = bar.substring(0, charAmt / 2 - 2) + String.format("%02d", (int) (downloadedBytes * 100 / totalDownloadBytes)) + "%" + bar.substring(charAmt / 2 + 1);
            return "[" + spinner + "]  " + fileName + "  (0KB)[" + bar + "](" + convertBytes(totalDownloadBytes) + ")  " + (float) downloadSpeed / 1000000 + " MB/s      ";
        } else {
            String result = "[" + spinner + "]  "+ convertBytes(totalDownloadBytes) ;
            float filled;
            totalDownloadBytes=0;
            for (int i = 0; i < fileOutputStreams.size(); i++) {
                filled = downloadedBytesPerPart.get(i) / charPercents.get(i);
                totalDownloadBytes += downloadedBytesPerPart.get(i);
                String a = new String(new char[(int) filled]).replace("\0", "=");
                String b = new String(new char[charAmt - (int) filled]).replace("\0", ".");
                String bar = a + b;
                bar = bar.substring(0, charAmt / 2 - 2) + String.format("%02d", (int) (downloadedBytesPerPart.get(i) * 100 / partSizes.get(i))) + "%" + bar.substring(charAmt / 2 + 1);
                result += " [" + bar + "] " + String.format("%.2f",(float) downloadSpeeds.get(i) / 1000000) + " MB/s";
            }
            return result;
        }
    }

    /**
     * @param bytes file size in bytes.
     * @return returns a String object containing the file size with proper units.
     */
    private String convertBytes(long bytes) {
        String sizeWithUnit;
        if (bytes > 1000) {
            bytes = bytes / 1024;
            sizeWithUnit = String.format("%.2f",(float)bytes)  + " KB";
            if (bytes > 1000) {
                bytes = bytes / 1024;
                sizeWithUnit = String.format("%.2f",(float)bytes)  + " MB";
                if (bytes > 1000) {
                    bytes = bytes / 1024;
                    sizeWithUnit = String.format("%.2f",(float)bytes) + "GB";
                }
            }
            return sizeWithUnit;
        } else {
            return totalDownloadBytes + " bytes";
        }
    }

    /**
     * Cleans up the resources.
     */
    private void cleanup() {
        System.out.println("\r" + generateProgressBar("/"));
        if(isThreadedDownloading){
            String sizeWithUnit = convertBytes(totalDownloadBytes);
            System.out.println("Downloaded " + fileName + " of size " + sizeWithUnit + " at " + FileDownloader.getDir() + fileName + " successfully !");
            Drifty_CLI.logger.log("INFO", "Downloaded " + fileName + " of size " + sizeWithUnit + " at " + FileDownloader.getDir() + fileName + " successfully !");
        }else if (downloadedBytes == totalDownloadBytes) {
            String sizeWithUnit = convertBytes(downloadedBytes);
            System.out.println("Downloaded " + fileName + " of size " + sizeWithUnit + " at " + FileDownloader.getDir() + fileName + " successfully !");
            Drifty_CLI.logger.log("INFO", "Downloaded " + fileName + " of size " + sizeWithUnit + " at " + FileDownloader.getDir() + fileName + " successfully !");
        } else {
            System.out.println("Download failed!");
            Drifty_CLI.logger.log("ERROR", "Download failed!");
        }
    }

    /**
     * run method for progress bar.
     */
    @Override
    public void run() {
        long initialMeasurement;
        String[] spinner = new String[]{"/", "-", "\\", "|"};
        List<Long> initialMeasurements = isThreadedDownloading ? new ArrayList<>(fileOutputStreams.size()) : null;
        while (downloading) {
            try {
                if (!isThreadedDownloading) {
                    for (int i = 0; i <= 3; i++) {
                        initialMeasurement = fos.getChannel().size();
                        Thread.sleep(250);
                        downloadedBytes = fos.getChannel().size();
                        downloadSpeed = (downloadedBytes - initialMeasurement) * 4;
                        System.out.print("\r" + generateProgressBar(spinner[i]));
                    }
                } else {
                    for (int i = 0; i <= 3; i++) {
                        for (int j = 0; j < fileOutputStreams.size(); j++) {
                            initialMeasurements.add(j,fileOutputStreams.get(j).getChannel().size());
                        }
                        Thread.sleep(300);
                        long downloadedPartBytes;
                        for (int j = 0; j < fileOutputStreams.size(); j++) {
                            downloadedPartBytes = fileOutputStreams.get(j).getChannel().size();
                            downloadedBytesPerPart.add(j,downloadedPartBytes);
                            downloadSpeeds.add(j, (downloadedPartBytes - initialMeasurements.get(j)) * 4);
                        }
                        System.out.print("\r" + generateProgressBar(spinner[i]));
                    }
                }
            } catch (InterruptedException | IOException ignored) {
            }
        }
        cleanup();
    }
}
