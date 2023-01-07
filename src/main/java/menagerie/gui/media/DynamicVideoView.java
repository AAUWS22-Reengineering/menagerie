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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Affine;
import menagerie.util.NanoTimer;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.MediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

/**
 * Dynamically sized view that shows a video using VLCJ.
 */
public class DynamicVideoView extends StackPane {

  /**
   * Logger for this class
   */
  private static final Logger LOGGER = Logger.getLogger(DynamicVideoView.class.getName());

  /**
   * List of all video views created during runtime. Used for cleanup
   */
  private static final List<DynamicVideoView> VIDEO_VIEWS = new ArrayList<>();

  private static final String DEFAULT_STYLE_CLASS = "dynamic-video-view";
  private static final String CONTROLS_STYLE_CLASS = "dynamic-video-controls";

  /**
   * Shared mute button image
   */
  private final Image muteImage = new Image(getClass().getResource("/misc/mute.png").toString());
  /**
   * Shared unmute button image
   */
  private final Image unmuteImage =
      new Image(getClass().getResource("/misc/unmute.png").toString());

  /**
   * Playback slider
   */
  private final Slider slider = new Slider(0, 1, 0);
  /**
   * Time/duration label
   */
  private final Label durationLabel = new Label("0:00/0:00");
  /**
   * Mute image view
   */
  private final ImageView muteImageView = new ImageView(unmuteImage);
  /**
   * Pause image view
   */
  private final ImageView pauseImageView =
      new ImageView(getClass().getResource("/misc/play.png").toString());
  /**
   * Video canvas
   */
  private final Canvas canvas = new Canvas(100, 50);
  /**
   * Media player component
   */
  private MediaPlayerComponent mediaPlayerComponent = null;
  /**
   * VLCJ media player
   */
  private EmbeddedMediaPlayer mediaPlayer = null;
  /**
   * Video image for passing data to canvas
   */
  private WritableImage img;
  /**
   * Pixel writer for the video image canvas
   */
  private PixelWriter pixelWriter;
  /**
   * Format to write pixels with
   */
  private final WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();

  /**
   * Property for the mute-state of the video
   */
  private final BooleanProperty mute = new SimpleBooleanProperty(false);

  /**
   * Period of time between each displayed frame
   */
  private static final double TIMER_PERIOD = 1000.0 / 60;

  /**
   * Frame display timer
   */
  private NanoTimer timer = new NanoTimer(TIMER_PERIOD) {
    @Override
    protected void onSucceeded() {
      renderFrame();
    }
  };

  /**
   * Flagged true if the VLCJ components have been released. This object is unusable if true.
   */
  private boolean released = false;

  public DynamicVideoView() {
    super();

    VIDEO_VIEWS.add(this);

    getStyleClass().addAll(DEFAULT_STYLE_CLASS);

    setupCanvas();
    HBox bottomBarHBox = getHBox();
    BorderPane controlsBorderPane = getControlsBorderPane();
    getChildren().add(controlsBorderPane);

    StackPane.setAlignment(pauseImageView, Pos.CENTER);
    pixelWriter = canvas.getGraphicsContext2D().getPixelWriter();

    setupMuteControls();

    addEventHandler(MouseEvent.MOUSE_ENTERED, event -> controlsBorderPane.setBottom(bottomBarHBox));
    addEventHandler(MouseEvent.MOUSE_EXITED, event -> controlsBorderPane.setBottom(null));
    setupClickEventHandler();
    setupScrollEventHandler();
  }

