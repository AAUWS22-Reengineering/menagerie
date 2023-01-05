package menagerie.model.menagerie.itemhandler.similarity;

import menagerie.model.menagerie.Item;

import java.util.List;

public interface ItemSimilarity {
  boolean isEligibleForSimCalc(Item item);
  boolean hasNoSimilar(Item item);
  void setNoSimilarity(Item item, boolean noSimFlag);
  boolean isSimilarTo(Item base, Item target, double confidenceSquare);
  boolean isExactDuplicate(Item base, Item target);
}
