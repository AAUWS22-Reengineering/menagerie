package menagerie.model.menagerie.itemhandler.search;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

import java.util.List;

public class GroupItemSearch implements ItemSearch {
  @Override
  public boolean hasMissingMD5(Item item) {
    return false;
  }

  @Override
  public boolean hasMissingFile(Item item) {
    return false;
  }

  @Override
  public boolean hasMissingHistogram(Item item) {
    return false;
  }

  @Override
  public boolean titleContains(Item item, List<String> words) {
    final String title = ((GroupItem) item).getTitle().toLowerCase();

    for (String word : words) {
      if (!title.contains(word)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isMedia(Item item) {
    return false;
  }

  @Override
  public boolean isImage(Item item) {
    return false;
  }

  @Override
  public boolean isVideo(Item item) {
    return false;
  }

  @Override
  public boolean isGroup(Item item) {
    return true;
  }
}
