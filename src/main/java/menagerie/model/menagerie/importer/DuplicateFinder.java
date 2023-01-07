package menagerie.model.menagerie.importer;

import menagerie.gui.itemhandler.Items;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.itemhandler.similarity.ItemSimilarity;
import menagerie.settings.MenagerieSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class DuplicateFinder {

  private static final Logger LOGGER = Logger.getLogger(DuplicateFinder.class.getName());

  private volatile boolean needsCheckDuplicate = true;
  private volatile boolean needsCheckSimilar = true;

  private MediaItem duplicateOf = null;
  private List<SimilarPair<MediaItem>> similarTo = null;


  /**
   * Tries to find a duplicate item in the Menagerie and stores the existing duplicate in {@link #duplicateOf}
   *
   * @param menagerie Menagerie to search.
   * @return True if a duplicate exists.
   */
  boolean tryDuplicate(Menagerie menagerie, MediaItem item) {
    if (!needsCheckDuplicate || item.getMD5() == null) {
      return false;
    }

    LOGGER.info("Checking for hash duplicates: " + item.getId());
    for (Item i : menagerie.getItems()) {
      Optional<ItemSimilarity> itemSim = Items.get(ItemSimilarity.class, i);
      if (itemSim.isPresent() && itemSim.get().isExactDuplicate(item, i)) {
        synchronized (this) {
          duplicateOf = (MediaItem) i;
        }
        LOGGER.info("Found hash duplicate, cancelling import: " + item.getId());
        menagerie.deleteItem(item);
        needsCheckDuplicate = false;
        needsCheckSimilar = false;
        return true;
      }
    }

    needsCheckDuplicate = false;
    return false;
  }

  /**
   * Tries to find similar items already imported and stores similar pairs in {@link #similarTo}
   *
   * @param menagerie Menagerie to find similar items in.
   * @param settings  Application settings to use.
   */
  void trySimilar(Menagerie menagerie, MenagerieSettings settings, MediaItem item) {
    if (!needsCheckSimilar || item.getHistogram() == null) {
      return;
    }

    LOGGER.info("Finding similar, existing items: " + item.getId());
    synchronized (this) {
      similarTo = new ArrayList<>();
    }
    final double confidence = settings.duplicatesConfidence.getValue();
    final double confidenceSquare = 1 - (1 - confidence) * (1 - confidence);
    boolean anyMinimallySimilar = false;
    for (Item i : menagerie.getItems()) {
      anyMinimallySimilar = trySimilarItem(confidence, confidenceSquare, item, i);
    }

    if (!anyMinimallySimilar) {
      LOGGER.info("None minimally similar to item: " + item.getId());
      item.setHasNoSimilar(true);
    }

    needsCheckSimilar = false;
  }

  private boolean trySimilarItem(double confidence, double confidenceSquare, MediaItem item,
                                 Item i) {

    boolean anyMinimallySimilar = false;
    Optional<ItemSimilarity> itemSim = Items.get(ItemSimilarity.class, i);
    if (!item.equals(i) && itemSim.isPresent() && itemSim.get().isEligibleForSimCalc(i)) {

      double similarity = itemSim.get().getSimilarity(item, i);

      if (similarity > MediaItem.MIN_CONFIDENCE) {
        anyMinimallySimilar = true;
        if (((MediaItem) i).hasNoSimilar()) {
          ((MediaItem) i).setHasNoSimilar(false);
        }
      }

      if (itemSim.get().isSimilarTo(item, i, confidenceSquare, confidence)) {
        synchronized (this) {
          LOGGER.info("Found similar item (To ID: " + item.getId() + "): " + i.getId());
          final var similarPair = new SimilarPair<>(item, (MediaItem) i, similarity);
          similarTo.add(similarPair);
        }
      }
    }
    return anyMinimallySimilar;
  }

  MediaItem getDuplicateOf() {
    return duplicateOf;
  }

  List<SimilarPair<MediaItem>> getSimilarTo() {
    return similarTo;
  }
}
