package menagerie.model.menagerie.importer;

import java.io.File;
import java.util.logging.Logger;
import menagerie.model.menagerie.MediaItem;

public class ItemHasher {

  private static final Logger LOGGER = Logger.getLogger(ItemHasher.class.getName());

  private volatile boolean needsHash = true;

  private volatile boolean needsHist = true;

  /**
   * Tries to construct and store an MD5 hash and a histogram for the item.
   */
  void tryHashHist(MediaItem item, File file) {
    if (needsHash) {
      LOGGER.info(() -> "Hashing file (ID: " + item.getId() + "): " + file);
      item.initializeMD5();
      needsHash = false;
    }
    if (needsHist) {
      if (item.initializeHistogram()) {
        LOGGER.info(
            () -> "Generated image histogram from file (ID: " + item.getId() + "): " + file);
      }
      needsHist = false;
    }
  }


}
