package menagerie.gui.itemhandler;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.util.HashMap;

public class Items {

  // <Interface, <ItemType, Implementation>>
  private static final HashMap<Class<?>, HashMap<Class<? extends Item>, Object>> register;

  static {
    register = new HashMap<>();

    register(ItemInfoBoxRenderer.class, MediaItem.class, new MediaItemInfoBoxRenderer());
  }

  private Items() {
  }

  private static <T> void register(Class<T> interfaceClass, Class<? extends Item> itemType, T interfaceImpl) {
    HashMap<Class<? extends Item>, Object> registeredImpls = register.computeIfAbsent(interfaceClass, k -> new HashMap<>());
    registeredImpls.put(itemType, interfaceImpl);
  }

  private static <T> T get(Class<T> interfaceClass, Item item) {
    HashMap<Class<? extends Item>, Object> registeredImpls = register.get(interfaceClass);
    if (registeredImpls != null) {
      return (T) registeredImpls.get(item != null ? item.getClass() : null);
    }
    return null;
  }

  public static ItemInfoBoxRenderer getItemInfoBoxRenderer(Item item) {
    return get(ItemInfoBoxRenderer.class, item);
  }
}
