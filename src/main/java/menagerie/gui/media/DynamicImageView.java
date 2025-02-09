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

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * A dynamically sized image view that conforms to the parent node.
 * Does not display image larger than one-to-one size.
 */
public class DynamicImageView extends ImageView {

  public DynamicImageView() {
    super();
  }

  public DynamicImageView(Image img) {
    super(img);
  }

  @Override
  public double minWidth(double height) {
    return 40;
  }

  @Override
  public double prefWidth(double height) {
    Image img = getImage();
    if (img == null) {
      return minWidth(height);
    }
    return img.getWidth();
  }

  @Override
  public double maxWidth(double height) {
    return 16384;
  }

  @Override
  public double minHeight(double width) {
    return 40;
  }

  @Override
  public double prefHeight(double width) {
    Image img = getImage();
    if (img == null) {
      return minHeight(width);
    }
    return img.getHeight();
  }

  @Override
  public double maxHeight(double width) {
    return 16384;
  }

  @Override
  public boolean isResizable() {
    return true;
  }

  @Override
  public void resize(double width, double height) {
    if (getImage() == null) {
      setFitWidth(width);
      setFitHeight(height);
    } else {
      double scale = 1;
      if (scale * getImage().getWidth() > width) {
        scale = width / getImage().getWidth();
      }
      if (scale * getImage().getHeight() > height) {
        scale = height / getImage().getHeight();
      }

      setFitWidth((int) (getImage().getWidth() * scale));
      setFitHeight((int) (getImage().getHeight() * scale));
    }
  }

}