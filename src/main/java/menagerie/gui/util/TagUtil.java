package menagerie.gui.util;

import menagerie.gui.grid.ItemGridView;
import menagerie.gui.taglist.TagListCell;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Tag;

import java.util.*;

public class TagUtil {

  private TagUtil() {
  }

  public static List<String> getTop8Tags(String prefix, boolean negative, List<Tag> tags) {
    List<String> results = new ArrayList<>();
    tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
    for (Tag tag : tags) {
      if (tag.getName().toLowerCase().startsWith(prefix)) {
        if (negative) {
          results.add("-" + tag.getName());
        } else {
          results.add(tag.getName());
        }
      }

      if (results.size() >= 8) {
        break;
      }
    }
    return results;
  }

  public static Map<Item, List<Tag>> removeTags(TagListCell c, ItemGridView itemGridView) {
    Map<Item, List<Tag>> removed = new HashMap<>();
    itemGridView.getSelected().forEach(item -> {
      if (item.removeTag(c.getItem())) {
        removed.computeIfAbsent(item, k -> new ArrayList<>()).add(c.getItem());
      }
    });
    return removed;
  }

  public static Map<Item, List<Tag>> addTags(TagListCell c, ItemGridView itemGridView) {
    Map<Item, List<Tag>> added = new HashMap<>();
    itemGridView.getSelected().forEach(item -> {
      if (item.addTag(c.getItem())) {
        added.computeIfAbsent(item, k -> new ArrayList<>()).add(c.getItem());
      }
    });
    return added;
  }

  public static Comparator<Tag> getColorComparator() {
    return (o1, o2) -> {
      if (o1.getColor() == null) {
        if (o2.getColor() == null) {
          return 0;
        } else {
          return 1;
        }
      } else {
        if (o2.getColor() == null) {
          return -1;
        } else {
          return o1.getColor().compareTo(o2.getColor());
        }
      }
    };
  }
}
