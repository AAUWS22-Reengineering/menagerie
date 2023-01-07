package menagerie.model.menagerie.importer;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.settings.MenagerieSettings;

import java.io.File;
import java.net.URL;

public class LocalImportJob extends ImportJob {

  private final FileImporter fileImporter = new FileImporter();

  /**
   * Constructs a job that will import a local file.
   *
   * @param file File to import.
   */
  public LocalImportJob(File file, GroupItem addToGroup) {
    this.file = file;
    this.addToGroup = addToGroup;
  }

  @Override
  void doRunJob(Menagerie menagerie, MenagerieSettings settings) {
    if (!fileImporter.tryImport(file, menagerie, settings)) {
      setStatus(ImportJobStatus.FAILED_IMPORT);
      return;
    }
    synchronized (this) {
      if (fileImporter.getItem() != null) {
        item = fileImporter.getItem();
      }
    }
  }

  @Override
  public synchronized URL getUrl() {
    return null;
  }
}
