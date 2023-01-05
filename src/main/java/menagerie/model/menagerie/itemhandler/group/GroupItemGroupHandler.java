package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class GroupItemGroupHandler implements ItemGroupHandler {
  @Override
  public void removeFromGroup(Item item) {
    // nothing
  }

  @Override
  public String getGroupTitle(Item item) {
    return ((GroupItem) item).getTitle();
  }
}
