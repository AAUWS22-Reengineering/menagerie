package menagerie.model.menagerie.importer;

import menagerie.gui.util.ItemUtil;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.settings.MenagerieSettings;

import java.io.File;
import java.util.logging.Logger;

public class FileImporter {

  private static final Logger LOGGER = Logger.getLogger(FileImporter.class.getName());

  private MediaItem item = null;

  private volatile boolean needsImport = true;

  /**
   * Tries to import the file into the Menagerie and store it in {@link #item}
   *
   * @param menagerie Menagerie to import into.
   * @return False if the import failed.
   */
  boolean tryImport(File file, Menagerie menagerie, MenagerieSettings settings) {
    if (!needsImport) {
      return true;
    }

    LOGGER.info(() -> "Importing file: " + file);
    synchronized (this) {
      item = menagerie.importFile(file);
    }

    if (item == null) {
      LOGGER.info(() -> "File failed to import: " + file);
      return false;
    }

    LOGGER.info(() -> "Successfully imported file: " + file + "\nWith ID: " + item.getId());
    needsImport = false;

    LOGGER.info("Applying auto-tags to imported item: " + item.getId());
    // Add tags
    if (settings.tagTagme.getValue()) {
      ItemUtil.addTag(menagerie, "tagme", item);
    }
    if (settings.tagImages.getValue() && item.isImage()) {
      ItemUtil.addTag(menagerie, "image", item);
    }
    if (settings.tagVideos.getValue() && item.isVideo()) {
      ItemUtil.addTag(menagerie, "video", item);
    }

    return true;
  }

  MediaItem getItem() {
    return item;
  }
}
