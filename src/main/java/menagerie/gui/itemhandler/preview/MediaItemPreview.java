package menagerie.gui.itemhandler.preview;

import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class MediaItemPreview implements ItemPreview {
  @Override
  public void stop(DynamicMediaView previewMediaView, Item currentlyPreviewing) {
    if (((MediaItem) currentlyPreviewing).isVideo()) {
      previewMediaView.stop();
    }
  }
}
