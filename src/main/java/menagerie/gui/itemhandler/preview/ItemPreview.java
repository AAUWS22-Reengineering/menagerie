package menagerie.gui.itemhandler.preview;

import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.Item;

public interface ItemPreview {
  void stop(DynamicMediaView previewMediaView, Item currentlyPreviewing);
}
