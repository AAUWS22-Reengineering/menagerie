package menagerie.model.menagerie.itemhandler.file;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.io.File;
import java.nio.file.Path;

public class MediaItemFileHandler implements ItemFileHandler {
  @Override
  public void move(Path path, Item item) {
    MediaItem mediaItem = (MediaItem) item;
    File target = path.resolve(mediaItem.getFile().getName()).toFile();
    mediaItem.moveFile(target);
  }
}
