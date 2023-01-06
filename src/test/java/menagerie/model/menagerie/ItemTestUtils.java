package menagerie.model.menagerie;

import java.io.File;

import static org.mockito.Mockito.mock;

public class ItemTestUtils {

  public static final File WHITE_IMAGE_FILE = new File("target/test-classes/white.png");
  public static final File BLACK_IMAGE_FILE = new File("target/test-classes/black.png");
  public static final File GREY_IMAGE_FILE = new File("target/test-classes/grey.png");
  public static final File GREY_DUPE_IMAGE_FILE = new File("target/test-classes/grey_duplicate.png");

  private ItemTestUtils() {
  }

  public static GroupItem getGroup() {
    return getGroupWithNElements(0);
  }

  public static GroupItem getGroupWithNElements(int elementCount) {
    Menagerie menagerieMock = mock(Menagerie.class);
    TestGroupItem g = new TestGroupItem(null, 1, 0, "title");
    g.setMenagerie(menagerieMock);

    for (int i=0; i<elementCount; i++) {
      TestMediaItem m = new TestMediaItem(null, i, 0, null);
      m.setMenagerie(menagerieMock);
      g.addItem(m);
    }
    return g;
  }

  public static MediaItem getMediaItem() {
    TestMediaItem m = new TestMediaItem(null, 1, 0, null);
    m.setMenagerie(mock(Menagerie.class));
    return m;
  }
}
