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

package menagerie.gui.taglist;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import menagerie.model.menagerie.Tag;
import menagerie.util.listeners.ObjectListener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TagListCell extends ListCell<Tag> {

  private static final Logger LOGGER = Logger.getLogger(TagListCell.class.getName());

  private static final String DEFAULT_STYLE_CLASS = "tag-list-cell";

  private final Label countLabel = new Label();
  private final Label nameLabel = new Label();
  private final Label addButton = new Label("+");
  private final Label removeButton = new Label("-");
  private final HBox rightHBox = new HBox(2, countLabel);

  private final ChangeListener<String> colorListener = (observable, oldValue, newValue) -> {
    if (Platform.isFxApplicationThread()) {
      setTextColor(newValue);
    } else {
      Platform.runLater(() -> setTextColor(newValue));
    }
  };
  private final ChangeListener<Number> frequencyListener = (observable, oldValue, newValue) -> {
    if (Platform.isFxApplicationThread()) {
      setFrequency(newValue.intValue());
    } else {
      Platform.runLater(() -> setFrequency(newValue.intValue()));
    }
  };


  public TagListCell(ObjectListener<Tag> addListener, ObjectListener<Tag> removeListener) {
    getStyleClass().addAll(DEFAULT_STYLE_CLASS);

    addButton.setPadding(new Insets(0, 2, 0, 2));
    addButton.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY) {
        if (addListener != null) {
          addListener.pass(getItem());
        }
        event.consume();
      }
    });
    addButton.getStyleClass().add("tag-list-cell-button");

    removeButton.setPadding(new Insets(0, 3, 0, 3));
    removeButton.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY) {
        if (removeListener != null) {
          removeListener.pass(getItem());
        }
        event.consume();
      }
    });
    removeButton.getStyleClass().add("tag-list-cell-button");

    countLabel.setMinWidth(USE_PREF_SIZE);
    BorderPane bp = new BorderPane(null, null, rightHBox, null, nameLabel);
    setGraphic(bp);
    nameLabel.maxWidthProperty()
        .bind(bp.widthProperty().subtract(rightHBox.widthProperty()).subtract(15));

    addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
      if (addListener != null && removeListener != null && getItem() != null &&
          !rightHBox.getChildren().contains(addButton)) {
        rightHBox.getChildren().addAll(addButton, removeButton);
      }
    });
    addEventHandler(MouseEvent.MOUSE_EXITED,
        event -> rightHBox.getChildren().removeAll(addButton, removeButton));
  }

  @Override
  protected void updateItem(Tag tag, boolean empty) {
    if (getItem() != null) {
      getItem().colorProperty().removeListener(colorListener);
      getItem().frequencyProperty().removeListener(frequencyListener);
    }

    super.updateItem(tag, empty);

    if (empty || tag == null) {
      nameLabel.setText(null);
      countLabel.setText(null);
      setTooltip(null);
      setTextColor(null);
    } else {
      nameLabel.setText(tag.getName());
      setFrequency(tag.getFrequency());
      setTooltip(new Tooltip("(ID: " + tag.getId() + ") " + tag.getName()));
      setTextColor(tag.getColor());

      tag.colorProperty().addListener(colorListener);
      tag.frequencyProperty().addListener(frequencyListener);
    }
  }

  private void setFrequency(int freq) {
    countLabel.setText("(" + freq + ")");
  }

  private void setTextColor(String color) {
    if (color == null || color.isEmpty()) {
      nameLabel.setTextFill(Color.WHITE);
      countLabel.setTextFill(Color.WHITE);
    } else {
      try {
        nameLabel.setTextFill(Paint.valueOf(color));
        countLabel.setTextFill(Paint.valueOf(color));
      } catch (IllegalArgumentException e) {
        LOGGER.log(Level.WARNING, "Invalid color string: " + color, e);
        setTextColor(null);
      }
    }
  }

}
