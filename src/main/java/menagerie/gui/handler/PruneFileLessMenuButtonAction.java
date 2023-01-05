package menagerie.gui.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import menagerie.gui.itemhandler.Items;
import menagerie.model.menagerie.itemhandler.properties.ItemProperties;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.gui.screens.dialogs.ProgressScreen;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.CancellableThread;

public class PruneFileLessMenuButtonAction extends CancellableThread {

  private final Menagerie menagerie;
  private final ProgressScreen ps;
  private final ScreenPane screenPane;

  public PruneFileLessMenuButtonAction(Menagerie menagerie, ProgressScreen ps,
                                       ScreenPane screenPane) {
    this.menagerie = menagerie;
    this.ps = ps;
    this.screenPane = screenPane;
  }

  @Override
  public void run() {
    final int total = menagerie.getItems().size();
    int i = 0;

    List<Item> toDelete = new ArrayList<>();

    for (Item item : menagerie.getItems()) {
      if (!running) {
        break;
      }
      i++;

      Optional<ItemProperties> itemProps = Items.get(ItemProperties.class, item);
      if (itemProps.isPresent() && itemProps.get().isFileBased(item) &&
          itemProps.get().getFile(item) != null && !itemProps.get().getFile(item).exists()) {
        toDelete.add(item);
      }

      final int finalI = i;
      Platform.runLater(() -> ps.setProgress(finalI, total));
    }

    menagerie.forgetItems(toDelete);
    Platform.runLater(() -> {
      ps.close();
      new AlertDialogScreen().open(screenPane, "Pruning complete",
          toDelete.size() + " file-less items pruned.", null);
    });
  }
}
