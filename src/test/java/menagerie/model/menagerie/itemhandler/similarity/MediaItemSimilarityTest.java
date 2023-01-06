package menagerie.model.menagerie.itemhandler.similarity;

import menagerie.gui.UITest;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;

public class MediaItemSimilarityTest extends UITest {

  private static final MediaItemSimilarity itemSim = new MediaItemSimilarity();

  private static MediaItem getInitializedMediaItem(int id, File file) {
    MediaItem m = new MediaItem(null, id, 1, file);
    m.initializeHistogram();
    m.initializeMD5();
    return m;
  }

  @Test
  void testIsEligibleForSimCalc() {
    MediaItem m1 = new MediaItem(null, 1, 1, ItemTestUtils.WHITE_IMAGE_FILE);

    assertNull(m1.getHistogram());
    assertFalse(itemSim.isEligibleForSimCalc(m1));

    m1.initializeHistogram();
    assertNotNull(m1.getHistogram());
    assertTrue(itemSim.isEligibleForSimCalc(m1));
  }

  @Test
  void testExactDuplicate() {
    MediaItem m1 = getInitializedMediaItem(1, ItemTestUtils.GREY_IMAGE_FILE);
    MediaItem m2 = getInitializedMediaItem(2, ItemTestUtils.GREY_DUPE_IMAGE_FILE);
    MediaItem m3 = getInitializedMediaItem(3, ItemTestUtils.BLACK_IMAGE_FILE);

    assertTrue(itemSim.isExactDuplicate(m1, m2));
    assertFalse(itemSim.isExactDuplicate(m1, m3));
  }

  @Test
  void testSimilarity() {
    double confidence = MediaItem.MIN_CONFIDENCE;
    double confidenceSquared = confidence * confidence;

    MediaItem m1 = getInitializedMediaItem(1, ItemTestUtils.GREY_IMAGE_FILE);
    MediaItem m2 = getInitializedMediaItem(2, ItemTestUtils.GREY_DUPE_IMAGE_FILE);
    MediaItem m3 = getInitializedMediaItem(3, ItemTestUtils.BLACK_IMAGE_FILE);

    assertTrue(itemSim.isSimilarTo(m1, m2, confidenceSquared, confidence));
    assertFalse(itemSim.isSimilarTo(m1, m3, confidenceSquared, confidence));

    assertEquals(1, itemSim.getSimilarity(m1, m2));
    assertThat("timestamp",
        itemSim.getSimilarity(m1, m3),
        lessThan(1d));
  }
}
