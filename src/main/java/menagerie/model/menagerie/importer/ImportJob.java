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

package menagerie.model.menagerie.importer;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.settings.MenagerieSettings;

/**
 * A runnable job that will import a file.
 */
public abstract class ImportJob {

  private static final Logger LOGGER = Logger.getLogger(ImportJob.class.getName());

  private ImporterThread importer = null;

  protected File file = null;
  protected MediaItem item = null;
  protected GroupItem addToGroup = null;

  private final ObjectProperty<ImportJobStatus> status =
      new SimpleObjectProperty<>(ImportJobStatus.WAITING);

  private final ItemHasher itemHasher = new ItemHasher();
  private final DuplicateFinder duplicateFinder = new DuplicateFinder();

  /**
   * Synchronously runs this job. Downloads, imports, creates hash, creates histogram, finds duplicates, and finds similar items.
   *
   * @param menagerie Menagerie to import into.
   * @param settings  Application settings to import with.
   */
  void runJob(Menagerie menagerie, MenagerieSettings settings) {
    setStatus(ImportJobStatus.IMPORTING);

    doRunJob(menagerie, settings);

    itemHasher.tryHashHist(item, file);
    if (duplicateFinder.tryDuplicate(menagerie, item)) {
      setStatus(ImportJobStatus.FAILED_DUPLICATE);
      return;
    }
    if (addToGroup != null) {
      addToGroup.addItem(item);
    }

    duplicateFinder.trySimilar(menagerie, settings, item);
    setStatus(ImportJobStatus.SUCCEEDED);
  }

  abstract void doRunJob(Menagerie menagerie, MenagerieSettings settings);


  /**
   * @return The imported item. Null if not yet imported.
   */
  public synchronized MediaItem getItem() {
    return item;
  }

  /**
   * @return The web URL. Null if not imported from web.
   */
  public abstract URL getUrl();

  /**
   * @return The file. Null if not yet downloaded.
   */
  public synchronized File getFile() {
    return file;
  }

  /**
   * @return The pre-existing duplicate. Null if not yet checked, or no duplicate found.
   */
  public synchronized MediaItem getDuplicateOf() {
    return duplicateFinder.getDuplicateOf();
  }

  /**
   * @return The list of similar pairs. Null if not checked.
   */
  public synchronized List<SimilarPair<MediaItem>> getSimilarTo() {
    return duplicateFinder.getSimilarTo();
  }

  public void addProgressListener(ChangeListener<Number> listener) {
    // default not supported
  }

  public void removeProgressListener(ChangeListener<Number> listener) {
    // default not supported
  }

  /**
   * @return The current progress.
   */
  public double getProgress() {
    return 0.0;
  }

  /**
   * @return The status of this job.
   */
  public ImportJobStatus getStatus() {
    return status.get();
  }

  /**
   * @param status The new status to set this job as.
   */
  protected void setStatus(ImportJobStatus status) {
    this.status.set(status);
  }

  public void addStatusListener(ChangeListener<ImportJobStatus> listener) {
    this.status.addListener(listener);
  }

  public void removeStatusListener(ChangeListener<ImportJobStatus> listener) {
    this.status.removeListener(listener);
  }

  /**
   * @param importer Importer to import with.
   */
  synchronized void setImporter(ImporterThread importer) {
    this.importer = importer;
  }

  /**
   * Cancels this job if it has not already been started.
   */
  public void cancel() {
    if (getStatus() == ImportJobStatus.WAITING) {
      final var url = getUrl();
      if (url != null) {
        LOGGER.info(() -> "Cancelling web import: " + url);
      } else {
        LOGGER.info(() -> "Cancelling local import: " + file);
      }

      importer.cancel(this);
    }
  }

}
