package menagerie.gui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.input.Dragboard;
import menagerie.gui.grid.ItemGridView;
import menagerie.gui.itemhandler.Items;
import menagerie.gui.itemhandler.gridviewselector.ItemGridViewSelector;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.itemhandler.properties.ItemProperties;

public class GridViewUtil {

  private GridViewUtil() {
  }

  public static void doDragAndDrop(Dragboard db, ItemGridView itemGridView) {
    for (Item item : itemGridView.getSelected()) {
      Optional<ItemGridViewSelector> igvs = Items.get(ItemGridViewSelector.class, item);
      if (igvs.isPresent() && igvs.get().doDragAndDrop(item, db, itemGridView)) {
        break;
      }
    }
  }

  public static List<File> getSelectedFiles(ItemGridView itemGridView) {
    List<File> files = new ArrayList<>();
    itemGridView.getSelected().forEach(item -> {
      Items.get(ItemProperties.class, item).ifPresent(itemProps -> {
        for (Item i : itemProps.getItems(item)) {
          Items.get(ItemProperties.class, i).ifPresent(itemProperties -> {
            if (itemProperties.isFileBased(i)) {
              files.add(itemProperties.getFile(i));
            }
          });
        }
      });
    });
    return files;
  }

}
