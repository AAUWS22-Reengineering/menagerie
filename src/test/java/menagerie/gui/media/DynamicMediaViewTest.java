package menagerie.gui.media;

import menagerie.gui.UITest;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DynamicMediaViewTest extends UITest {

  @Test
  void testPreviewMediaItem() {
    MediaItem m = new MediaItem(null, 1, 1, ItemTestUtils.WHITE_IMAGE_FILE);
    DynamicMediaView dynamicMediaView = new DynamicMediaView();

    assertTrue(dynamicMediaView.preview(m));
    assertEquals(m.getImage(), dynamicMediaView.getImageView().getTrueImage());
  }

  @Test
  void testPreviewGroupItem() {
    MediaItem m1 = new MediaItem(null, 1, 1, ItemTestUtils.WHITE_IMAGE_FILE);
    MediaItem m2 = new MediaItem(null, 2, 1, ItemTestUtils.BLACK_IMAGE_FILE);
    MediaItem m3 = new MediaItem(null, 3, 1, ItemTestUtils.GREY_IMAGE_FILE);
    GroupItem g = ItemTestUtils.getGroup(m1, m2, m3);

    DynamicMediaView dynamicMediaView = new DynamicMediaView();

    assertTrue(dynamicMediaView.preview(g));
    // check if image of first element is previewed
    assertEquals(m1.getImage(), dynamicMediaView.getImageView().getTrueImage());
  }
}
