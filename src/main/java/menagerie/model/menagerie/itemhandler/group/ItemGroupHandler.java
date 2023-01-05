package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

public interface ItemGroupHandler {
  void removeFromGroup(Item item);
  String getGroupTitle(Item item);
  void addToGroup(Item item, GroupItem group);
  void sortItems(Item item);
}
