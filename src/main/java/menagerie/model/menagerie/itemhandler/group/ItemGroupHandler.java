package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.Item;

public interface ItemGroupHandler {
  void removeFromGroup(Item item);
  String getGroupTitle(Item item);
}
