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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import menagerie.model.menagerie.Menagerie;
import menagerie.settings.MenagerieSettings;
import menagerie.util.listeners.ObjectListener;

/**
 * A thread that cleanly serializes Menagerie imports as jobs with additional features.
 */
public class ImporterThread implements Runnable {

  private volatile boolean paused = false;

  private final ImportJobQueue queue;

  private final Set<ObjectListener<ImportJob>> importerListeners =
      Collections.synchronizedSet(new HashSet<>());


  public ImporterThread(Menagerie menagerie, MenagerieSettings settings) {
    this.queue = new ImportJobQueue(job -> job.runJob(menagerie, settings));
  }

  @Override
  public void run() {
    while (true) {

      while (paused) {
        sleep();
      }

      if (queue.isEmpty()) {
        sleep();
        continue;
      }

      queue.startNextImportJob();
    }
  }

  private void sleep() {
    synchronized (this) {
      try {
        wait();
      } catch (InterruptedException ignore) {
        // ignore since this is used in an endless loop
      }
    }
  }

  /**
   * Adds a job to the back of the queue. FIFO.
   *
   * @param job Job to add.
   */
  public synchronized void addJob(ImportJob job) {
    job.setImporter(this);
    queue.add(job);
    notifyAll();
    importerListeners.forEach(listener -> listener.pass(job));
  }

  /**
   * Sets the paused state of this import thread. If a job is already running, the paused state is not queried by this thread until the job finishes.
   *
   * @param paused Value
   */
  public synchronized void setPaused(boolean paused) {
    this.paused = paused;
    notifyAll();
  }

  /**
   * @return True if this thread is paused.
   */
  public synchronized boolean isPaused() {
    return paused;
  }

  /**
   * @param listener Listener that listens for jobs being added.
   */
  public void addImporterListener(ObjectListener<ImportJob> listener) {
    importerListeners.add(listener);
  }

  /**
   * Cancels a job, if it has not already been consumed/ran.
   *
   * @param job Job to remove.
   */
  void cancel(ImportJob job) {
    queue.remove(job);
  }

}
