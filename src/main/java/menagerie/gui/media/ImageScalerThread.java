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

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import menagerie.util.CancellableThread;
import menagerie.util.listeners.ObjectListener;

import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageScalerThread extends CancellableThread {

  /**
   * Logger for this class
   */
  private static final Logger LOGGER = Logger.getLogger(ImageScalerThread.class.getName());

  /**
   * Queued source image to scale
   */
  private Image source = null;

  /**
   * Queued target scale
   */
  private double scale = 1;

  /**
   * Queued callback to call once scaling is complete for this image
   */
  private ObjectListener<Image> callback = null;

  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  @Override
  public void run() {
    while (running) {
      Image currentSource;
      double currentScale;
      ObjectListener<Image> currentCallback;

      // Loop until job is received
      while (true) {
        lock.lock();
        try {
          // Pop queue
          currentSource = this.source;
          currentScale = this.scale;
          currentCallback = this.callback;
          clear();

          if (currentSource == null || currentScale < 0 || currentCallback == null) {
            // Nothing in queue
            try {
              condition.await();
            } catch (InterruptedException ignore) {
              Thread.currentThread().interrupt();
            }
          } else {
            // Something in queue
            break;
          }
        } finally {
          lock.unlock();
        }
      }

      try {
        BufferedImage bimg = SwingFXUtils.fromFXImage(currentSource, null);

        ResampleOp resizeOp = new ResampleOp((int) (bimg.getWidth() / currentScale + 0.5),
            (int) (bimg.getHeight() / currentScale + 0.5));
        resizeOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
        resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
        BufferedImage scaledImage = resizeOp.filter(bimg, bimg);

        currentCallback.pass(SwingFXUtils.toFXImage(scaledImage, null));
      } catch (Throwable e) {
        LOGGER.log(Level.SEVERE,
            String.format(
                "Unexpected exception while scaling image (scale:%s, width:%s, height:%s, callback:%s)",
                currentScale, currentSource.getWidth(), currentSource.getHeight(), currentCallback), e);
      }
    }
  }

  /**
   * Clears the queue
   */
  public void clear() {
    lock.lock();
    try {
      source = null;
      scale = -1;
      callback = null;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Puts image scale job in queue
   *
   * @param source   Source image to scale
   * @param scale    Amount to scale by
   * @param callback Callback once complete
   */
  public void enqueue(Image source, double scale, ObjectListener<Image> callback) {
    lock.lock();
    try {
      this.source = source;
      this.scale = scale;
      this.callback = callback;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

}
