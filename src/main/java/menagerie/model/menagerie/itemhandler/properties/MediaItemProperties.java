package menagerie.model.menagerie.itemhandler.properties;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.io.File;

public class MediaItemProperties implements ItemProperties {

  @Override
  public boolean isMedia(Item item) {
    return true;
  }

  @Override
  public boolean isGroup(Item item) {
    return false;
  }

  @Override
  public boolean isInGroup(Item item) {
    return ((MediaItem) item).isInGroup();
  }

  @Override
  public GroupItem getParentGroup(Item item) {
    return ((MediaItem) item).getGroup();
  }

  @Override
  public boolean isFileBased(Item item) {
    return true;
  }

  @Override
  public File getFile(Item item) {
    return ((MediaItem) item).getFile();
  }
}
