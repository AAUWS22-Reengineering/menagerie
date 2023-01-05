package menagerie.gui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import menagerie.gui.itemhandler.Items;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.menagerie.itemhandler.group.ItemGroupHandler;
import menagerie.model.menagerie.itemhandler.properties.ItemProperties;

public class ItemUtil {

  private ItemUtil() {
  }

  public static void removeFromGroup(List<Item> selected, ContextMenu cm) {
    MenuItem removeFromGroup = new MenuItem("Remove from group");
    removeFromGroup.setOnAction(event -> selected.forEach(item -> {
      Items.get(ItemGroupHandler.class, item).ifPresent(itemGroupHandler -> itemGroupHandler.removeFromGroup(item));
    }));
    cm.getItems().add(removeFromGroup);
  }

  public static void addGroupElements(List<Item> items) {
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      Items.get(ItemProperties.class, item).ifPresent(itemProps -> {
        if (itemProps.isGroup(item)) {
          items.addAll(itemProps.getItems(item));
        }
      });
    }
  }

  public static List<Item> flattenGroups(List<Item> items, boolean reversed) {
    ArrayList<Item> itemsFlat = new ArrayList<>();
    for (Item item : items) {
      Items.get(ItemProperties.class, item).ifPresent(itemProps -> itemsFlat.addAll(itemProps.getItems(item)));
    }
    if (reversed) {
      Collections.reverse(itemsFlat);
    }
    return itemsFlat;
  }

  // REENG: Probably unused - not worth the refactoring effort right now
  public static List<MediaItem> flattenGroups(List<Item> selected) {
    List<MediaItem> items = new ArrayList<>();
    for (Item item : selected) {
      if (item instanceof MediaItem) {
        items.add((MediaItem) item);
      } else if (item instanceof GroupItem) {
        for (MediaItem element : ((GroupItem) item).getElements()) {
          final String name = element.getFile().getName().toLowerCase();
          if (FileExplorer.hasAllowedFileEnding(name)) {
            items.add(element);
          }
        }
      }
    }
    return items;
  }

  public static List<GroupItem> getGroupItems(List<Item> items) {
    List<GroupItem> groups = new ArrayList<>();
    items.forEach(item -> {
      if (item instanceof GroupItem) {
        groups.add((GroupItem) item);
      }
    });
    return groups;
  }

  public static String getFirstGroupTitle(List<Item> toGroup) {
    String title = null;
    for (Item item : toGroup) {
      Optional<ItemGroupHandler> itemGroupHandler = Items.get(ItemGroupHandler.class, item);
      if (itemGroupHandler.isPresent()) {
        String t = itemGroupHandler.get().getGroupTitle(item);
        if (t != null) {
          title = t;
          break;
        }
      }
    }
    return title;
  }

  public static void addTag(Menagerie menagerie, String name, Item item) {
    Tag tag = menagerie.getTagByName(name);
    if (tag == null) {
      tag = menagerie.createTag(name);
    }
    item.addTag(tag);
  }
}
