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

public class RightContextMenuListener implements EventHandler<ContextMenuEvent> {

  private final DynamicMediaView rightMediaView;
  private final Supplier<ObjectListener<Item>> selectListener;
  private final Supplier<SimilarPair<MediaItem>> currentPair;

  private final Action closeScreen;

  public RightContextMenuListener(DynamicMediaView rightMediaView,
                                  Supplier<ObjectListener<Item>> selectListener,
                                  Supplier<SimilarPair<MediaItem>> currentPair,
                                  Action closeScreen) {
    this.rightMediaView = rightMediaView;
    this.selectListener = selectListener;
    this.currentPair = currentPair;
    this.closeScreen = closeScreen;
  }

  @Override
  public void handle(ContextMenuEvent event) {
    MenuItem select = new MenuItem("Select in explorer");
    MenuItem combineTags = new MenuItem("<-- Add tags to other");
    ContextMenu cm = new ContextMenu(select, combineTags);
    select.setOnAction(event1 -> {
      if (selectListener != null) {
        selectListener.get().pass(currentPair.get().getObject2());
      }
      cm.hide();
      closeScreen.execute();
    });
    combineTags.setOnAction(event1 -> {
      currentPair.get().getObject2().getTags()
          .forEach(tag -> currentPair.get().getObject1().addTag(tag));
      cm.hide();
    });
    cm.show(rightMediaView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

}
