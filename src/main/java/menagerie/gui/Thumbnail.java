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

package menagerie.gui;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import menagerie.model.menagerie.Item;
import menagerie.util.Filters;
import menagerie.util.listeners.ObjectListener;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * JavaFX Image wrapper specifically for loading thumbnails of various types.
 */
public class Thumbnail {

  private static final Logger LOGGER = Logger.getLogger(Thumbnail.class.getName());

  // ------------------------------- Constants -----------------------------------------

  private static final String[] VLC_THUMBNAILER_ARGS =
      {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats",
          "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};

  /**
   * Vertical and horizontal maximum size of thumbnails.
   */
  public static final int THUMBNAIL_SIZE = 150;

  private static final String UNSUPPORTED_IMAGE_PATH = "/misc/default-thumb.png";
  private static Image unsupportedImage = null;

  // -------------------------------- Variables -----------------------------------------

  private final File file;
  private final Item owner;
  private Image image;

  private int want = 0;

  private final Set<ObjectListener<Image>> imageReadyListeners = new HashSet<>();

  private static volatile boolean generalThreadRunning = false;
  private static volatile boolean videoThreadRunning = false;
  private static final BlockingQueue<Thumbnail> generalQueue = new LinkedBlockingQueue<>();
  private static final BlockingQueue<Thumbnail> videoQueue = new LinkedBlockingQueue<>();
  private static MediaPlayer mediaPlayer;
  private static MediaPlayerFactory mediaPlayerFactory;


  /**
   * Constructs a thumbnail for a file and begins loading it.
   *
   * @param file A media file that is accepted by the {@link Filters}
   */
  public Thumbnail(Item owner, File file) {
    this.owner = owner;
    this.file = file;

    if (Main.isVlcjLoaded() && !videoThreadRunning) {
      startVideoThread();
    }

    if (!generalThreadRunning) {
      startGeneralThread();
    }

    if (Filters.VIDEO_NAME_FILTER.accept(file)) {
      videoQueue.add(this);
    } else {
      generalQueue.add(this);
    }
  }

  private void loadItemImage() {
    if (Filters.IMAGE_NAME_FILTER.accept(file)) {
      createImageForImage();
    } else if (Filters.RAR_NAME_FILTER.accept(file)) {
      if (!createImageForRAR()) {
        // image creation failed
        return;
      }
    } else if (Filters.ZIP_NAME_FILTER.accept(file)) {
      if (!createImageForZIP()) {
        // image creation failed
        return;
      }
    } else if (Filters.PDF_NAME_FILTER.accept(file)) {
      if (!createImageForPDF()) {
        // image creation failed
        return;
      }
    } else {
      createImageForUnsupported();
    }
    synchronized (imageReadyListeners) {
      imageReadyListeners.forEach(listener -> listener.pass(image));
    }
  }

  private void createImageForImage() {
    image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);
  }

  private void createImageForUnsupported() {
    synchronized (this) {
      if (unsupportedImage == null) {
        unsupportedImage = new Image(getClass().getResourceAsStream(UNSUPPORTED_IMAGE_PATH));
      }

      image = unsupportedImage;
    }
  }

  private boolean createImageForPDF() {
    try (PDDocument doc = PDDocument.load(file)) {
      PDRectangle mb = doc.getPage(0).getMediaBox();
      float scale = THUMBNAIL_SIZE / mb.getWidth();
      if (THUMBNAIL_SIZE / mb.getHeight() < scale) {
        scale = THUMBNAIL_SIZE / mb.getHeight();
      }
      BufferedImage img = new PDFRenderer(doc).renderImage(0, scale);
      image = SwingFXUtils.toFXImage(img, null);
    } catch (IOException e) {
      LOGGER.log(Level.INFO, "Failed to thumbnail PDF: " + file, e);
      return false;
    }
    return true;
  }

  private boolean createImageForZIP() {
    try (ZipFile zip = new ZipFile(file)) {
      if (zip.entries().hasMoreElements()) {
        try (InputStream is = zip.getInputStream(zip.entries().nextElement())) {
          image = new Image(is, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);
        }
      } else {
        return false;
      }
    } catch (IOException e) {
      LOGGER.log(Level.INFO, "Failed to thumbnail ZIP: " + file);
      return false;
    }
    return true;
  }

  private boolean createImageForRAR() {
    try (Archive a = new Archive(new FileInputStream(file))) {
      List<FileHeader> fileHeaders = a.getFileHeaders();
      if (!fileHeaders.isEmpty()) {
        try (InputStream is = a.getInputStream(fileHeaders.get(0))) {
          image = new Image(is, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);
        }
      } else {
        return false;
      }
    } catch (RarException | IOException | NullPointerException e) {
      LOGGER.log(Level.INFO, "Failed to thumbnail RAR: " + file);
      return false;
    }
    return true;
  }

