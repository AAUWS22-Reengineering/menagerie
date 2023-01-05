package menagerie.model.menagerie.itemhandler.similarity;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class MediaItemSimilarity implements ItemSimilarity {
  @Override
  public boolean isEligibleForSimCalc(Item item) {
    return item != null && ((MediaItem) item).getHistogram() != null;
  }

  @Override
  public boolean hasNoSimilar(Item item) {
    return ((MediaItem) item).hasNoSimilar();
  }

  @Override
  public void setNoSimilarity(Item item, boolean noSimFlag) {
    ((MediaItem) item).setHasNoSimilar(noSimFlag);
  }

  /**
   * Determine if MediaItem base is similar to Item target.
   * Only supports comparison to other MediaItem (i.e. target has to be of type MediaItem too)
   */
  @Override
  public boolean isSimilarTo(Item base, Item target, double confidenceSquare) {
    MediaItem mediaItemBase = (MediaItem) base;
    if (target instanceof MediaItem mediaItemTarget) {
      double similarity = mediaItemBase.getSimilarityTo(mediaItemTarget);
      if (similarity >= confidenceSquare ||
          ((mediaItemBase.getHistogram().isColorful() || mediaItemTarget.getHistogram().isColorful()) &&
              similarity > MediaItem.MIN_CONFIDENCE)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isExactDuplicate(Item base, Item target) {
    MediaItem mediaBase = (MediaItem) base;
    if (target instanceof MediaItem mediaTarget) {
      return !mediaBase.equals(mediaTarget) && mediaBase.getMD5() != null &&
          mediaBase.getMD5().equalsIgnoreCase(mediaTarget.getMD5());
    }
    return false;
  }
}
