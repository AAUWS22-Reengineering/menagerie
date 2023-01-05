package menagerie.gui.itemhandler.gridcell;

import javafx.scene.control.Tooltip;
import menagerie.gui.grid.ItemGridCell;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

public class GroupItemCellHandler implements ItemCellHandler {

  @Override
  public void initialize(Item item, ItemGridCell igc) {
    GroupItem groupItem = (GroupItem) item;

    igc.getCenterLabel().setText(groupItem.getTitle());
    Tooltip tt = new Tooltip(groupItem.getTitle());
    tt.setWrapText(true);
    igc.setTooltip(tt);
    groupItem.titleProperty().addListener(igc.getGroupTitleListener());

    igc.getTagView().setImage(ItemGridCell.getGroupTagImage());

    igc.getBottomRightLabel().setText(groupItem.getElements().size() + "");
    groupItem.getElements().addListener(igc.getGroupListListener());
  }

  @Override
  public void cleanUp(Item item, ItemGridCell igc) {
    GroupItem groupItem = (GroupItem) item;

    groupItem.titleProperty().removeListener(igc.getGroupTitleListener());
    groupItem.getElements().removeListener(igc.getGroupListListener());
  }
}
