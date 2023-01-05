package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupItemGroupHandler implements ItemGroupHandler {
  @Override
  public void removeFromGroup(Item item) {
    // nothing
  }

  @Override
  public String getGroupTitle(Item item) {
    return ((GroupItem) item).getTitle();
  }

  @Override
  public void addToGroup(Item item, GroupItem group) {
    List<MediaItem> e = new ArrayList<>(((GroupItem) item).getElements());
    item.getTags().forEach(group::addTag);
    item.getMenagerie().forgetItem(item);
    e.forEach(group::addItem);
  }

  @Override
  public void sortItems(Item item) {
    GroupItem groupItem = (GroupItem) item;
    groupItem.getElements().sort(Comparator.comparingInt(MediaItem::getPageIndex));
  }
}
