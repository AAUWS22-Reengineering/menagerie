package menagerie.gui.itemhandler.opener;

import menagerie.gui.MainController;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MediaItemOpener implements ItemOpener {

  private static final Logger LOGGER = Logger.getLogger(MediaItemOpener.class.getName());

  @Override
  public void open(Item item, MainController controller) {
    try {
      Desktop.getDesktop().open(((MediaItem) item).getFile());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e,
          () -> "Failed to open file with system default: " +
              ((MediaItem) item).getFile());
    }
  }
}
