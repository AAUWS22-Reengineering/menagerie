package menagerie.gui.itemhandler.gridcell;

import javafx.scene.control.Tooltip;
import menagerie.gui.Main;
import menagerie.gui.grid.ItemGridCell;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class MediaItemCellHandler implements ItemCellHandler {
  @Override
  public void initialize(Item item, ItemGridCell igc) {
    MediaItem mediaItem = (MediaItem) item;
    if (mediaItem.isVideo()) {
      igc.getTagView().setImage(ItemGridCell.getVideoTagImage());
      if (!Main.isVlcjLoaded()) {
        igc.getCenterLabel().setText(mediaItem.getFile().getName());
      }
    } else {
      igc.getCenterLabel().setText(null);
    }
    if (mediaItem.isInGroup()) {
      igc.getBottomRightLabel().setText(mediaItem.getPageIndex() + "");
    } else {
      igc.getBottomRightLabel().setText(null);
    }
    if (!mediaItem.isImage() && !mediaItem.isVideo()) {
      igc.getCenterLabel().setText(mediaItem.getFile().getName());
    }
    Tooltip tt = new Tooltip(mediaItem.getFile().getAbsolutePath());
    tt.setWrapText(true);
    igc.setTooltip(tt);
  }

  @Override
  public void cleanUp(Item item, ItemGridCell igc) {
    // no additional cleaning necessary
  }
}
