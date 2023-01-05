package menagerie.gui.itemhandler.gridviewselector;

import javafx.scene.input.MouseEvent;
import menagerie.gui.grid.ItemGridView;
import menagerie.model.menagerie.Item;

public class MediaItemGridViewSelector implements ItemGridViewSelector {
  @Override
  public void select(Item item, ItemGridView itemGridView, MouseEvent event) {
    if (!itemGridView.isSelected(item)) {
      itemGridView.select(item, event.isControlDown(), event.isShiftDown());
    }
  }
}
