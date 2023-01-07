package menagerie.gui.grid;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import menagerie.model.menagerie.Item;

public class ItemGridSelectionChangeListener implements ListChangeListener<Item> {

  public static final String SELECTED = "selected";

  public void onChanged(Change<? extends Item> c) {
    while (c.next()) {
      for (Item item : c.getRemoved()) {
        Object obj = item.getMetadata().get(SELECTED);
        if (obj instanceof BooleanProperty booleanProperty) {
          booleanProperty.set(false);
        } else {
          item.getMetadata().put(SELECTED, new SimpleBooleanProperty(false));
        }
      }
      for (Item item : c.getAddedSubList()) {
        Object obj = item.getMetadata().get(SELECTED);
        if (obj instanceof BooleanProperty booleanProperty) {
          booleanProperty.set(true);
        } else {
          item.getMetadata().put(SELECTED, new SimpleBooleanProperty(true));
        }
      }
    }
  }
}