  private void setupClickEventHandler() {
    addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getButton() == MouseButton.PRIMARY) {
        if (isPlaying()) {
          pause();
        } else {
          play();
        }
      } else if (event.getButton() == MouseButton.SECONDARY) {
        setMute(!isMuted());
      }
      event.consume();
    });
  }

  private void setupScrollEventHandler() {
    addEventHandler(ScrollEvent.SCROLL, event -> {
      if (!released && getMediaPlayer() != null) {
        long duration = getMediaPlayer().media().info().duration();
        float delta;
        if (duration < 10000) {
          delta = 0.25f;
        } else if (duration < 30000) {
          delta = 5000f / duration;
        } else {
          delta = 10000f / duration;
        }
        if (event.getDeltaY() < 0) {
          delta = -delta;
        }
        float newPos = Math.min(0.9999f, Math.max(getMediaPlayer().status().position() + delta, 0));
        getMediaPlayer().controls().setPosition(newPos);
        slider.setValue(newPos);
        event.consume();
      }
    });
  }

  private void setupMuteControls() {
    muteImageView.setPickOnBounds(true);
    muteImageView.addEventHandler(MouseEvent.MOUSE_ENTERED,
        event -> getScene().setCursor(Cursor.HAND));
    muteImageView.addEventHandler(MouseEvent.MOUSE_EXITED,
        event -> getScene().setCursor(Cursor.DEFAULT));
    muteImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
      setMute(!isMuted());
      event.consume();
    });

    mute.addListener((observable, oldValue, newValue) -> {
      if (!released && getMediaPlayer() != null) {
        getMediaPlayer().audio().setMute(newValue);
      }
      muteImageView.setImage(newValue ? muteImage : unmuteImage);
    });
  }

  private BorderPane getControlsBorderPane() {
    BorderPane controlsBorderPane = new BorderPane(null, null, null, null, null);
    HBox.setHgrow(slider, Priority.ALWAYS);
    slider.setFocusTraversable(false);
    slider.valueChangingProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue && !released && getMediaPlayer() != null) {
        getMediaPlayer().controls().setPosition((float) slider.getValue());
      }
    });
    slider.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (!released && getMediaPlayer() != null) {
        final long len = getMediaPlayer().status().length();
        final long cur = (long) (len * newValue.doubleValue());
        final long totalSeconds = (len / 1000) % 60;
        final long totalMinutes = len / 1000 / 60;
        final long seconds = (cur / 1000) % 60;
        final long minutes = cur / 1000 / 60;
        durationLabel.setText(
            String.format("%d:%02d/%d:%02d", minutes, seconds, totalMinutes, totalSeconds));
      }
    });
    controlsBorderPane.setPadding(new Insets(5));
    BorderPane.setAlignment(muteImageView, Pos.BOTTOM_RIGHT);
    return controlsBorderPane;
  }

  private HBox getHBox() {
    HBox bottomBarHBox = new HBox(5, durationLabel, slider, muteImageView);
    bottomBarHBox.getStyleClass().addAll(CONTROLS_STYLE_CLASS);
    bottomBarHBox.setAlignment(Pos.CENTER);
    bottomBarHBox.setPadding(new Insets(3));
    return bottomBarHBox;
  }

  private void setupCanvas() {
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
    getChildren().add(canvas);
  }

  /**
   * @return The VLCJ media player backing this view.
   */
  private MediaPlayer getMediaPlayer() {
    if (!released) {
      if (mediaPlayer == null) {
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        mediaPlayer =
            mediaPlayerComponent.mediaPlayerFactory().mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.videoSurface().set(new JavaFxVideoSurface());
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
          @Override
          public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
            Platform.runLater(() -> {
              if (!slider.isValueChanging()) {
                slider.setValue(newPosition);
              }
            });
          }

          @Override
          public void mediaPlayerReady(MediaPlayer mediaPlayer) {
            mediaPlayer.submit(() -> mediaPlayer.audio().setMute(isMuted()));
          }
        });
      }

      return mediaPlayer;
    } else {
      return null;
    }
  }

  @Override
  protected double computeMinWidth(double height) {
    return 40;
  }

  @Override
  protected double computeMinHeight(double width) {
    return 40;
  }

  @Override
  public void resize(double width, double height) {
    if (img == null) {
      setWidth(width);
      setHeight(height);
    } else {
      double scale = 1;
      if (img.getWidth() > width) {
        scale = width / img.getWidth();
      }
      if (scale * img.getHeight() > height) {
        scale = height / img.getHeight();
      }

      setWidth(img.getWidth() * scale);
      setHeight(img.getHeight() * scale);
    }
  }

  /**
   * Mutes or unmutes the video
   *
   * @param b True to mute, false to unmute.
   */
  public void setMute(boolean b) {
    mute.set(b);
  }

  /**
   * Sets the video to repeat or not.
   *
   * @param b Repeat
   */
  public void setRepeat(boolean b) {
    MediaPlayer mediaPlayer = getMediaPlayer();
    if (!released && mediaPlayer != null) {
      mediaPlayer.controls().setRepeat(b);
    }
  }

  /**
   * @return True if the video is currently playing
   */
  public boolean isPlaying() {
    MediaPlayer mediaPlayer = getMediaPlayer();
    return !released && mediaPlayer != null && mediaPlayer.status().isPlaying();
  }

  /**
   * @return True if the video will repeat on completion
   */
  public boolean isRepeating() {
    MediaPlayer mediaPlayer = getMediaPlayer();
    return !released && mediaPlayer != null && mediaPlayer.controls().getRepeat();
  }

  /**
   * @return True if the video is muted
   */
  public boolean isMuted() {
    return mute.get();
  }

  /**
   * Pauses the video without resetting it
   */
  public void pause() {
    MediaPlayer mediaPlayer = getMediaPlayer();
    if (!released && mediaPlayer != null) {
      mediaPlayer.controls().pause();
      timer.cancel();
      if (!getChildren().contains(pauseImageView)) {
        getChildren().add(pauseImageView);
      }
    }
  }

  /**
   * Plays the video from the current position
   */
  public void play() {
    MediaPlayer mediaPlayer = getMediaPlayer();
    if (!released && mediaPlayer != null) {
      mediaPlayer.controls().play();
      if (!timer.getState().equals(Worker.State.READY)) {
        timer = new NanoTimer(TIMER_PERIOD) {
          @Override
          protected void onSucceeded() {
            renderFrame();
          }
        };
      }
      timer.start();
      getChildren().remove(pauseImageView);
    }
  }

  /**
   * Stops the video and resets it to the start of the video
   */
  public void stop() {
    MediaPlayer mediaPlayer = getMediaPlayer();
    if (!released && mediaPlayer != null) {
      mediaPlayer.controls().stop();
      timer.cancel();
      if (!getChildren().contains(pauseImageView)) {
        getChildren().add(pauseImageView);
      }
    }
  }

  /**
   * Loads and starts a video
   *
   * @param path Path to video file
   */
  public void startMedia(String path) {
    MediaPlayer mediaPlayer = getMediaPlayer();
    if (!released && mediaPlayer != null) {
      mediaPlayer.media().start(path);
      play();
    }
  }

  /**
   * Releases all VLCJ components and renders this object invalid
   */
  public void releaseVLCJ() {
    MediaPlayer mediaPlayer = getMediaPlayer();
    if (!released && mediaPlayer != null) {
      if (mediaPlayerComponent != null) {
        mediaPlayerComponent.mediaPlayerFactory().release();
      }
      mediaPlayer.controls().stop();
      mediaPlayer.release();
      released = true;
    }
  }

  private class JavaFxVideoSurface extends CallbackVideoSurface {
    JavaFxVideoSurface() {
      super(new JavaFxBufferFormatCallback(), new JavaFxRenderCallback(), true,
          VideoSurfaceAdapters.getVideoSurfaceAdapter());
    }
  }

  private class JavaFxBufferFormatCallback implements BufferFormatCallback {
    @Override
    public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
      DynamicVideoView.this.img = new WritableImage(sourceWidth, sourceHeight);
      DynamicVideoView.this.pixelWriter = img.getPixelWriter();

      Platform.runLater(() -> {
        DynamicVideoView.this.setWidth(sourceWidth);
        DynamicVideoView.this.setHeight(sourceHeight);
      });
      return new RV32BufferFormat(sourceWidth, sourceHeight);
    }
  }

  private final Semaphore semaphore = new Semaphore(1);

  private class JavaFxRenderCallback implements RenderCallback {
    @Override
    public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers,
                        BufferFormat bufferFormat) {
      try {
        semaphore.acquire();
        pixelWriter.setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), pixelFormat,
            nativeBuffers[0], bufferFormat.getPitches()[0]);
        semaphore.release();
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Renders one frame from the VLCJ context to the canvas
   */
  private void renderFrame() {
    GraphicsContext g = canvas.getGraphicsContext2D();

    double width = canvas.getWidth();
    double height = canvas.getHeight();

    if (img != null) {
      double imageWidth = img.getWidth();
      double imageHeight = img.getHeight();

      double sx = width / imageWidth;
      double sy = height / imageHeight;

      double sf = Math.min(sx, sy);

      double scaledW = imageWidth * sf;
      double scaledH = imageHeight * sf;

      Affine ax = g.getTransform();

      g.translate((width - scaledW) / 2, (height - scaledH) / 2);

      if (sf != 1.0) {
        g.scale(sf, sf);
      }

      try {
        semaphore.acquire();
        g.drawImage(img, 0, 0);
        semaphore.release();
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }

      g.setTransform(ax);
    }
  }

  /**
   * Releases all existing VLCJ video view resources
   */
  public static void releaseAllVLCJ() {
    for (DynamicVideoView videoView : VIDEO_VIEWS) {
      try {
        videoView.releaseVLCJ();
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error while releasing VLCJ resources", e);
      }
    }
  }

}
