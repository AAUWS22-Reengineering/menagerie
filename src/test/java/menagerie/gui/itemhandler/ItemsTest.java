package menagerie.gui.itemhandler;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ItemsTest {

  private static final MediaItem mediaItem = ItemTestUtils.getMediaItem();
  private static final GroupItem groupItem = ItemTestUtils.getGroupWithNElements(0);

  @Test
  void testRegister() {
    assertFalse(Items.get(DummyInterface.class, mediaItem).isPresent());
    Items.register(DummyInterface.class, MediaItem.class, new DummyInterfaceImpl1());
    assertTrue(Items.get(DummyInterface.class, mediaItem).isPresent());

    assertNotNull(Items.unregister(DummyInterface.class, MediaItem.class));
    assertFalse(Items.get(DummyInterface.class, mediaItem).isPresent());
  }

  @Test
  void testRegisterMultipleDifferentItemClass() {
    Items.register(DummyInterface.class, MediaItem.class, new DummyInterfaceImpl1());
    Items.register(DummyInterface.class, GroupItem.class, new DummyInterfaceImpl2());

    assertTrue(Items.get(DummyInterface.class, mediaItem).isPresent());
    assertTrue(Items.get(DummyInterface.class, groupItem).isPresent());

    Items.unregister(DummyInterface.class, MediaItem.class);
    Items.unregister(DummyInterface.class, GroupItem.class);
  }

  @Test
  void testRegisterMultipleSameItemClass() {
    Items.register(DummyInterface.class, MediaItem.class, new DummyInterfaceImpl1());

    assertThrows(IllegalStateException.class, () ->
        Items.register(DummyInterface.class, MediaItem.class, new DummyInterfaceImpl2()));

    Items.unregister(DummyInterface.class, MediaItem.class);
  }

  @Test
  void testGetInheritanceHierarchy() {
    Items.register(DummyInterface.class, Item.class, new DummyInterfaceImpl1());

    assertTrue(Items.get(DummyInterface.class, mediaItem).isPresent());

    Items.unregister(DummyInterface.class, Item.class);
  }
}
