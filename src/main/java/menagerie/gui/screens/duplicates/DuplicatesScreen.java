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

package menagerie.gui.screens.duplicates;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.util.listeners.ObjectListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DuplicatesScreen extends Screen {

  private final CheckBox nonDupeCheckBox = new CheckBox("Not a duplicate");

  private final Label similarityLabel = new Label("N/A");
  private final TextField indexTextField = new TextField("0");

  private Menagerie menagerie = null;
  private List<SimilarPair<MediaItem>> pairs = null;
  private SimilarPair<MediaItem> currentPair = null;

  private final BooleanProperty deleteFile = new SimpleBooleanProperty(true);
  private final BooleanProperty preload = new SimpleBooleanProperty(true);

  private final PairPreview pairPreview = new PairPreview(this::previewPair);

  private ObjectListener<Item> selectListener = null;

  private final DuplicatesScreenSide leftSide = new DuplicatesScreenSide();
  private final DuplicatesScreenSide rightSide = new DuplicatesScreenSide();
  private final ListChangeListener<Tag> tagListener = new TagListChangeListener(
      leftSide.getTagList(), rightSide.getTagList(), () -> currentPair);

  public DuplicatesScreen() {
    registerKeyEvents();
    getStyleClass().addAll(ROOT_STYLE_CLASS);

    // ---------------------------------------------- Center Element -----------------------------------------------
    registerContextMenuRequestedEvents();
    registerCellFactories();
    configureCenterElements();
    addSplitPane();

    // ---------------------------------------- Bottom element -----------------------------------------------------
    VBox bottom = new VBox(5);
    bottom.setPadding(new Insets(5));
    constructFirstElement(bottom);
    constructSecondElement(bottom);
  }

  private void registerCellFactories() {
    leftSide.getTagList().setCellFactory(
        new DuplicatesScreenLeftCellFactory<>(this::getCurrentPair));
    rightSide.getTagList().setCellFactory(
        new DuplicatesScreenRightCellFactory<>(this::getCurrentPair));
  }

  private SimilarPair<MediaItem> getCurrentPair() {
    return currentPair;
  }

  private void registerContextMenuRequestedEvents() {
    leftSide.getTagList().setOnContextMenuRequested(new LeftContextMenuListener(
        leftSide.getMediaView(), () -> selectListener, () -> currentPair, this::close));
    rightSide.getTagList().setOnContextMenuRequested(new RightContextMenuListener(
        rightSide.getMediaView(), () -> selectListener, () -> currentPair, this::close));
  }

  private void constructFirstElement(VBox bottom) {
    configureIndexTextField();
    HBox hbc = new HBox(indexTextField, similarityLabel);
    hbc.setAlignment(Pos.CENTER);
    Button leftDeleteButton = new Button("Delete");
    leftDeleteButton.setOnAction(event -> deleteItem(currentPair.getObject1()));
    Button rightDeleteButton = new Button("Delete");
    rightDeleteButton.setOnAction(event -> deleteItem(currentPair.getObject2()));
    HBox hbl = new HBox(leftDeleteButton);
    hbl.setAlignment(Pos.CENTER_LEFT);
    HBox hbr = new HBox(rightDeleteButton);
    hbr.setAlignment(Pos.CENTER_RIGHT);
    bottom.getChildren().add(new BorderPane(hbc, null, hbr, null, hbl));
  }

  private void constructSecondElement(VBox bottom) {
    Button prevPairButton = new Button("<-");
    prevPairButton.setOnAction(event -> pairPreview.previewPrev(pairs, currentPair));
    Button closeButton = new Button("Close");
    closeButton.setOnAction(event -> close());
    Button nextPairButton = new Button("->");
    nextPairButton.setOnAction(event -> pairPreview.previewNext(pairs, currentPair));
    registerAddListenerToCheckbox();
    HBox hb = new HBox(5, prevPairButton, closeButton, nextPairButton, nonDupeCheckBox);
    hb.setAlignment(Pos.CENTER);
    bottom.getChildren().add(hb);
    setBottom(bottom);
    setDefaultFocusNode(closeButton);
  }

  private void registerAddListenerToCheckbox() {
    nonDupeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        menagerie.addNonDuplicate(currentPair);
      } else {
        menagerie.removeNonDuplicate(currentPair);
      }
    });
  }

  private void configureIndexTextField() {
    indexTextField.setAlignment(Pos.CENTER_RIGHT);
    indexTextField.setPrefWidth(50);
    indexTextField.setOnAction(event -> {
      int i = pairs.indexOf(currentPair);
      try {
        int temp = Integer.parseInt(indexTextField.getText()) - 1;
        i = Math.max(0, Math.min(temp, pairs.size() - 1)); // Clamp to valid indices
      } catch (NumberFormatException e) {
        // Nothing
      }

      preview(pairs.get(i));
      requestFocus();
    });
  }

  private void addSplitPane() {

    final BorderPane lbp = leftSide.buildBorderPane(leftSide.getTagList(), leftSide.getInfoBox());
    final var lsp = leftSide.buildStackPane(lbp);

    final BorderPane rbp =
        rightSide.buildBorderPane(rightSide.getInfoBox(), rightSide.getTagList());
    final var rsp = rightSide.buildStackPane(rbp);

    SplitPane sp = new SplitPane(lsp, rsp);
    sp.setOnMouseEntered(event -> {
      lbp.setRight(leftSide.getTagList());
      rbp.setLeft(rightSide.getTagList());
    });
    sp.setOnMouseExited(event -> {
      lbp.setRight(null);
      rbp.setLeft(null);
    });
    setCenter(sp);
  }

  private void configureCenterElements() {
    leftSide.configureCenterElements(Pos.BOTTOM_LEFT);
    rightSide.configureCenterElements(Pos.BOTTOM_RIGHT);
  }

  private void registerKeyEvents() {
    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        close();
      } else if (event.getCode() == KeyCode.LEFT) {
        pairPreview.previewPrev(pairs, currentPair);
      } else if (event.getCode() == KeyCode.RIGHT) {
        pairPreview.previewNext(pairs, currentPair);
      } else if (event.getCode() == KeyCode.END) {
        preview(pairs.get(pairs.size() - 1));
      } else if (event.getCode() == KeyCode.HOME) {
        preview(pairs.get(0));
      }
      event.consume();
    });
  }

  /**
   * Opens this screen in a manager and displays the first pair.
   *
   * @param manager   Manager to open in.
   * @param menagerie Menagerie.
   * @param pairs     Pairs to view and resolve.
   */
  public void open(ScreenPane manager, Menagerie menagerie, List<SimilarPair<MediaItem>> pairs) {
    if (manager == null || menagerie == null || pairs == null || pairs.isEmpty()) {
      return;
    }

    openWithOldPairs(manager, menagerie);

    this.pairs = pairs;
    preview(pairs.get(0));
  }

  public void openWithOldPairs(ScreenPane manager, Menagerie menagerie) {
    this.menagerie = menagerie;
    leftSide.styleTagList();
    rightSide.styleTagList();
    manager.open(this);
  }

  /**
   * Displays a pair.
   *
   * @param pair Pair to display.
   */
  private void preview(SimilarPair<MediaItem> pair) {
    if (currentPair != null) {
      currentPair.getObject1().getTags().removeListener(tagListener);
      currentPair.getObject2().getTags().removeListener(tagListener);
    }
    currentPair = pair;

    leftSide.clearTagList();
    rightSide.clearTagList();

    if (pair != null) {
      previewPair(pair);
    } else {
      resetPreview();
    }
  }

  private void resetPreview() {
    leftSide.reset();
    rightSide.reset();
    similarityLabel.setText("N/A");
    nonDupeCheckBox.setSelected(false);
  }

  private void previewPair(SimilarPair<MediaItem> pair) {
    leftSide.preview(pair.getObject1());
    rightSide.preview(pair.getObject2());

    currentPair.getObject1().getTags().addListener(tagListener);
    currentPair.getObject2().getTags().addListener(tagListener);

    DecimalFormat df = new DecimalFormat("#.##");
    indexTextField.setText((pairs.indexOf(pair) + 1) + "");
    similarityLabel.setText(
        "/" + pairs.size() + ": " + df.format(pair.getSimilarity() * 100) + "%");

    nonDupeCheckBox.setSelected(menagerie.hasNonDuplicate(pair));
  }

  /**
   * Attempts to delete an item. WARNING: Deletes the file.
   *
   * @param toDelete Item to delete.
   */
  private void deleteItem(MediaItem toDelete) {
    if (menagerie == null) {
      return;
    }

    if (currentPair.getObject1().equals(toDelete)) {
      leftSide.stopPlayback();
    }
    if (currentPair.getObject2().equals(toDelete)) {
      rightSide.stopPlayback();
    }

    int index = pairs.indexOf(currentPair);

    if (isDeleteFile()) {
      menagerie.deleteItem(toDelete);
    } else {
      menagerie.forgetItem(toDelete);
    }

    //Remove other pairs containing the deleted image
    for (final var pair : new ArrayList<>(pairs)) {
      if (toDelete.equals(pair.getObject1()) || toDelete.equals(pair.getObject2())) {
        int i = pairs.indexOf(pair);
        pairs.remove(pair);
        if (i < index) {
          index--;
        }
      }
    }

    if (index > pairs.size() - 1) {
      index = pairs.size() - 1;
    }

    if (pairs.isEmpty()) {
      close();
    } else {
      preview(pairs.get(index));
    }
  }

  public List<SimilarPair<MediaItem>> getPairs() {
    return pairs;
  }

  private boolean isDeleteFile() {
    return deleteFile.get();
  }

  public BooleanProperty preloadProperty() {
    return preload;
  }

  /**
   * @param selectListener Listener waiting for the user to ask to select an item in the explorer.
   */
  public void setSelectListener(ObjectListener<Item> selectListener) {
    this.selectListener = selectListener;
  }

  public ItemInfoBox getLeftInfoBox() {
    return leftSide.getInfoBox();
  }

  public ItemInfoBox getRightInfoBox() {
    return rightSide.getInfoBox();
  }

}
