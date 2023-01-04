package menagerie.gui.itemhandler.infoboxrenderer;

import javafx.scene.control.Label;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Util;

public class MediaItemInfoBoxRenderer implements ItemInfoBoxRenderer {
  @Override
  public void setItemInfoBoxLabels(Item item, Label fileSizeLabel, Label filePathLabel, Label resolutionLabel) {
    MediaItem mediaItem = (MediaItem) item;
    fileSizeLabel.setText(Util.bytesToPrettyString(mediaItem.getFile().length()));
    filePathLabel.setText(mediaItem.getFile().toString());
    if (mediaItem.isImage()) { //TODO: Support for video resolution (May be possible in latest VLCJ api)
      if (mediaItem.getImage().isBackgroundLoading() &&
          mediaItem.getImage().getProgress() != 1) {
        resolutionLabel.setText("Loading...");
        mediaItem.getImage().progressProperty()
            .addListener((observable, oldValue, newValue) -> {
              if (newValue.doubleValue() == 1 && !mediaItem.getImage().isError()) {
                resolutionLabel.setText((int) mediaItem.getImage().getWidth() + "x" +
                    (int) mediaItem.getImage().getHeight());
              }
            });
      } else {
        resolutionLabel.setText((int) mediaItem.getImage().getWidth() + "x" +
            (int) mediaItem.getImage().getHeight());
      }
    }
  }
}
