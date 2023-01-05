package menagerie.model.menagerie.itemhandler.similarity;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class MediaItemSimilarityTest {

  private static final File WHITE_IMAGE_FILE = new File("target/test-classes/white.png");
  private static final File BLACK_IMAGE_FILE = new File("target/test-classes/black.png");
  private static final File GREY_IMAGE_FILE = new File("target/test-classes/grey.png");
  private static final File GREY_DUPE_IMAGE_FILE = new File("target/test-classes/grey_duplicate.png");

  private static final MediaItemSimilarity itemSim = new MediaItemSimilarity();

  private static MediaItem getInitializedMediaItem(int id, File file) {
    MediaItem m = new MediaItem(null, id, 1, file);
    m.initializeHistogram();
    m.initializeMD5();
    return m;
  }

  @BeforeAll
  static void setupHeadless() throws Exception {
        /* Set "headless" property to true to enable headless testing (using -Dheadless=true)
           PLEASE NOTE:
           Headless tests must be run at a MAX resolution of 1280x800.
           Higher resolutions cause buffer overflows */
    if (Boolean.getBoolean("headless")) {
      System.setProperty("testfx.robot", "glass");
      System.setProperty("testfx.headless", "true");
      System.setProperty("prism.order", "sw");
      System.setProperty("prism.text", "t2k");
      System.setProperty("java.awt.headless", "true");
    }
  }

  @Test
  void testIsEligibleForSimCalc() {
    MediaItem m1 = new MediaItem(null, 1, 1, WHITE_IMAGE_FILE);

    assertNull(m1.getHistogram());
    assertFalse(itemSim.isEligibleForSimCalc(m1));

    m1.initializeHistogram();
    assertNotNull(m1.getHistogram());
    assertTrue(itemSim.isEligibleForSimCalc(m1));
  }

  @Test
  void testExactDuplicate() {
    MediaItem m1 = getInitializedMediaItem(1, GREY_IMAGE_FILE);
    MediaItem m2 = getInitializedMediaItem(2, GREY_DUPE_IMAGE_FILE);
    MediaItem m3 = getInitializedMediaItem(3, BLACK_IMAGE_FILE);

    assertTrue(itemSim.isExactDuplicate(m1, m2));
    assertFalse(itemSim.isExactDuplicate(m1, m3));
  }

  @Test
  void testSimilarity() {
    double confidence = MediaItem.MIN_CONFIDENCE;
    double confidenceSquared = confidence * confidence;

    MediaItem m1 = getInitializedMediaItem(1, GREY_IMAGE_FILE);
    MediaItem m2 = getInitializedMediaItem(2, GREY_DUPE_IMAGE_FILE);
    MediaItem m3 = getInitializedMediaItem(3, BLACK_IMAGE_FILE);

    assertTrue(itemSim.isSimilarTo(m1, m2, confidenceSquared, confidence));
    assertFalse(itemSim.isSimilarTo(m1, m3, confidenceSquared, confidence));

    assertEquals(1, itemSim.getSimilarity(m1, m2));
    assertThat("timestamp",
        itemSim.getSimilarity(m1, m3),
        lessThan(1d));
  }
}
