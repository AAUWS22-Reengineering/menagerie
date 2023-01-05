package menagerie.model.menagerie.itemhandler.properties;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

import java.io.File;

public interface ItemProperties {
  boolean isMedia(Item item);
  boolean isGroup(Item item);
  boolean isInGroup(Item item);
  int getItemCount(Item item);
  GroupItem getParentGroup(Item item);
  boolean isFileBased(Item item);
  File getFile(Item item);
}
