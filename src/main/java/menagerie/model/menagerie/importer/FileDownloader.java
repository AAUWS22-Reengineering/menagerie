package menagerie.model.menagerie.importer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import menagerie.settings.MenagerieSettings;
import menagerie.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileDownloader {

  private static final Logger LOGGER = Logger.getLogger(FileDownloader.class.getName());

  private volatile boolean needsDownload = false;

  private URL url = null;
  private File downloadTo = null;

  private File file = null;

  public FileDownloader() {
  }

  public FileDownloader(boolean needsDownload, URL url, File downloadTo) {
    this.needsDownload = needsDownload;
    this.url = url;
    this.downloadTo = downloadTo;
  }

  private final DoubleProperty progressProperty = new SimpleDoubleProperty(-1);

  /**
   * Tries to download the file from the web and save it to {@link #file}
   *
   * @param settings Application settings to use.
   * @return False if the download fails.
   */
  boolean tryDownload(MenagerieSettings settings) {
    if (needsDownload) {
      try {
        if (downloadTo == null) {
          String folder = settings.defaultFolder.getValue();
          if (folder == null || folder.isEmpty() || !Files.isDirectory(Paths.get(folder))) {
            LOGGER.warning(() ->
                String.format("Default folder '%s' doesn't exist or isn't a folder", folder));
            return false;
          }
          String filename = url.getPath().replaceAll("^.*/", "");

          downloadTo = FileUtil.resolveDuplicateFilename(new File(folder, filename));
        }
        if (downloadTo.exists()) {
          LOGGER.warning(() ->
              String.format("Attempted to download '%s' into pre-existing file '%s'",
                  url.toString(), downloadTo.toString()));
          return false;
        }

        doDownload();
      } catch (RuntimeException e) {
        LOGGER.log(Level.WARNING, e, () ->
            "Unexpected exception while downloading from url: %s" + url.toString());
        return false;
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, e, () ->
            "IOException while downloading file from url: %s" + url.toString());
        return false;
      }
    }
    progressProperty.set(-1);
    return true;
  }

  private void doDownload() throws IOException {
    LOGGER.info(() -> "Downloading: " + url + "\nTo local: " + downloadTo);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.addRequestProperty("User-Agent", "Mozilla/4.0");
    try (ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream())) {
      try (FileOutputStream fos = new FileOutputStream(downloadTo)) {
        final long size = conn.getContentLengthLong();
        final int chunkSize = 4096;
        for (int i = 0; i < size; i += chunkSize) {
          fos.getChannel().transferFrom(rbc, i, chunkSize);

          progressProperty.set((double) i / size);
        }
      }
    }
    conn.disconnect();
    LOGGER.info(() -> "Successfully downloaded: " + url + "\nTo local: " + downloadTo);

    file = downloadTo;
    needsDownload = false;
  }

  File getFile() {
    return file;
  }

  URL getUrl() {
    return url;
  }

  void addProgressListener(ChangeListener<Number> listener) {
    this.progressProperty.addListener(listener);
  }

  void removeProgressListener(ChangeListener<Number> listener) {
    this.progressProperty.removeListener(listener);
  }

  double getProgress() {
    return this.progressProperty.doubleValue();
  }
}
