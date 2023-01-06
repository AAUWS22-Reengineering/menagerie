package menagerie.gui.grid;

import javafx.beans.property.StringProperty;
import menagerie.gui.UITest;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.TestGroupItem;
import org.junit.jupiter.api.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ItemGridCellTest extends UITest {

  @Test
  void testUpdateMediaItem() throws InterruptedException {
    File file = ItemTestUtils.WHITE_IMAGE_FILE;
    ItemGridCell gridCell = new ItemGridCell();
    MediaItem m = new MediaItem(null, 1, 1, file);

    gridCell.updateItem(m, false);
    // wait for rendering to be done
    Thread.sleep(100);

    assertEquals(file.getAbsolutePath(), gridCell.getTooltip().getText());
  }

  @Test
  void testUpdateGroupItem() throws InterruptedException {
    String title = "MyGroupTitle";
    int elementCount = 3;
    ItemGridCell gridCell = new ItemGridCell();
    GroupItem g = ItemTestUtils.getGroupWithNElements(3);
    g.setTitle(title);

    gridCell.updateItem(g, false);
    // wait for rendering to be done
    Thread.sleep(100);

    assertEquals(title, gridCell.getTooltip().getText());
    assertEquals(String.valueOf(elementCount), gridCell.getBottomRightLabel().getText());
  }

  @Test
  void testCleanUpGroup() throws InterruptedException {
    ItemGridCell gridCell = new ItemGridCell();
    TestGroupItem g = new TestGroupItem(null, 1, 1, "");

    StringProperty titlePropMock = mock(StringProperty.class);
    g.setTitleProperty(titlePropMock);

    // set group
    gridCell.updateItem(g, false);
    // replace with empty
    gridCell.updateItem(null, true);
    // wait for rendering to be done
    Thread.sleep(100);

    // verify that listeners were successfully removed
    verify(titlePropMock, times(1)).removeListener(gridCell.getGroupTitleListener());
  }
}
