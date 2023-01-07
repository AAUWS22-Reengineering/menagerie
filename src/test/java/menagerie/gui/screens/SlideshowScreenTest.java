package menagerie.gui.screens;

import menagerie.gui.UITest;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.awaitility.Awaitility.await;

public class SlideshowScreenTest extends UITest {

  private final MediaItem m1 = new MediaItem(null, 1, 1, ItemTestUtils.GREY_IMAGE_FILE);
  private final MediaItem m2 = new MediaItem(null, 1, 1, ItemTestUtils.WHITE_IMAGE_FILE);

  private List<Item> getItems() {
    ArrayList<Item> l = new ArrayList<>();
    l.add(m1);
    l.add(m2);
    return l;
  }

  @Test
  void testPreviewInSlideshow() {
    SlideshowScreen screen = new SlideshowScreen(null);
    ScreenPane pane = new ScreenPane();
    screen.open(pane, null, getItems());

    await()
        .atMost(Duration.ofMillis(UITest.MAX_WAIT_TIME))
        .until(() -> screen.mediaView.getImageView().getTrueImage() != null);

    assertEquals(m1.getImage(), screen.mediaView.getImageView().getTrueImage());
    // click next button
    screen.getRightButton().fire();

    await()
        .atMost(Duration.ofMillis(UITest.MAX_WAIT_TIME))
        .until(() -> screen.mediaView.getImageView().getTrueImage() != m1.getImage());

    assertEquals(m2.getImage(), screen.mediaView.getImageView().getTrueImage());
  }
}
