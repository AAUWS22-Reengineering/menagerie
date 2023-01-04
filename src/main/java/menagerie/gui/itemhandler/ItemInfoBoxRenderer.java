package menagerie.gui.itemhandler;

import javafx.scene.control.Label;
import menagerie.model.menagerie.Item;

public interface ItemInfoBoxRenderer {
  void setItemInfoBoxLabels(Item item, Label fileSizeLabel, Label filePathLabel, Label resolutionLabel);
}
