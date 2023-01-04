package menagerie.gui.itemhandler.opener;

import menagerie.gui.MainController;
import menagerie.model.menagerie.Item;

public interface ItemOpener {
  void open(Item item, MainController controller);
}
