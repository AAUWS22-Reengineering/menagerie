package menagerie.gui.screens.duplicates;

import java.util.function.Supplier;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Action;
import menagerie.util.listeners.ObjectListener;

public class LeftContextMenuListener implements EventHandler<ContextMenuEvent> {

  private final DynamicMediaView leftMediaView;
  private final Supplier<ObjectListener<Item>> selectListener;
  private final Supplier<SimilarPair<MediaItem>> currentPair;

  private final Action closeScreen;

  public LeftContextMenuListener(DynamicMediaView leftMediaView,
                                 Supplier<ObjectListener<Item>> selectListener,
                                 Supplier<SimilarPair<MediaItem>> currentPair, Action closeScreen) {
    this.leftMediaView = leftMediaView;
    this.selectListener = selectListener;
    this.currentPair = currentPair;
    this.closeScreen = closeScreen;
  }

  @Override
  public void handle(ContextMenuEvent event) {
    MenuItem select = new MenuItem("Select in explorer");
    MenuItem combineTags = new MenuItem("Add tags to other -->");
    ContextMenu cm = new ContextMenu(select, combineTags);
    select.setOnAction(event1 -> {
      if (selectListener != null) {
        selectListener.get().pass(currentPair.get().getObject1());
      }
      cm.hide();
      closeScreen.execute();
    });
    combineTags.setOnAction(event1 -> {
      currentPair.get().getObject1().getTags()
          .forEach(tag -> currentPair.get().getObject2().addTag(tag));
      cm.hide();
    });
    cm.show(leftMediaView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

}
