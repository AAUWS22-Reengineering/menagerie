package menagerie.model.menagerie.itemhandler.group;

import menagerie.model.menagerie.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class GroupItemGroupHandlerTest {

  private static final GroupItemGroupHandler groupHandler = new GroupItemGroupHandler();

  @Test
  void testGroupTitle() {
    String title = "My Title";
    GroupItem g = new GroupItem(null, 1, 0, title);

    assertEquals(title, groupHandler.getGroupTitle(g));
  }

  @Test
  void testAddToGroup() {
    int elementCount = 3;
    GroupItem emptyG = ItemTestUtils.getGroupWithNElements(0);
    GroupItem gWithElements = ItemTestUtils.getGroupWithNElements(elementCount);

    groupHandler.addToGroup(gWithElements, emptyG);

    assertEquals(0, gWithElements.getElements().size());
    assertEquals(elementCount, emptyG.getElements().size());
  }
}
