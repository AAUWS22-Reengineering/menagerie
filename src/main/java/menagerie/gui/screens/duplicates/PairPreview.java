package menagerie.gui.screens.duplicates;

import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;

import java.util.List;
import java.util.function.Consumer;

class PairPreview {

  private final Consumer<SimilarPair<MediaItem>> preview;

  public PairPreview(Consumer<SimilarPair<MediaItem>> preview) {
    this.preview = preview;
  }

  void previewNext(List<SimilarPair<MediaItem>> pairs, SimilarPair<MediaItem> currentPair) {
    if (pairs == null || pairs.isEmpty()) {
      return;
    }

    if (currentPair == null) {
      preview.accept(pairs.get(0));
    } else {
      int i = pairs.indexOf(currentPair);
      if (i >= 0) {
        if (i + 1 < pairs.size()) {
          preview.accept(pairs.get(i + 1));
        }
      } else {
        preview.accept(pairs.get(0));
      }
    }
  }

  void previewPrev(List<SimilarPair<MediaItem>> pairs, SimilarPair<MediaItem> currentPair) {
    if (pairs == null || pairs.isEmpty()) {
      return;
    }

    if (currentPair == null) {
      preview.accept(pairs.get(0));
    } else {
      int i = pairs.indexOf(currentPair);
      if (i > 0) {
        preview.accept(pairs.get(i - 1));
      } else {
        preview.accept(pairs.get(0));
      }
    }
  }
}
