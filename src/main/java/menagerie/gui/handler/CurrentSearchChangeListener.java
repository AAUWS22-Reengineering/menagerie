package menagerie.gui.handler;

import java.util.List;
import java.util.function.Supplier;
import javafx.collections.ListChangeListener;
import menagerie.gui.grid.ItemGridView;
import menagerie.model.menagerie.Item;
import menagerie.util.Action;

public class CurrentSearchChangeListener implements ListChangeListener<Item> {

  private final ItemGridView itemGridView;
  private final Supplier<Item> currentlyPreviewing;
  private final Action resetPreview;

  public CurrentSearchChangeListener(ItemGridView itemGridView, Supplier<Item> currentlyPreviewing,
                                     Action resetPreview) {
    this.itemGridView = itemGridView;
    this.currentlyPreviewing = currentlyPreviewing;
    this.resetPreview = resetPreview;
  }

  @Override
  public void onChanged(Change<? extends Item> c) {
    while (c.next()) {
      // Added
      if (c.wasAdded()) {
        itemGridView.getItems().addAll(0,
            c.getAddedSubList()); // TODO: Insert these in the right position or sort it or something.
      }

      // Removed
      if (c.wasRemoved()) {
        if (c.getRemoved().contains(currentlyPreviewing.get())) {
          resetPreview.execute();
        }

        final int oldLastIndex = itemGridView.getItems().indexOf(itemGridView.getLastSelected());
        int newIndex = getNewIndex(c.getRemoved(), oldLastIndex);

        itemGridView.getItems().removeAll(c.getRemoved());

        updateGridViewSelection(newIndex);
      }
    }
  }

  private void updateGridViewSelection(int newIndex) {
    if (!itemGridView.getItems().isEmpty() && itemGridView.getSelected().isEmpty()) {
      if (newIndex >= itemGridView.getItems().size()) {
        newIndex = itemGridView.getItems().size() - 1;
      }
      if (newIndex >= 0) {
        itemGridView.select(itemGridView.getItems().get(newIndex), false, false);
      }
    }
  }

  private int getNewIndex(List<? extends Item> removedItems, int oldLastIndex) {
    int newIndex = oldLastIndex;
    for (Item item : removedItems) {
      final int i = itemGridView.getItems().indexOf(item);
      if (i < 0) {
        continue;
      }

      if (i < oldLastIndex) {
        newIndex--;
      }
    }
    return newIndex;
  }

}
