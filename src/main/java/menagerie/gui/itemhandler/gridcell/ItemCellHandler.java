package menagerie.gui.itemhandler.gridcell;

import menagerie.gui.grid.ItemGridCell;
import menagerie.model.menagerie.Item;

public interface ItemCellHandler {
  /**
   * Initializes/displays an item in this cell
   *
   * @param item Group to display
   */
  void initialize(Item item, ItemGridCell igc);
  void cleanUp(Item item, ItemGridCell igc);
}
