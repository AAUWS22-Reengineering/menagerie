package menagerie.gui.screens.duplicates;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Tag;

import java.util.Comparator;

import static javafx.scene.layout.Region.USE_PREF_SIZE;

public class DuplicatesScreenSide {

  private final DynamicMediaView mediaView = new DynamicMediaView();
  private final ListView<Tag> tagList = new ListView<>();
  private final ItemInfoBox infoBox = new ItemInfoBox();

  ListView<Tag> getTagList() {
    return tagList;
  }

  DynamicMediaView getMediaView() {
    return mediaView;
  }

  BorderPane buildBorderPane(Node node2, Node node4) {
    BorderPane lbp = new BorderPane(null, null, node2, null, node4);
    lbp.setPickOnBounds(false);
    return lbp;
  }

  StackPane buildStackPane(BorderPane lbp) {
    return new StackPane(mediaView, lbp);
  }

  void configureCenterElements(Pos position) {
    tagList.setPrefWidth(200);
    configureInfoBox(infoBox, position);
  }

  private void configureInfoBox(ItemInfoBox infoBox, Pos position) {
    infoBox.setAlignment(position);
    infoBox.setMaxHeight(USE_PREF_SIZE);
    infoBox.setOpacity(0.75);
    BorderPane.setAlignment(infoBox, position);
  }

  void styleTagList() {
    tagList.setDisable(false);
    tagList.setOpacity(0.75);
  }

  void clearTagList() {
    tagList.getItems().clear();
  }

  void reset() {
    mediaView.preview(null);
    infoBox.setItem(null);
  }

  void preview(MediaItem item) {
    mediaView.preview(item);
    tagList.getItems().addAll(item.getTags());
    tagList.getItems().sort(Comparator.comparing(Tag::getName));
    infoBox.setItem(item);
  }

  void stopPlayback() {
    mediaView.stop();
  }

  ItemInfoBox getInfoBox() {
    return infoBox;
  }
}
