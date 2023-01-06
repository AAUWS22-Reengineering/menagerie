package menagerie.gui;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ItemInfoBoxTest extends UITest {

  private final File img = ItemTestUtils.WHITE_IMAGE_FILE;
  private final int imgHeight = 22;
  private final int imgWidth = 21;

  @Test
  void testSetMediaItem() throws InterruptedException {
    TestItemInfoBox infoBox = new TestItemInfoBox();
    MediaItem m = new MediaItem(null, 1, 1, img);

    infoBox.setItem(m);
    // wait for rendering to be done
    Thread.sleep(100);
    assertEquals(imgWidth + "x" + imgHeight, infoBox.getResolutionLabel().getText());
    assertNotEquals(TestItemInfoBox.DEFAULT_FILESIZE_TEXT, infoBox.getFileSizeLabel().getText());
  }

  @Test
  void testSetGroupItem() throws InterruptedException {
    TestItemInfoBox infoBox = new TestItemInfoBox();
    GroupItem g = ItemTestUtils.getGroup();

    infoBox.setItem(g);
    // wait for rendering to be done
    Thread.sleep(100);
    assertEquals(TestItemInfoBox.DEFAULT_RESOLUTION_TEXT, infoBox.getResolutionLabel().getText());
    assertEquals(TestItemInfoBox.DEFAULT_FILESIZE_TEXT, infoBox.getFileSizeLabel().getText());
  }
}
