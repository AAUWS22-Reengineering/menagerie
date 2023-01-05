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

package menagerie.model.search;

import menagerie.gui.itemhandler.Items;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.itemhandler.properties.ItemProperties;

import java.util.Comparator;
import java.util.Optional;

public class GroupSearch extends Search {

  private final GroupItem group;

  /**
   * Constructs a group search with given rules.
   *
   * @param search     User input string to parse.
   * @param group      Group scope of this search.
   * @param descending Sort the results descending.
   */
  public GroupSearch(String search, GroupItem group, boolean descending, boolean shuffled) {
    super(search, descending, true, shuffled);
    this.group = group;
  }

  @Override
  protected Comparator<Item> getItemComparator(boolean descending, boolean shuffled) {
    return (o1, o2) -> {
      if (o1 instanceof MediaItem && o2 instanceof MediaItem) {
        if (descending) {
          return ((MediaItem) o2).getPageIndex() - ((MediaItem) o1).getPageIndex();
        } else {
          return ((MediaItem) o1).getPageIndex() - ((MediaItem) o2).getPageIndex();
        }
      } else {
        return 0;
      }
    };
  }

  @Override
  protected boolean isItemValid(Item item) {
    Optional<ItemProperties> itemProps = Items.get(ItemProperties.class, item);
    if (itemProps.isEmpty() || !itemProps.get().isInGroup(item) || !itemProps.get().getParentGroup(item).equals(group)) {
      return false;
    }
    return super.isItemValid(item);
  }

  public GroupItem getGroup() {
    return group;
  }

}
