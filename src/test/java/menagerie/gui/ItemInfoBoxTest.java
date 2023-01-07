package menagerie.gui;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;

import static junit.framework.Assert.assertEquals;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ItemInfoBoxTest extends UITest {

  private final File img = ItemTestUtils.WHITE_IMAGE_FILE;
  private final int imgHeight = 22;
  private final int imgWidth = 21;

  @Test
  void testSetMediaItem() {
    TestItemInfoBox infoBox = new TestItemInfoBox();
    MediaItem m = new MediaItem(null, 1, 1, img);

    infoBox.setItem(m);
    // wait for rendering to be done
    await()
        .atMost(Duration.ofMillis(UITest.MAX_WAIT_TIME))
        .until(() -> infoBox.getResolutionLabel().getText().equals(imgWidth + "x" + imgHeight));

    assertEquals(imgWidth + "x" + imgHeight, infoBox.getResolutionLabel().getText());
    assertNotEquals(TestItemInfoBox.DEFAULT_FILESIZE_TEXT, infoBox.getFileSizeLabel().getText());
  }

  @Test
  void testSetGroupItem() {
    TestItemInfoBox infoBox = new TestItemInfoBox();
    GroupItem g = ItemTestUtils.getGroup();

    infoBox.setItem(g);
    // wait for rendering to be done
    await()
        .atMost(Duration.ofMillis(UITest.MAX_WAIT_TIME))
        .until(() -> infoBox.getResolutionLabel().getText().equals(TestItemInfoBox.DEFAULT_RESOLUTION_TEXT));

    assertEquals(TestItemInfoBox.DEFAULT_RESOLUTION_TEXT, infoBox.getResolutionLabel().getText());
    assertEquals(TestItemInfoBox.DEFAULT_FILESIZE_TEXT, infoBox.getFileSizeLabel().getText());
  }
}
