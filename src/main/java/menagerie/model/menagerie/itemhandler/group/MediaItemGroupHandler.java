package menagerie.model.menagerie.itemhandler.group;

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
}
