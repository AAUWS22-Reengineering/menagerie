package menagerie.gui.itemhandler;

import javafx.scene.control.Label;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Util;

public class MediaItemInfoBoxRenderer implements ItemInfoBoxRenderer {
  @Override
  public void setItemInfoBoxLabels(Item item, Label fileSizeLabel, Label filePathLabel, Label resolutionLabel) {
    fileSizeLabel.setText(Util.bytesToPrettyString(((MediaItem) item).getFile().length()));
    filePathLabel.setText(((MediaItem) item).getFile().toString());
    if (((MediaItem) item).isImage()) { //TODO: Support for video resolution (May be possible in latest VLCJ api)
      if (((MediaItem) item).getImage().isBackgroundLoading() &&
          ((MediaItem) item).getImage().getProgress() != 1) {
        resolutionLabel.setText("Loading...");
        ((MediaItem) item).getImage().progressProperty()
            .addListener((observable, oldValue, newValue) -> {
              if (newValue.doubleValue() == 1 && !((MediaItem) item).getImage().isError()) {
                resolutionLabel.setText((int) ((MediaItem) item).getImage().getWidth() + "x" +
                    (int) ((MediaItem) item).getImage().getHeight());
              }
            });
      } else {
        resolutionLabel.setText((int) ((MediaItem) item).getImage().getWidth() + "x" +
            (int) ((MediaItem) item).getImage().getHeight());
      }
    }
  }
}
