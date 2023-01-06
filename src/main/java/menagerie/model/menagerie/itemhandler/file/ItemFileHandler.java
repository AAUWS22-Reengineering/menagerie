package menagerie.model.menagerie.itemhandler.file;

import menagerie.model.menagerie.Item;

import java.nio.file.Path;

public interface ItemFileHandler {
  void move(Path path, Item item);
}
