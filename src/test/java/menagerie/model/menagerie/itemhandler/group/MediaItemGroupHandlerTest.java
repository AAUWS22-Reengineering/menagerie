package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MediaItemGroupHandlerTest {

  private static final MediaItemGroupHandler mediaHandler = new MediaItemGroupHandler();

  @Test
  void testAddToGroup() {
    GroupItem group = new GroupItem(null, 1, 0, "title");
    MediaItem m = new TestMediaItem(null, 1, 0, null);

    mediaHandler.addToGroup(m, group);

    assertEquals(1, group.getElements().size());
  }

  @Test
  void testRemoveFromGroup() {
    GroupItem group = ItemTestUtils.getGroupWithNElements(2);

    assertEquals(2, group.getElements().size());
    mediaHandler.removeFromGroup(group.getElements().get(0));
    assertEquals(1, group.getElements().size());
  }
}
