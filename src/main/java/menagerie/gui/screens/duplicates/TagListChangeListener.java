package menagerie.gui.screens.duplicates;

import java.util.Comparator;
import java.util.function.Supplier;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListView;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Tag;

public class TagListChangeListener implements ListChangeListener<Tag> {

  private final ListView<Tag> leftTagList;
  private final ListView<Tag> rightTagList;
  private final Supplier<SimilarPair<MediaItem>> currentPair;

  public TagListChangeListener(ListView<Tag> leftTagList, ListView<Tag> rightTagList,
                               Supplier<SimilarPair<MediaItem>> currentPair) {
    this.leftTagList = leftTagList;
    this.rightTagList = rightTagList;
    this.currentPair = currentPair;
  }

  @Override
  public void onChanged(Change<? extends Tag> c) {
    while (c.next()) {
      repopulateTagLists();
    }
  }

  private void repopulateTagLists() {
    leftTagList.getItems().clear();
    if (currentPair != null) {
      leftTagList.getItems().addAll(currentPair.get().getObject1().getTags());
      leftTagList.getItems().sort(Comparator.comparing(Tag::getName));
    }

    rightTagList.getItems().clear();
    if (currentPair != null) {
      rightTagList.getItems().addAll(currentPair.get().getObject2().getTags());
      rightTagList.getItems().sort(Comparator.comparing(Tag::getName));
    }
  }
}
