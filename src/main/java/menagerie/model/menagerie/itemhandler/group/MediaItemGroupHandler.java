package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class MediaItemGroupHandler implements ItemGroupHandler {
  @Override
  public void removeFromGroup(Item item) {
    MediaItem mediaItem = (MediaItem) item;
    if (mediaItem != null && mediaItem.isInGroup()) {
      mediaItem.getGroup().removeItem(mediaItem);
    }
  }

  @Override
  public String getGroupTitle(Item item) {
    return null;
  }

  @Override
  public void addToGroup(Item item, GroupItem group) {
    group.addItem((MediaItem) item);
  }
}
