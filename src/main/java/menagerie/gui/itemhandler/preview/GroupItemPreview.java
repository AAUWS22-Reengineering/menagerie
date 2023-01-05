package menagerie.gui.itemhandler.preview;

import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.util.logging.Logger;

public class GroupItemPreview implements ItemPreview {

  @Override
  public boolean preview(DynamicMediaView v, Item item) {
    if (!((GroupItem) item).getElements().isEmpty()) {
      v.preview(((GroupItem) item).getElements().get(0));
    }
    return true;
  }

  @Override
  public void stop(DynamicMediaView previewMediaView, Item currentlyPreviewing) {
    if (((MediaItem) currentlyPreviewing).isVideo()) {
      previewMediaView.stop();
    }
  }
}
