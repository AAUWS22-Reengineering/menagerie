package menagerie.model.menagerie.itemhandler.file;

import menagerie.gui.screens.move.FileMoveNode;
import menagerie.model.menagerie.Item;

import java.nio.file.Path;

public interface ItemFileHandler {
  void move(Path path, Item item);
}
