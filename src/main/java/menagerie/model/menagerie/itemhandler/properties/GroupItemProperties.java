package menagerie.model.menagerie.itemhandler.properties;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

import java.io.File;

public class GroupItemProperties implements ItemProperties {

  @Override
  public boolean isMedia(Item item) {
    return false;
  }

  @Override
  public boolean isGroup(Item item) {
    return true;
  }

  @Override
  public boolean isInGroup(Item item) {
    return false;
  }

  @Override
  public GroupItem getParentGroup(Item item) {
    return null;
  }

  @Override
  public boolean isFileBased(Item item) {
    return false;
  }

  @Override
  public File getFile(Item item) {
    return null;
  }
}
