package menagerie.gui.itemhandler.properties;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

public interface ItemProperties {
  boolean isMedia(Item item);
  boolean isGroup(Item item);
  boolean isInGroup(Item item);
  GroupItem getParentGroup(Item item);
}
