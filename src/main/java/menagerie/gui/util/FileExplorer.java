package menagerie.gui.util;

import menagerie.gui.itemhandler.Items;
import menagerie.gui.itemhandler.opener.ItemOpener;
import menagerie.model.menagerie.Item;

import java.util.List;

public class FileExplorer {

  private FileExplorer() {
  }

  public static void openExplorer(List<Item> selected) {
    Item last = selected.get(selected.size() - 1);
    Items.get(ItemOpener.class, last).ifPresent(itemOpener -> itemOpener.openInExplorer(last));
  }

  public static void openDefault(List<Item> selected) {
    Item last = selected.get(selected.size() - 1);
    Items.get(ItemOpener.class, last).ifPresent(itemOpener -> itemOpener.open(last, null));
  }

  public static boolean hasAllowedFileEnding(String name) {
    return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
           name.endsWith(".bmp");
  }
}
