package menagerie.model.menagerie.itemhandler.search;

import menagerie.model.menagerie.Item;

import java.util.List;

public interface ItemSearch {
  boolean hasMissingMD5(Item item);
  boolean hasMissingFile(Item item);
  boolean hasMissingHistogram(Item item);
  boolean titleContains(Item item, List<String> words);
  boolean isMedia(Item item);
  boolean isImage(Item item);
  boolean isVideo(Item item);
  boolean isGroup(Item item);
}
