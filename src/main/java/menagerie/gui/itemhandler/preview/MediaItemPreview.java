package menagerie.gui.itemhandler.preview;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import javafx.scene.image.Image;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Filters;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public class MediaItemPreview implements ItemPreview {

  private static final Logger LOGGER = Logger.getLogger(MediaItemPreview.class.getName());

  @Override
  public boolean preview(DynamicMediaView v, Item i) {
    try {
      MediaItem item = (MediaItem) i;

      if (item.isImage()) {
        previewImage(v, item);
      } else if (item.isVideo() && v.getVideoView() != null) {
        previewVideo(v, item);
      } else if (Filters.RAR_NAME_FILTER.accept(item.getFile())) {
        previewRAR(v, item);
      } else if (Filters.ZIP_NAME_FILTER.accept(item.getFile())) {
        previewZIP(v, item);
      } else if (Filters.PDF_NAME_FILTER.accept(item.getFile())) {
        previewPDF(v, item);
      } else if (Files.probeContentType(item.getFile().toPath()).equalsIgnoreCase("text/plain")) {
        previewPlainText(v, item);
      } else {
        return false; // Unknown file type, can't preview it
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error previewing media: " + i, e);
    }
    return true;
  }

  private void previewImage(DynamicMediaView v, MediaItem item) {
    if (v.getVideoView() != null) {
      v.getVideoView().stop();
    }
    v.getImageView().setTrueImage(item.getImage());
    v.showImageView();
  }

  private void previewVideo(DynamicMediaView v, MediaItem item) {
    v.getImageView().setTrueImage(null);
    v.getVideoView().startMedia(item.getFile().getAbsolutePath());
    v.showVideoView();
  }

  private void previewRAR(DynamicMediaView v, MediaItem item) {
    try (Archive a = new Archive(Files.newInputStream(item.getFile().toPath()))) {
      List<FileHeader> fileHeaders = a.getFileHeaders();
      if (!fileHeaders.isEmpty()) {
        try (InputStream is = a.getInputStream(fileHeaders.get(0))) {
          v.getImageView().setTrueImage(new Image(is));
        }
      }
    } catch (RarException | IOException | NullPointerException e) {
      LOGGER.log(Level.INFO, "Failed to preview RAR: " + item.getFile());
    }
    v.showImageView();
  }

  private void previewZIP(DynamicMediaView v, MediaItem item) {
    try (ZipFile zip = new ZipFile(item.getFile())) {
      if (zip.entries().hasMoreElements()) {
        try (InputStream is = zip.getInputStream(zip.entries().nextElement())) {
          v.getImageView().setTrueImage(new Image(is));
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.INFO, "Failed to preview ZIP: " + item.getFile());
    }
  }

  private void previewPDF(DynamicMediaView v, MediaItem item) throws IOException {
    if (v.getCurrentPDF() != null) {
      v.getCurrentPDF().close();
    }
    v.setCurrentPDF(PDDocument.load(item.getFile()));
    v.setPDFPage(0);
    v.showPDFView();
  }

  private void previewPlainText(DynamicMediaView v, MediaItem item) throws IOException {
    v.getTextView().setText(String.join("\n", Files.readAllLines(item.getFile().toPath())));
    v.showTextView();
  }

  @Override
  public void stop(DynamicMediaView previewMediaView, Item currentlyPreviewing) {
    if (((MediaItem) currentlyPreviewing).isVideo()) {
      previewMediaView.stop();
    }
  }

  @Override
  public boolean previewInSlideshow(DynamicMediaView mediaView, ItemInfoBox infoBox, Item item) {
    mediaView.preview(item);
    infoBox.setItem(item);
    return true;
  }
}
