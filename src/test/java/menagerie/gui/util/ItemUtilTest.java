package menagerie.gui.util;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.ItemTestUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class ItemUtilTest {
  @Test
  void testGetFirstGroupTitle() {
    String firstGroupTitle = "t1";

    List<Item> items = new ArrayList<>();
    items.add(ItemTestUtils.getMediaItem());
    items.add(ItemTestUtils.getGroup(firstGroupTitle));
    items.add(ItemTestUtils.getGroup("someOtherTitle"));
    items.add(ItemTestUtils.getMediaItem());

    assertEquals(firstGroupTitle, ItemUtil.getFirstGroupTitle(items));
  }

  @Test
  void testAddGroupElements() {
    int elementCount = 3;
    List<Item> items = new ArrayList<>();
    items.add(ItemTestUtils.getMediaItem());
    items.add(ItemTestUtils.getGroupWithNElements(elementCount));

    int countBefore = items.size();
    ItemUtil.addGroupElements(items);
    assertEquals(countBefore+elementCount, items.size());
  }

  @Test
  void testFlattenGroups() {
    int elementCount = 3;
    int mediaItemCount = 2;
    List<Item> items = new ArrayList<>();
    for (int i=0; i<mediaItemCount; i++) {
      items.add(ItemTestUtils.getMediaItem());
    }
    items.add(ItemTestUtils.getGroupWithNElements(elementCount));

    assertEquals(elementCount+mediaItemCount, ItemUtil.flattenGroups(items, false).size());
  }
}
