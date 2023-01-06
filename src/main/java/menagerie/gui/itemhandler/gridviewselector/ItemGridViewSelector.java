package menagerie.gui.itemhandler.gridviewselector;

import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import menagerie.gui.grid.ItemGridView;
import menagerie.model.menagerie.Item;

public interface ItemGridViewSelector {
  void select(Item item, ItemGridView itemGridView, MouseEvent event);
  boolean doDragAndDrop(Item item, Dragboard db);
}
