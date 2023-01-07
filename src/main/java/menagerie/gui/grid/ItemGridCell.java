/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.gui.grid;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import menagerie.gui.itemhandler.Items;
import menagerie.gui.itemhandler.gridcell.ItemCellHandler;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.util.listeners.ObjectListener;
import org.controlsfx.control.GridCell;

import java.util.Objects;


public class ItemGridCell extends GridCell<Item> {

  private static final Font SMALL_FONT =
      Font.font(Font.getDefault().getName(), FontWeight.BOLD, 12);
  public static final String DEFAULT_STYLE_CLASS = "item-grid-cell";
  public static final String SELECTED = "selected";

  /**
   * Shared group tag image.
   */
  private static Image groupTagImage = null;
  /**
   * Shared video tag image.
   */
  private static Image videoTagImage = null;

  private final ImageView thumbnailView = new ImageView();
  private final ImageView tagView = new ImageView();
  private final Label centerLabel = new Label();
  private final Label bottomRightLabel = new Label();

  /**
   * Listens for an image to be ready
   */
  private final ObjectListener<Image> imageReadyListener;

  /**
   * Listens for changes to the current group item's title
   */
  private final InvalidationListener groupTitleListener = observable -> Platform.runLater(() -> {
    centerLabel.setText(((GroupItem) getItem()).getTitle());
    Tooltip tt = new Tooltip(((GroupItem) getItem()).getTitle());
    tt.setWrapText(true);
    setTooltip(tt);
  });

  /**
   * Listens for changes to the current group item's contents
   */
  private final InvalidationListener groupListListener = observable -> Platform.runLater(
      () -> bottomRightLabel.setText(((GroupItem) getItem()).getElements().size() + ""));

  /**
   * Listens for changes to the current item's selected state
   */
  private final InvalidationListener selectedListener = observable -> updateSelected(
      ((BooleanProperty) getItem().getMetadata().get(SELECTED)).get());


  public ItemGridCell() {
    super();
    this.getStyleClass().add(DEFAULT_STYLE_CLASS);

    centerLabel.setPadding(new Insets(5));
    centerLabel.setFont(SMALL_FONT);
    DropShadow effect = new DropShadow();
    effect.setSpread(0.5);
    centerLabel.setEffect(effect);
    centerLabel.setWrapText(true);
    StackPane.setAlignment(centerLabel, Pos.TOP_CENTER);

    bottomRightLabel.setPadding(new Insets(2));
    bottomRightLabel.setFont(SMALL_FONT);
    bottomRightLabel.setEffect(new DropShadow());
    StackPane.setAlignment(bottomRightLabel, Pos.BOTTOM_RIGHT);

    tagView.setTranslateX(-3);
    tagView.setTranslateY(-5);
    StackPane.setAlignment(tagView, Pos.BOTTOM_LEFT);
    if (groupTagImage == null) {
      groupTagImage = new Image(Objects.requireNonNull(getClass().getResource("/misc/group_tag.png")).toString());
    }
    if (videoTagImage == null) {
      videoTagImage = new Image(Objects.requireNonNull(getClass().getResource("/misc/video_tag.png")).toString());
    }

    setGraphic(new StackPane(thumbnailView, centerLabel, bottomRightLabel, tagView));
    setAlignment(Pos.CENTER);

    imageReadyListener = image -> Platform.runLater(() -> thumbnailView.setImage(image));
  }

  @Override
  protected void updateItem(Item item, boolean empty) {
    cleanUpOldItem();

    super.updateItem(item, empty);

    if (empty) {
      initEmpty();
    } else {
      initSelected(item);
      initThumbnail(item);

      Items.get(ItemCellHandler.class, item).ifPresent(itemCellHandler ->
          itemCellHandler.initialize(item, this));
    }
  }

  /**
   * Cleans up listeners and connections to the previous item. Called while updating the item
   */
  private void cleanUpOldItem() {
    if (getItem() != null) {
      if (getItem().getThumbnail() != null) {
        getItem().getThumbnail().doNotWant();
        if (!getItem().getThumbnail().isLoaded()) {
          getItem().getThumbnail().doNotWant();
        }
        getItem().getThumbnail().removeImageReadyListener(imageReadyListener);
      }
      Object obj = getItem().getMetadata().get(SELECTED);
      if (obj instanceof BooleanProperty) {
        ((BooleanProperty) obj).removeListener(selectedListener);
      }

      Items.get(ItemCellHandler.class, getItem()).ifPresent(itemCellHandler ->
          itemCellHandler.cleanUp(getItem(), this));
    }
  }

  /**
   * Initializes this cell to be empty
   */
  private void initEmpty() {
    thumbnailView.setImage(null);
    centerLabel.setText(null);
    bottomRightLabel.setText(null);
    tagView.setImage(null);
  }

  /**
   * Initializes/displays the thumbnail of an item
   *
   * @param item Item to display thumbnail of
   */
  private void initThumbnail(Item item) {
    if (item.getThumbnail() != null) {
      item.getThumbnail().want();
      if (item.getThumbnail().getImage() != null) {
        thumbnailView.setImage(item.getThumbnail().getImage());
      } else {
        item.getThumbnail().want();
        thumbnailView.setImage(null);
        item.getThumbnail().addImageReadyListener(imageReadyListener);
      }
    }
  }

  /**
   * Initializes/listens to the selected state of an item
   *
   * @param item Item to initialize and listen to selected state of
   */
  private void initSelected(Item item) {
    Object obj = item.getMetadata().get(SELECTED);
    if (obj instanceof BooleanProperty) {
      updateSelected(((BooleanProperty) obj).get());
      ((BooleanProperty) obj).addListener(selectedListener);
    } else {
      final boolean sel = getGridView() instanceof ItemGridView &&
          ((ItemGridView) getGridView()).isSelected(item);
      BooleanProperty prop = new SimpleBooleanProperty(sel);
      prop.addListener(selectedListener);
      item.getMetadata().put(SELECTED, prop);
      updateSelected(sel);
    }
  }

  public static Image getGroupTagImage() {
    return groupTagImage;
  }

  public static Image getVideoTagImage() {
    return videoTagImage;
  }

  public ImageView getThumbnailView() {
    return thumbnailView;
  }

  public ImageView getTagView() {
    return tagView;
  }

  public Label getCenterLabel() {
    return centerLabel;
  }

  public Label getBottomRightLabel() {
    return bottomRightLabel;
  }

  public InvalidationListener getGroupTitleListener() {
    return groupTitleListener;
  }

  public InvalidationListener getGroupListListener() {
    return groupListListener;
  }
}
