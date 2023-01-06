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

package menagerie.gui.media;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import menagerie.gui.Main;
import menagerie.gui.itemhandler.Items;
import menagerie.gui.itemhandler.preview.ItemPreview;
import menagerie.model.menagerie.Item;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dynamically sized view that can display images or videos.
 * Does not display content larger than one-to-one size.
 */
public class DynamicMediaView extends StackPane {

  /**
   * Logger for this class
   */
  private static final Logger LOGGER = Logger.getLogger(DynamicMediaView.class.getName());

  /**
   * The video view used to display videos
   */
  private DynamicVideoView videoView;

  /**
   * The image view used to display images
   */
  private final PanZoomImageView imageView = new PanZoomImageView();

  /**
   * The text view used to display text
   */
  private final TextArea textView = new TextArea();

  /**
   * Pane containing PDF controls
   */
  private final BorderPane pdfControlsPane;

  /**
   * Page counter for PDF view
   */
  private final Label pdfPageLabel = new Label("0/0");

  /**
   * The currently active PDF, if present
   */
  private PDDocument currentPDF = null;

  /**
   * Current PDF page index
   */
  private int currentPDFPageIndex = 0;

  public DynamicMediaView() {
    super();

    Button pdfLeftButton = new Button("<-");
    pdfLeftButton.setOnAction(event -> {
      if (currentPDFPageIndex > 0) {
        setPDFPage(currentPDFPageIndex - 1);
      }
    });
    Button pdfRightButton = new Button("->");
    pdfRightButton.setOnAction(event -> {
      if (currentPDFPageIndex < currentPDF.getNumberOfPages() - 1) {
        setPDFPage(currentPDFPageIndex + 1);
      }
    });
    HBox bottomHBox = new HBox(5, pdfLeftButton, pdfPageLabel, pdfRightButton);
    bottomHBox.setAlignment(Pos.CENTER);
    bottomHBox.setPadding(new Insets(5));
    pdfControlsPane = new BorderPane(null, null, null, bottomHBox, null);
    pdfControlsPane.setPickOnBounds(false);

    textView.setEditable(false);
    textView.setFocusTraversable(false);
    textView.setWrapText(true);
  }

  /**
   * Attempts to display a media item.
   * If media item is a video and VLCJ is not loaded, nothing will be displayed.
   *
   * @param item Item to display.
   * @return True if successful, false otherwise.
   */
  public boolean preview(Item item) {
    if (getVideoView() != null) {
      getVideoView().stop();
    }
    imageView.setTrueImage(null);
    hideAllViews();

    return Items.get(ItemPreview.class, item)
        .map(preview -> preview.preview(this, item))
        .orElse(true);
  }

  /**
   * Hides both the video and the image views, if they exist.
   */
  private void hideAllViews() {
    getChildren().removeAll(getImageView(), textView, pdfControlsPane);
    if (getVideoView() != null) {
      getChildren().remove(getVideoView());
    }
  }

  /**
   * Shows the image view.
   */
  public void showImageView() {
    hideAllViews();
    getChildren().add(getImageView());
  }

  /**
   * Shows the video view, if VLCJ is loaded.
   */
  public void showVideoView() {
    hideAllViews();
    if (getVideoView() != null) {
      getChildren().add(getVideoView());
    }
  }

  /**
   * Shows the text view
   */
  public void showTextView() {
    hideAllViews();
    getChildren().add(textView);
  }

  /**
   * Shows the PDF view with controls
   */
  public void showPDFView() {
    hideAllViews();
    getChildren().addAll(getImageView(), pdfControlsPane);
  }

  /**
   * Attempts to get the video view. If VLCJ not loaded and this is the first call to this method,
   * video view will be constructed.
   *
   * @return The video view, or null if VLCJ is not loaded.
   */
  public DynamicVideoView getVideoView() {
    if (!Main.isVlcjLoaded()) {
      return null;
    }

    if (videoView == null) {
      videoView = new DynamicVideoView();
    }

    return videoView;
  }

  /**
   * Gets the active image view
   *
   * @return Image view owned by this media view
   */
  public PanZoomImageView getImageView() {
    return imageView;
  }

  /**
   * Sets the current PDF page
   *
   * @param page Index to go to
   */
  public void setPDFPage(int page) {
    if (currentPDF == null || page < 0 || page >= currentPDF.getNumberOfPages()) {
      return;
    }

    currentPDFPageIndex = page;

    try {
      BufferedImage img = new PDFRenderer(currentPDF).renderImageWithDPI(page, 300);
      imageView.setTrueImage(SwingFXUtils.toFXImage(img, null));
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to render PDF page: " + page, e);
    }

    pdfPageLabel.setText((page + 1) + "/" + currentPDF.getNumberOfPages());
  }

  /**
   * @param mute The mute property for the media player, if the video view exists.
   */
  public void setMute(boolean mute) {
    if (getVideoView() != null) {
      getVideoView().setMute(mute);
    }
  }

  /**
   * @param repeat The repeat property for the media player, if the video view exists.
   */
  public void setRepeat(boolean repeat) {
    if (getVideoView() != null) {
      getVideoView().setRepeat(repeat);
    }
  }

  /**
   * @return True if the video is currently playing.
   */
  public boolean isPlaying() {
    return getVideoView() != null && getVideoView().isPlaying();
  }

  /**
   * Pauses the media player. No effect if not playing.
   */
  public void pause() {
    if (getVideoView() != null) {
      getVideoView().pause();
    }
  }

  /**
   * Plays the media player. No effect if already playing.
   */
  public void play() {
    if (getVideoView() != null) {
      getVideoView().play();
    }
  }

  /**
   * Releases VLCJ resources and invalidates the video view.
   */
  public void releaseVLCJ() {
    if (videoView != null) {
      Platform.runLater(() -> getChildren().remove(videoView));
      videoView.releaseVLCJ();
      videoView = null;
    }
  }

  /**
   * Stop the video playback, if present and playing
   */
  public void stop() {
    if (getVideoView() != null) {
      getVideoView().stop();
    }
  }

  public PDDocument getCurrentPDF() {
    return currentPDF;
  }

  public void setCurrentPDF(PDDocument currentPDF) {
    this.currentPDF = currentPDF;
  }

  public TextArea getTextView() {
    return textView;
  }
}
