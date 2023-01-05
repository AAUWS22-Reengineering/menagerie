package menagerie.model.menagerie;

import static org.mockito.Mockito.mock;

public class ItemTestUtils {
  private ItemTestUtils() {
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
