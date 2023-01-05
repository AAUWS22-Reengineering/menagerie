package menagerie.model.menagerie;

import java.io.File;

public class TestMediaItem extends MediaItem {
  public TestMediaItem(Menagerie menagerie, int id, long dateAdded, File file) {
    super(menagerie, id, dateAdded, file);
  }

  public void setMenagerie(Menagerie m) {
    this.menagerie = m;
  }
}
