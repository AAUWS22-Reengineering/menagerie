package menagerie.gui.screens.duplicates;

import javafx.scene.control.*;
import javafx.util.Callback;
import menagerie.gui.taglist.OtherMissingTagListCell;
import menagerie.gui.taglist.TagListCell;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Tag;

import java.util.function.Supplier;

public class DuplicatesScreenRightCellFactory<T> implements Callback<ListView<T>, ListCell<Tag>> {

  private final Supplier<SimilarPair<MediaItem>> currentPair;

  public DuplicatesScreenRightCellFactory(Supplier<SimilarPair<MediaItem>> currentPair) {
    this.currentPair = currentPair;
  }

  @Override
  public TagListCell call(ListView<T> tListView) {
    TagListCell c = new OtherMissingTagListCell(() -> currentPair.get().getObject1());
    MenuItem addToOther = new MenuItem("Add to other");
    addToOther.setOnAction(event -> currentPair.get().getObject1().addTag(c.getItem()));
    MenuItem removeTag = new MenuItem("Remove tag");
    removeTag.setOnAction(event -> currentPair.get().getObject2().removeTag(c.getItem()));
    ContextMenu cm = new ContextMenu(addToOther, new SeparatorMenuItem(), removeTag);
    c.setOnContextMenuRequested(
        event -> cm.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY()));
    return c;
  }
}
