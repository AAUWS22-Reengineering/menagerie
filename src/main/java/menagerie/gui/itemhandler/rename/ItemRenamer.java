package menagerie.gui.itemhandler.rename;

import menagerie.gui.MainController;
import menagerie.model.menagerie.Item;

public interface ItemRenamer {
  void rename(Item item, MainController mainController);
}
