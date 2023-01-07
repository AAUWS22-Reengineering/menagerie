/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.gui.screens.duplicates;

import menagerie.gui.itemhandler.Items;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.itemhandler.similarity.ItemSimilarity;
import menagerie.util.CancellableThread;
import menagerie.util.listeners.ObjectListener;
import menagerie.util.listeners.PokeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DuplicateFinderThread extends CancellableThread {

  private final List<Item> compareFrom;
  private final List<Item> compareTo;
  private final double confidence;

  private final Menagerie menagerie;

  private final List<SimilarPair<MediaItem>> pairs = new ArrayList<>();

  private final PokeListener progressListener;
  private final ObjectListener<List<SimilarPair<MediaItem>>> finishListener;


  public DuplicateFinderThread(Menagerie menagerie, List<Item> compareFrom, List<Item> compareTo,
                               double confidence, PokeListener progressListener,
                               ObjectListener<List<SimilarPair<MediaItem>>> finishListener) {
    this.menagerie = menagerie;
    this.compareFrom = compareFrom;
    this.compareTo = compareTo;
    this.confidence = confidence;

    this.progressListener = progressListener;
    this.finishListener = finishListener;

    setName("Duplicate Finder");
    setDaemon(true);
  }

  @Override
  public void run() {
    final double confidenceSquare = 1 - (1 - confidence) * (1 - confidence);

    for (Item item1 : compareFrom) {
      if (!running) {
        break;
      }
      if (!canUseInComparison(item1)) {
        continue;
      }

      performComparison(confidenceSquare, item1);

      if (progressListener != null) {
        progressListener.poke();
      }
    }

    if (running && finishListener != null) {
      finishListener.pass(pairs);
    }
    running = false;
  }

  private boolean canUseInComparison(Item item) {
    Optional<ItemSimilarity> itemSim = Items.get(ItemSimilarity.class, item);
    return itemSim.isPresent() && itemSim.get().isEligibleForSimCalc(item) && !itemSim.get().hasNoSimilar(item);
  }

  private void performComparison(double confidenceSquare, Item base) {
    for (Item item2 : compareTo) {
      if (!running) {
        break;
      }

      Optional<ItemSimilarity> itemSim2 = Items.get(ItemSimilarity.class, item2);
      if (itemSim2.isEmpty() || !itemSim2.get().isEligibleForSimCalc(item2) || itemSim2.get().hasNoSimilar(item2) ||
          base.equals(item2)) {
        continue;
      }

      createSimPairIfRequired(confidenceSquare, (MediaItem) base, (MediaItem) item2);
    }
  }

  private void createSimPairIfRequired(double confidenceSquare, MediaItem item1, MediaItem item2) {
    final double similarity = item1.getSimilarityTo(item2);
    if (similarity >= confidenceSquare ||
        (similarity >= confidence && item1.getHistogram().isColorful() &&
            item2.getHistogram().isColorful())) {
      SimilarPair<MediaItem> pair =
          new SimilarPair<>(item1, item2, similarity);
      if (!menagerie.hasNonDuplicate(pair)) {
        pairs.add(pair);
      }
    }
  }

}
