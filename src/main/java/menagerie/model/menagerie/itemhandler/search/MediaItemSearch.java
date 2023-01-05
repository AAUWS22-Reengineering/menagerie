package menagerie.model.menagerie.itemhandler.search;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.util.List;

public class MediaItemSearch implements ItemSearch {
  @Override
  public boolean hasMissingMD5(Item item) {
    return ((MediaItem) item).getMD5() == null;
  }

  @Override
  public boolean hasMissingFile(Item item) {
    return ((MediaItem) item).getFile() == null || !((MediaItem) item).getFile().exists();
  }

  @Override
  public boolean hasMissingHistogram(Item item) {
    return ((MediaItem) item).getHistogram() == null;
  }

  @Override
  public boolean titleContains(Item item, List<String> words) {
    return false;
  }

  @Override
  public boolean isMedia(Item item) {
    return true;
  }

  @Override
  public boolean isImage(Item item) {
    return ((MediaItem) item).isImage();
  }

  @Override
  public boolean isVideo(Item item) {
    return ((MediaItem) item).isVideo();
  }

  @Override
  public boolean isGroup(Item item) {
    return false;
  }
}
