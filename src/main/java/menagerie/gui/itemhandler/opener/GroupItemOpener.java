package menagerie.gui.itemhandler.opener;

import menagerie.gui.MainController;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

public class GroupItemOpener implements ItemOpener {
  @Override
  public void open(Item item, MainController controller) {
    controller.explorerOpenGroup((GroupItem) item);
  }

  @Override
  public void openInExplorer(Item item) {
    // not supported
  }
}
