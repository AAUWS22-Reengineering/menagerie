package menagerie.gui.itemhandler.gridviewselector;

import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import menagerie.gui.grid.ItemGridView;
import menagerie.gui.util.FileExplorer;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class MediaItemGridViewSelector implements ItemGridViewSelector {
  @Override
  public void select(Item item, ItemGridView itemGridView, MouseEvent event) {
    if (!itemGridView.isSelected(item)) {
      itemGridView.select(item, event.isControlDown(), event.isShiftDown());
    }
  }

  @Override
  public boolean doDragAndDrop(Item item, Dragboard db, ItemGridView itemGridView) {
    String filename = ((MediaItem) item).getFile().getName().toLowerCase();
    if (FileExplorer.hasAllowedFileEnding(filename)) {
      if (item.getThumbnail().isLoaded()) {
        db.setDragView(item.getThumbnail().getImage());
        return true;
      }
    }
    return false;
  }
}
