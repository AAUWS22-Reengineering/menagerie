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

package menagerie.gui.screens.move;

import menagerie.gui.itemhandler.Items;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.itemhandler.properties.ItemProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FileMoveTree {

  private final List<FileMoveNode> roots = new ArrayList<>();

  public FileMoveTree() {
  }

  public FileMoveTree(List<Item> toAdd) {
    toAdd.forEach(this::addItem);
  }

  public List<FileMoveNode> getRoots() {
    return roots;
  }

  public void addItem(Item item) {
    Optional<ItemProperties> itemProps = Items.get(ItemProperties.class, item);
    if (itemProps.isEmpty() || !itemProps.get().isFileBased(item) ||
        itemProps.get().getFile(item) == null || !itemProps.get().getFile(item).exists()) {
      return;
    }

    // Get list of stops to traverse to reach the file
    List<File> stops = new ArrayList<>();
    List<File> systemRoots = Arrays.asList(File.listRoots());
    File file = itemProps.get().getFile(item).getParentFile();
    while (!systemRoots.contains(file)) {
      stops.add(0, file);
      file = file.getParentFile();
    }
    FileMoveNode node = getRootNode(file);

    // Get/create nodes until reached the correct parent node for the item
    for (File stop : stops) {
      FileMoveNode subnode = null;
      for (FileMoveNode n : node.getNodes()) {
        if (n.getFolder().equals(stop)) {
          subnode = n;
          break;
        }
      }

      if (subnode == null) {
        subnode = new FileMoveNode(stop);
        node.getNodes().add(subnode);
        subnode.setParent(node);
      }

      node = subnode;
    }

    // Add the item to the node that should contain it
    node.getItems().add(item);
  }

  private FileMoveNode getRootNode(File root) {
    FileMoveNode node = null;
    for (FileMoveNode n : roots) {
      if (n.getFolder().equals(root)) {
        node = n;
        break;
      }
    }
    if (node == null) {
      node = new FileMoveNode(root);
      roots.add(node);
    }
    return node;
  }

  public void clear() {
    roots.clear();
  }

}
