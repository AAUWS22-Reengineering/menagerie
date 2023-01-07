package menagerie.model.menagerie.importer;

import javafx.beans.value.ChangeListener;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.settings.MenagerieSettings;

import java.io.File;
import java.net.URL;

public class WebImportJob extends ImportJob {

  private final FileDownloader fileDownloader;

  /**
   * Constructs a job that will download and import a file from the web into a specified file.
   *
   * @param url        URL of file to download.
   * @param downloadTo File to download URL into.
   */
  public WebImportJob(URL url, File downloadTo, GroupItem addToGroup) {
    this.addToGroup = addToGroup;
    fileDownloader = new FileDownloader(true, url, downloadTo);
  }


  @Override
  void doRunJob(Menagerie menagerie, MenagerieSettings settings) {
    if (!fileDownloader.tryDownload(settings)) {
      setStatus(ImportJobStatus.FAILED_IMPORT);
      return;
    }
    synchronized (this) {
      if (fileDownloader.getFile() != null) {
        file = fileDownloader.getFile();
      }
    }
  }

  @Override
  public synchronized URL getUrl() {
    return fileDownloader.getUrl();
  }

  @Override
  public double getProgress() {
    return fileDownloader.getProgress();
  }

  @Override
  public void addProgressListener(ChangeListener<Number> listener) {
    this.fileDownloader.addProgressListener(listener);
  }

  @Override
  public void removeProgressListener(ChangeListener<Number> listener) {
    this.fileDownloader.removeProgressListener(listener);
  }
}
