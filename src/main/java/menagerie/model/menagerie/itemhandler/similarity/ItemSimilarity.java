package menagerie.model.menagerie.itemhandler.similarity;

import menagerie.model.menagerie.Item;

public interface ItemSimilarity {
  boolean isEligibleForSimCalc(Item item);
  boolean hasNoSimilarity(Item item);
  void setNoSimilarity(Item item, boolean noSimFlag);
  boolean isSimilarTo(Item base, Item target, double confidenceSquare);
}
