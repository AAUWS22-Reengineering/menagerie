package menagerie.gui.handler;

import javafx.application.Platform;
import menagerie.gui.itemhandler.Items;
import menagerie.gui.screens.dialogs.ProgressScreen;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.itemhandler.similarity.ItemSimilarity;
import menagerie.util.CancellableThread;

import java.util.Optional;

public class RebuildSimilarityCacheMenuButtonAction extends CancellableThread {

  private final Menagerie menagerie;
  private final ProgressScreen ps;

  public RebuildSimilarityCacheMenuButtonAction(Menagerie menagerie, ProgressScreen ps) {
    this.menagerie = menagerie;
    this.ps = ps;
  }

  /**
   * Compare each item to each other in terms of similarity.
   */
  @Override
  public void run() {
    final int total = menagerie.getItems().size();
    final double confidenceSquare = 1 - (1 - MediaItem.MIN_CONFIDENCE) * (1 - MediaItem.MIN_CONFIDENCE);

    for (int i = 0; i < menagerie.getItems().size(); i++) {
      Item i1 = menagerie.getItems().get(i);

      Optional<ItemSimilarity> itemSim1 = Items.get(ItemSimilarity.class, i1);
      // skip item if it does not support sim calc
      if (itemSim1.isEmpty() || !itemSim1.get().isEligibleForSimCalc(i1)) {
        continue;
      }

      boolean hasSimilar = false;
      for (int j = 0; j < menagerie.getItems().size(); j++) {
        if (i == j) {
          continue;
        }
        Item i2 = menagerie.getItems().get(j);

        Optional<ItemSimilarity> itemSim2 = Items.get(ItemSimilarity.class, i2);
        // skip item if it does not support sim calc or already has noSim flag set
        // REENG: The condition itemSim2.get().hasNoSimilarity(i2) was present in the original code, but does seem like a bug.
        //  With it the rebuilding does not do anything...
        if (itemSim2.isEmpty() || !itemSim2.get().isEligibleForSimCalc(i2) /*|| itemSim2.get().hasNoSimilarity(i2)*/) {
          continue;
        }

        if (itemSim1.get().isSimilarTo(i1, i2, confidenceSquare)) {
          hasSimilar = true;
          break;
        }
      }

      itemSim1.get().setNoSimilarity(i1, !hasSimilar);

      final int finalI = i;
      Platform.runLater(() -> ps.setProgress(finalI, total));
    }

    Platform.runLater(ps::close);
  }

}
