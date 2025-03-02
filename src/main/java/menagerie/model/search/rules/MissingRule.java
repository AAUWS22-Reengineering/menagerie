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

package menagerie.model.search.rules;

import menagerie.gui.itemhandler.Items;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.itemhandler.search.ItemSearch;

import java.util.Optional;

/**
 * Rule that searches for missing attributes.
 */
public class MissingRule extends SearchRule {

  public enum Type {
    MD5, FILE, HISTOGRAM
  }

  private final Type type;


  /**
   * @param type     Type of missing attribute.
   * @param inverted Negate this rule.
   */
  public MissingRule(Type type, boolean inverted) {
    super(inverted);
    this.type = type;
  }

  @Override
  protected boolean checkRule(Item item) {
    Optional<ItemSearch> is = Items.get(ItemSearch.class, item);
    return is.map(itemSearch -> switch (type) {
      case MD5 -> itemSearch.hasMissingMD5(item);
      case FILE -> itemSearch.hasMissingFile(item);
      case HISTOGRAM -> itemSearch.hasMissingHistogram(item);
    }).orElse(false);
  }

  @Override
  public String toString() {
    String result = "Missing Rule: " + type;
    if (isInverted()) {
      result += " [inverted]";
    }
    return result;
  }

}
