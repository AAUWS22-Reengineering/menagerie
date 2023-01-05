package menagerie.gui.itemhandler;

import menagerie.gui.itemhandler.gridcell.GroupItemCellHandler;
import menagerie.gui.itemhandler.gridcell.ItemCellHandler;
import menagerie.gui.itemhandler.gridcell.MediaItemCellHandler;
import menagerie.model.menagerie.itemhandler.properties.GroupItemProperties;
import menagerie.model.menagerie.itemhandler.properties.ItemProperties;
import menagerie.model.menagerie.itemhandler.properties.MediaItemProperties;
import menagerie.gui.itemhandler.gridviewselector.ItemGridViewSelector;
import menagerie.gui.itemhandler.gridviewselector.MediaItemGridViewSelector;
import menagerie.gui.itemhandler.infoboxrenderer.ItemInfoBoxRenderer;
import menagerie.gui.itemhandler.infoboxrenderer.MediaItemInfoBoxRenderer;
import menagerie.gui.itemhandler.opener.GroupItemOpener;
import menagerie.gui.itemhandler.opener.ItemOpener;
import menagerie.gui.itemhandler.opener.MediaItemOpener;
import menagerie.gui.itemhandler.preview.ItemPreview;
import menagerie.gui.itemhandler.preview.MediaItemPreview;
import menagerie.gui.itemhandler.rename.GroupItemRenamer;
import menagerie.gui.itemhandler.rename.ItemRenamer;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.itemhandler.similarity.ItemSimilarity;
import menagerie.model.menagerie.itemhandler.similarity.MediaItemSimilarity;

import java.util.HashMap;
import java.util.Optional;

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
    register(ItemOpener.class, GroupItem.class, new GroupItemOpener());
    register(ItemOpener.class, MediaItem.class, new MediaItemOpener());
    register(ItemGridViewSelector.class, MediaItem.class, new MediaItemGridViewSelector());
    register(ItemProperties.class, GroupItem.class, new GroupItemProperties());
    register(ItemProperties.class, MediaItem.class, new MediaItemProperties());
    register(ItemPreview.class, MediaItem.class, new MediaItemPreview());
    register(ItemRenamer.class, GroupItem.class, new GroupItemRenamer());
    register(ItemCellHandler.class, GroupItem.class, new GroupItemCellHandler());
    register(ItemCellHandler.class, MediaItem.class, new MediaItemCellHandler());
    register(ItemSimilarity.class, MediaItem.class, new MediaItemSimilarity());
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
  public static <T> void register(Class<T> interfaceClass, Class<? extends Item> itemType, T interfaceImpl) {
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
   * @return Optional of interface implementation.
   */
  public static <T> Optional<T> get(Class<T> interfaceClass, Item item) {
    HashMap<Class<? extends Item>, Object> registeredImpls = register.get(interfaceClass);
    if (registeredImpls != null) {
      return getRecursive(registeredImpls, item != null ? item.getClass() : Item.class);
    }
    return Optional.empty();
  }

  /**
   * Get registered implementation for specified item type. If none is found,
   * recursively trace the inheritance tree until Item.
   */
  private static <T> Optional<T> getRecursive(HashMap<Class<? extends Item>, Object> registeredImpls,
                                    Class<? extends Item> itemClass) {
    if (registeredImpls != null) {
      T impl = (T) registeredImpls.get(itemClass);
      if (impl == null && itemClass != Item.class) {
        return getRecursive(registeredImpls, (Class<? extends Item>) itemClass.getSuperclass());
      }
      return Optional.ofNullable(impl);
    }
    return Optional.empty();
  }
}