  private void loadVideoImage() {
    final CountDownLatch inPositionLatch = new CountDownLatch(2);
    final CountDownLatch snapshotLatch = new CountDownLatch(1);

    MediaPlayerEventListener eventListener = new MediaPlayerEventAdapter() {
      @Override
      public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
        inPositionLatch.countDown();
      }

      @Override
      public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
        inPositionLatch.countDown();
      }

      @Override
      public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {
        snapshotLatch.countDown();
      }
    };
    Objects.requireNonNull(mediaPlayer).events().addMediaPlayerEventListener(eventListener);

    try {
      if (mediaPlayer.media().start(file.getAbsolutePath())) {
        if (!inPositionLatch.await(2, TimeUnit.SECONDS)) {
          return;
        }

        if (mediaPlayer.video().videoDimension() != null) {
          float vidWidth = (float) mediaPlayer.video().videoDimension().getWidth();
          float vidHeight = (float) mediaPlayer.video().videoDimension().getHeight();
          float scale = Thumbnail.THUMBNAIL_SIZE / vidWidth;
          if (scale * vidHeight > Thumbnail.THUMBNAIL_SIZE) {
            scale = Thumbnail.THUMBNAIL_SIZE / vidHeight;
          }
          int width = (int) (scale * vidWidth);
          int height = (int) (scale * vidHeight);

          try {
            if (!createImageForVideo(snapshotLatch, eventListener, width, height)) {
              return;
            }
          } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to get video snapshot of file: " + file, e);
          }
        }

        mediaPlayer.controls().stop();
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error while trying to create video thumbnail: " + file, e);
      Thread.currentThread().interrupt();
    }
  }

  private boolean createImageForVideo(CountDownLatch snapshotLatch, MediaPlayerEventListener eventListener, int width, int height) throws IOException, InterruptedException {
    File tempFile = File.createTempFile("menagerie-video-thumb", ".jpg");
    mediaPlayer.snapshots().save(tempFile, width, height);
    boolean snapshotWaitSuccessful = snapshotLatch.await(2, TimeUnit.SECONDS);
    mediaPlayer.events().removeMediaPlayerEventListener(eventListener);
    if (!snapshotWaitSuccessful) {
      return false;
    }
    image = new Image(tempFile.toURI().toString());
    try {
      Files.delete(tempFile.toPath());
    }
    catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Failed to delete tempfile: " + tempFile, ex);
    }

    synchronized (imageReadyListeners) {
      imageReadyListeners.forEach(listener -> listener.pass(image));
    }
    return true;
  }

  private static void startGeneralThread() {
    generalThreadRunning = true;

    Thread t = new Thread(() -> {
      while (generalThreadRunning) {
        try {
          Thumbnail thumb = generalQueue.take();
          if (thumb.isDoNotLoad()) {
            thumb.owner.purgeThumbnail();
            continue;
          }

          thumb.loadItemImage();
        } catch (InterruptedException ignore) {
          Thread.currentThread().interrupt();
        }
      }
    }, "General Thumbnailer Thread");
    t.setDaemon(true);
    t.start();
  }

  private static void startVideoThread() {
    videoThreadRunning = true;

    Thread t = new Thread(() -> {
      if (mediaPlayer != null) {
        mediaPlayer.release();
      }
      mediaPlayerFactory = new MediaPlayerFactory(VLC_THUMBNAILER_ARGS);
      mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

      while (videoThreadRunning) {
        try {
          Thumbnail thumb = videoQueue.take();
          if (thumb.isDoNotLoad()) {
            thumb.owner.purgeThumbnail();
            continue;
          }

          thumb.loadVideoImage();
          if (!thumb.isLoaded()) {
            thumb.owner.purgeThumbnail();
          }
        } catch (InterruptedException ignore) {
          Thread.currentThread().interrupt();
        }
      }
    }, "Video Thumbnailer Thread");
    t.setDaemon(true);
    t.start();
  }

  public boolean addImageReadyListener(ObjectListener<Image> listener) {
    return imageReadyListeners.add(listener);
  }

  public boolean removeImageReadyListener(ObjectListener<Image> listener) {
    return imageReadyListeners.remove(listener);
  }

  public synchronized void want() {
    want++;
  }

  public synchronized void doNotWant() {
    want--;
  }

  public synchronized boolean isDoNotLoad() {
    return want == 0;
  }

  public static void releaseVLCJResources() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayerFactory.release();
      mediaPlayer = null;
    }
  }

  /**
   * @return This thumbnail's image.
   */
  public synchronized Image getImage() {
    return image;
  }

  /**
   * @return True if the image has been completely loaded.
   */
  public synchronized boolean isLoaded() {
    return image != null;
  }

}
