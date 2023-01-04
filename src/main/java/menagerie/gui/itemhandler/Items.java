package menagerie.gui.itemhandler;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.util.HashMap;

/**
 * Registry for implementations of item-type dependent functionality.
 * Allows for easy-er extension of functionality than hard-coded instanceof checks.
 * This does support inheritance in the expected way,
 * i.e. if a subclass has more specific functionality registered, this will be used.
 */
public class Items {

  // <Interface, <ItemType, Implementation>>
  private static final HashMap<Class<?>, HashMap<Class<? extends Item>, Object>> register;

  // Register new implementations here
  static {
    register = new HashMap<>();

    register(ItemInfoBoxRenderer.class, MediaItem.class, new MediaItemInfoBoxRenderer());
  }

  private Items() {
  }

  /**
   * Register an implementation of a interface for a specific item class.
   * @param interfaceClass Class of interface
   * @param itemType Class of item
   * @param interfaceImpl Class of interface implementation.
   * @param <T> Class of the interface.
   */
  private static <T> void register(Class<T> interfaceClass, Class<? extends Item> itemType, T interfaceImpl) {
    HashMap<Class<? extends Item>, Object> registeredImpls = register.computeIfAbsent(interfaceClass, k -> new HashMap<>());

    if (registeredImpls.get(itemType) != null) {
      throw new IllegalStateException("must not register more than one interface implementations for the same item type");
    }

    registeredImpls.put(itemType, interfaceImpl);
  }

  /**
   * Get an instance of the implementation registered for the provided interface and item type.
   * If no implementation is found for an item class, the superclass' will be returned (up until Item).
   * @param interfaceClass Interface of the desired implementation.
   * @param item Item instance (may be null).
   * @param <T> Class of interface.
   * @return Interface implementation, or null if none was registered.
   */
  private static <T> T get(Class<T> interfaceClass, Item item) {
    HashMap<Class<? extends Item>, Object> registeredImpls = register.get(interfaceClass);
    if (registeredImpls != null) {
      return getRecursive(registeredImpls, item != null ? item.getClass() : Item.class);
    }
    return null;
  }

  /**
   * Get registered implementation for specified item type. If none is found,
   * recursively trace the inheritance tree until Item.
   */
  private static <T> T getRecursive(HashMap<Class<? extends Item>, Object> registeredImpls,
                                    Class<? extends Item> itemClass) {
    if (registeredImpls != null) {
      T impl = (T) registeredImpls.get(itemClass);
      if (impl == null && itemClass != Item.class) {
        return getRecursive(registeredImpls, (Class<? extends Item>) itemClass.getSuperclass());
      }
      return impl;
    }
    return null;
  }

  public static ItemInfoBoxRenderer getItemInfoBoxRenderer(Item item) {
    return get(ItemInfoBoxRenderer.class, item);
  }
}
