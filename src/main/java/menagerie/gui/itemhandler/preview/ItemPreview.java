package menagerie.gui.itemhandler.preview;

import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.Item;

public interface ItemPreview {
  boolean preview(DynamicMediaView previewMediaView, Item item);
  void stop(DynamicMediaView previewMediaView, Item currentlyPreviewing);

  /**
   * Preview item on slideshow screen.
   * @return True, if preview was successful.
   */
  boolean previewInSlideshow(DynamicMediaView mediaView, ItemInfoBox infoBox, Item item);
}
