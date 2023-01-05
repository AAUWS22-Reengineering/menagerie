package menagerie.gui.itemhandler.rename;

import menagerie.gui.MainController;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

public class GroupItemRenamer implements ItemRenamer {
  @Override
  public void rename(Item item, MainController mainController) {
    mainController.openGroupRenameDialog((GroupItem) item);
  }
}
