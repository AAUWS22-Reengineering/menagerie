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

package menagerie.gui;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menagerie.duplicates.DuplicateFinder;
import menagerie.gui.grid.ItemGridCell;
import menagerie.gui.grid.ItemGridView;
import menagerie.gui.handler.*;
import menagerie.gui.itemhandler.properties.ItemProperties;
import menagerie.gui.itemhandler.gridviewselector.ItemGridViewSelector;
import menagerie.gui.itemhandler.opener.ItemOpener;
import menagerie.gui.itemhandler.Items;
import menagerie.gui.itemhandler.preview.ItemPreview;
import menagerie.gui.itemhandler.rename.ItemRenamer;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.media.DynamicVideoView;
import menagerie.gui.predictive.PredictiveTextField;
import menagerie.gui.screens.HelpScreen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.SlideshowScreen;
import menagerie.gui.screens.TagListScreen;
import menagerie.gui.screens.dialogs.*;
import menagerie.gui.screens.duplicates.DuplicateOptionsScreen;
import menagerie.gui.screens.findonline.FindOnlineScreen;
import menagerie.gui.screens.importer.ImporterScreen;
import menagerie.gui.screens.log.LogListCell;
import menagerie.gui.screens.log.LogScreen;
import menagerie.gui.screens.move.MoveFilesScreen;
import menagerie.gui.screens.settings.SettingsScreen;
import menagerie.gui.taglist.*;
import menagerie.gui.util.*;
import menagerie.model.Plugins;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.*;
import menagerie.model.menagerie.db.DatabaseUtil;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.model.search.GroupSearch;
import menagerie.model.search.Search;
import menagerie.model.search.SearchHistory;
import menagerie.settings.MenagerieSettings;
import menagerie.util.CancellableThread;
import menagerie.util.FileUtil;
import menagerie.util.Filters;
import menagerie.util.folderwatcher.FolderWatcherThread;
import menagerie.util.folderwatcher.FolderWatcherUtil;
import menagerie.util.listeners.ObjectListener;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

  private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

  // ------------------------------- JFX -------------------------------------------
  @FXML
  private StackPane rootPane;
  @FXML
  private BorderPane explorerRootPane;
  @FXML
  private ScreenPane screenPane;
  @FXML
  private MenuBar menuBar;

  // Right side
  @FXML
  private BorderPane gridPane;
  @FXML
  private PredictiveTextField searchTextField;
  @FXML
  private ToggleButton listDescendingToggleButton;
  @FXML
  private ToggleButton showGroupedToggleButton;
  @FXML
  private ToggleButton shuffledSearchButton;
  @FXML
  private ItemGridView itemGridView;
  @FXML
  private Label resultCountLabel;
  @FXML
  private Label scopeLabel;
  @FXML
  private HBox scopeHBox;
  @FXML
  private Button importsButton;
  @FXML
  private Label dbUpdatesLabel;
  @FXML
  private Button logButton;
  @FXML
  private Button backButton;

  // Left side
  @FXML
  private PredictiveTextField editTagsTextField;
  @FXML
  private DynamicMediaView previewMediaView;
  @FXML
  private ItemInfoBox itemInfoBox;
  @FXML
  private ListView<Tag> tagListView;
  @FXML
  private Label explorerZoomLabel;

  // ----------------------------------- Screens -----------------------------------
  private TagListScreen tagListScreen;
  private HelpScreen helpScreen;
  private SlideshowScreen slideshowScreen;
  private SettingsScreen settingsScreen;
  private ImporterScreen importerScreen;
  private LogScreen logScreen;
  private DuplicateOptionsScreen duplicateOptionsScreen;
  private ImportDialogScreen importDialogScreen;
  private GroupDialogScreen groupDialogScreen;
  private MoveFilesScreen moveFilesScreen;
  private FindOnlineScreen findOnlineScreen;

  // --------------------------------- Menagerie vars ------------------------------
  /**
   * The Menagerie environment this application is using.
   */
  private final Menagerie menagerie;

  /**
   * History of tag edit events.
   */
  private final Deque<TagEditEvent> tagEditHistory = new ArrayDeque<>();

  /**
   * Loaded menagerie plugins.
   */
  private final Plugins plugins = new Plugins();

  // ------------------------------- Explorer screen vars --------------------------
  /**
   * Clipboard content object used by this application.
   */
  private final ClipboardContent clipboard = new ClipboardContent();

  /**
   * Current search that is active and being shown in the item grid.
   */
  private Search currentSearch = null;

  /**
   * Item that is currently being displayed in the preview viewport.
   */
  private Item currentlyPreviewing = null;

  /**
   * The last string that was used to edit tags.
   */
  private String lastEditTagString = null;

  /**
   * Variable used to track drag status of items from the item grid.
   */
  private boolean itemGridViewDragging = false;

  /**
   * Search history stack
   */
  private final Deque<SearchHistory> searchHistory = new ArrayDeque<>();

  /**
   * Tag listener used to update tag list for currently previewed item
   */
  private final ListChangeListener<Tag> previewTagListener = c -> {
    while (c.next()) {
      tagListView.getItems().addAll(c.getAddedSubList());
      tagListView.getItems().removeAll(c.getRemoved());
      tagListView.getItems().sort(Comparator.comparing(Tag::getName));
    }
  };

  /**
   * Search listener listening for items being added or removed from the current search
   */
  private ListChangeListener<Item> searchChangeListener;

  private Item getCurrentlyPreviewing() {
    return currentlyPreviewing;
  }

  /**
   * Log button pseudoclass for unread error state
   */
  private final PseudoClass logErrorPseudoClass = PseudoClass.getPseudoClass("error");

  /**
   * Log button pseudoclass for unread warning state
   */
  private final PseudoClass logWarningPseudoClass = PseudoClass.getPseudoClass("warning");

  /**
   * Flag set when an unread error log is present
   */
  private BooleanProperty logError;
  /**
   * Flag set when an unread warning log is present
   */
  private BooleanProperty logWarning;

  // --------------------------------- Threads -------------------------------------
  /**
   * Importer thread for the menagerie. Main pipeline for adding any new items.
   */
  private ImporterThread importer;

  /**
   * Current folder watcher thread, may be null. Thread monitors a folder for new files and sends them to the importer.
   */
  private FolderWatcherThread folderWatcherThread = null;

  // ---------------------------------- Settings var -------------------------------
  /**
   * Settings object used by this application.
   */
  private final MenagerieSettings settings;

  // ------------------------------ Video preview status ---------------------------
  /**
   * Variable used to track if a video should be played after the player regains focus.
   */
  private boolean playVideoAfterFocusGain = false;

  /**
   * Variable used to track if a video should be played after the explorer regains focus.
   */
  private boolean playVideoAfterExplorerEnabled = false;

  private long lastDBLabelUpdates = 0;

  // --------------------------------- Constructor ---------------------------------

  /**
   * Constructs this controller with a given environment
   *
   * @param menagerie Menagerie of the environment
   * @param settings  Settings for the controller and environment
   */
  MainController(Menagerie menagerie, MenagerieSettings settings) {
    this.menagerie = menagerie;
    this.settings = settings;
  }

  // ---------------------------------- Initializers -------------------------------

  /**
   * Initializes this controller and elements
   */
  @FXML
  private void initialize() {
    // Initialize the menagerie
    initImporterThread();

    // Init screens
    initScreens();

    plugins.initPlugins();

    initDatabaseUpdateCounter();

    // Things to run on first "tick"
    Platform.runLater(() -> {

      //Apply window props and listeners
      initWindowPropertiesAndListeners();

      initAltTabbingFix();

      //Init closeRequest handling on window
      initCloseEventHandler();

      applyDefaultSearch();
      initFolderWatcher();
      showHelpScreen();

      // Init user-accepted filetypes in filters
      initUserAcceptedFiletypes();
    });
  }

  private void initUserAcceptedFiletypes() {
    if (settings.userFileTypes.getValue() != null &&
        !settings.userFileTypes.getValue().trim().isEmpty()) {
      Filters.USER_EXTS.addAll(
          Arrays.asList(settings.userFileTypes.getValue().trim().split(" ")));
    }
    settings.userFileTypes.valueProperty().addListener((observable, oldValue, newValue) -> {
      Filters.USER_EXTS.clear();

      if (newValue != null && !newValue.isEmpty()) {
        Filters.USER_EXTS.addAll(Arrays.asList(newValue.trim().split(" ")));
      }
    });
  }

  private void initCloseEventHandler() {
    rootPane.getScene().getWindow().setOnCloseRequest(event -> cleanExit(false));
  }

  private void showHelpScreen() {
    if (settings.helpOnStart.getValue()) {
      screenPane.open(helpScreen);
      settings.helpOnStart.setValue(false);
    }
  }

  private void initFolderWatcher() {
    if (settings.autoImportGroup.isEnabled()) {
      startWatchingFolderForImages(settings.autoImportFolder.getValue(),
          settings.autoImportMove.getValue());
    }
  }

  private void applyDefaultSearch() {
    applySearch(null, null, listDescendingToggleButton.isSelected(),
        showGroupedToggleButton.isSelected(), shuffledSearchButton.isSelected());
  }

  private void initDatabaseUpdateCounter() {
    menagerie.getDatabaseManager().setQueueSizeListener(count -> {
      if (count == 0 || System.currentTimeMillis() - lastDBLabelUpdates > 17) {
        lastDBLabelUpdates = System.currentTimeMillis();
        Platform.runLater(() -> dbUpdatesLabel.setText("Queued DB updates: " + count));
      }
    });
  }

  /**
   * Initializes a workaround for the garbage implementation of alt tabbing with toolbar menus in JavaFX
   */
  private void initAltTabbingFix() {
    LOGGER.info("Initializing alt-tab fix");
    rootPane.getScene().addEventFilter(KeyEvent.KEY_RELEASED, event -> {
      // Workaround for alt-tabbing correctly
      if (event.getCode() == KeyCode.ALT) {
        if (menuBar.isFocused()) {
          itemGridView.requestFocus();
        } else {
          menuBar.requestFocus();
        }
        event.consume();
      }
    });
    rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ALT) {
        event.consume();
      }
    });
  }

  /**
   * Initializes screen objects for the ScreenPane
   */
  private void initScreens() {
    LOGGER.info("Initializing screens");

    // Main Screen
    logError = new UnreadLogFlag(logButton, logErrorPseudoClass, "error");
    logWarning = new UnreadLogFlag(logButton, logWarningPseudoClass, "warning");
    searchChangeListener = new CurrentSearchChangeListener(itemGridView,
        this::getCurrentlyPreviewing, () -> previewItem(null));

    // Explorer. Base screen.
    initExplorer();

    // SettingsScreen
    settingsScreen = new SettingsScreen();

    // TagListScreen
    tagListScreen = new TagListScreen();
    tagListScreen.setCellFactory(param -> {
      final ObjectListener<Tag> addListener = new TagListCellAddListener(searchTextField);
      final ObjectListener<Tag> removeListener = new TagListCellRemoveListener(searchTextField);
      TagListCell c = new TagListCell(addListener, removeListener);
      setTagListCellContextMenuEventListener(c);
      setTagListCellMouseClickEventListener(c);
      return c;
    });

    // SlideshowScreen
    initSlideshowScreen();

    // HelpScreen
    helpScreen = new HelpScreen();

    // DuplicatesScreen
    initDuplicatesScreen();

    // ImportScreen
    initImportScreen();

    // LogScreen
    initLogScreen();

    // ImportDialogScreen
    importDialogScreen = new ImportDialogScreen(settings, menagerie, importer);

    // GroupDialogScreen
    groupDialogScreen = new GroupDialogScreen();
    groupDialogScreen.tagTagmeProperty().bind(settings.tagTagme.valueProperty());

    // MoveFilesScreen
    moveFilesScreen = new MoveFilesScreen();

    // FindOnlineScreen
    findOnlineScreen = new FindOnlineScreen();
    findOnlineScreen.loadAheadProperty().bind(settings.onlineLoadAhead.valueProperty());

    // Init disabling explorer when screen is open
    screenPane.currentProperty().addListener(
        (observable, oldValue, newValue) -> explorerRootPane.setDisable(newValue != null));
  }

  private void setTagListCellMouseClickEventListener(TagListCell c) {
    c.setOnMouseClicked(new TagListCellMouseClickListener(c, searchTextField));
  }

  private void setTagListCellContextMenuEventListener(TagListCell c) {
    c.setOnContextMenuRequested(new TagListCellContextMenuEventListener(c, screenPane));
  }

  /**
   * Initializes the import screen, which displays queued/in-progress/failed imports
   */
  private void initImportScreen() {
    importerScreen = new ImporterScreen(importer,
        this::resolveDuplicates, this::selectItemInGridView);

    final PseudoClass hasImportsPseudoClass = PseudoClass.getPseudoClass("has-imports");
    final BooleanProperty hasImports = new SimpleBooleanProperty();
    hasImports.addListener(observable ->
        importsButton.pseudoClassStateChanged(hasImportsPseudoClass, hasImports.get()));
    importsButton.getStyleClass().addAll("imports-button");
    importerScreen.getListView().getItems()
        .addListener((ListChangeListener<? super ImportJob>) c -> Platform.runLater(() -> {
          int count = c.getList().size() + importerScreen.getSimilar().size();
          importsButton.setText("Imports: " + count);
          hasImports.set(count > 0);
        }));
    importerScreen.getSimilar().addListener(
        (ListChangeListener<? super SimilarPair<MediaItem>>) c -> Platform.runLater(() -> {
          int count = c.getList().size() + importerScreen.getListView().getItems().size();
          importsButton.setText("Imports: " + count);
          hasImports.set(count > 0);
        }));
  }

  private void resolveDuplicates(List<SimilarPair<MediaItem>> pairs) {
    duplicateOptionsScreen.getDuplicatesScreen().open(screenPane, menagerie, pairs);
  }

  /**
   * Initializes the general purpose duplicates screen
   */
  private void initDuplicatesScreen() {
    duplicateOptionsScreen = new DuplicateOptionsScreen(settings);
    duplicateOptionsScreen.getDuplicatesScreen().setSelectListener(this::selectItemInGridView);
    duplicateOptionsScreen.getDuplicatesScreen().preloadProperty()
        .bind(settings.duplicatePreload.valueProperty());
    duplicateOptionsScreen.getDuplicatesScreen().getLeftInfoBox().extendedProperty().addListener(
        (observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
    duplicateOptionsScreen.getDuplicatesScreen().getRightInfoBox().extendedProperty().addListener(
        (observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
  }

  /**
   * Initializes the slideshow screen
   */
  private void initSlideshowScreen() {
    slideshowScreen = new SlideshowScreen(item -> {
      slideshowScreen.close();
      selectItemInGridView(item);
    });
    slideshowScreen.getInfoBox().extendedProperty().addListener(
        (observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
    slideshowScreen.intervalProperty().bind(settings.slideshowInterval.valueProperty());
    slideshowScreen.preloadProperty().bind(settings.slideshowPreload.valueProperty());
  }

  /**
   * Initializes menagerie importer thread.
   */
  private void initImporterThread() {
    LOGGER.info("Starting importer thread");
    importer = new ImporterThread(menagerie, settings);
    importer.setDaemon(true);
    importer.start();

    settings.autoImportGroup.enabledProperty()
        .addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
          // Defer to later to ensure other settings get updated before any action is taken,
          // since this operation relies on other settings
          if (folderWatcherThread != null) {
            folderWatcherThread.stopWatching();
          }

          if (newValue) {
            startWatchingFolderForImages(settings.autoImportFolder.getValue(),
                settings.autoImportMove.getValue());
          }
        }));
  }

  /**
   * Initializes the log screen
   */
  private void initLogScreen() {
    logScreen = new LogScreen();
    logScreen.getListView().setCellFactory(param -> new LogListCell());
    logButton.getStyleClass().addAll("log-button");

    Main.MENAGERIE_LOGGER.addHandler(new LoggerHandler(logError, logWarning, logScreen));
  }

  /**
   * Initializes the core explorer interface
   */
  private void initExplorer() {
    // Set image grid width from settings
    GridPaneUtil.setGridWidth(settings.gridWidth.getValue(), itemGridView, gridPane);

    // Init image grid
    initExplorerItemGridView();

    // Init drag/drop handlers
    explorerRootPane.disabledProperty().addListener(
        (observable1, oldValue1, newValue1) -> explorerRootPaneDisabledChanged(newValue1));
    explorerRootPane.setOnDragOver(this::explorerRootPaneOnDragOver);
    explorerRootPane.setOnDragDropped(this::explorerRootPaneOnDragDropped);

    // Init tag list cell factory
    initExplorerTagListCellFactory();

    // Init predictive textfields
    initPredictiveTextFields();

    initPreview();

    initItemInfoBox();

    initScopeLabelBinding();

    initSearchButtonIcons();

    // Init settings listeners
    settings.gridWidth.valueProperty()
        .addListener((observable, oldValue, newValue) -> GridPaneUtil
            .setGridWidth(newValue.intValue(), itemGridView, gridPane));
    settings.muteVideo.valueProperty()
        .addListener((observable, oldValue, newValue) -> previewMediaView.setMute(newValue));
    settings.repeatVideo.valueProperty()
        .addListener((observable, oldValue, newValue) -> previewMediaView.setRepeat(newValue));
  }

  private void initSearchButtonIcons() {
    listDescendingToggleButton.setGraphic(new ImageView(Icons.getInstance().getDescendingIcon()));
    listDescendingToggleButton.setTooltip(new Tooltip("Descending order"));
    showGroupedToggleButton.setGraphic(new ImageView(Icons.getInstance().getOpenGroupsIcon()));
    showGroupedToggleButton.setTooltip(new Tooltip("Show group elements"));
    shuffledSearchButton.setGraphic(new ImageView(Icons.getInstance().getShuffleIcon()));
    shuffledSearchButton.setTooltip(new Tooltip("Shuffle results"));
  }

  private void initScopeLabelBinding() {
    scopeLabel.maxWidthProperty().bind(
        scopeHBox.widthProperty().subtract(backButton.widthProperty())
            .subtract(scopeHBox.getSpacing()));
  }

  private void initItemInfoBox() {
    itemInfoBox.extendedProperty().addListener(
        (observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
    settings.expandItemInfo.valueProperty().addListener((observable, oldValue, newValue) -> {
      itemInfoBox.setExtended(newValue);
      slideshowScreen.getInfoBox().setExtended(newValue);
      duplicateOptionsScreen.getDuplicatesScreen().getLeftInfoBox().setExtended(newValue);
      duplicateOptionsScreen.getDuplicatesScreen().getRightInfoBox().setExtended(newValue);
    });
  }

  private void initPreview() {
    previewMediaView.getImageView().getScale().addListener((observable, oldValue, newValue) -> {
      if (newValue.doubleValue() == 1) {
        explorerZoomLabel.setText(null);
      } else {
        explorerZoomLabel.setText(
            String.format("%d%%", (int) (100 * (1 / newValue.doubleValue()))));
      }
    });
    previewMediaView.setMute(settings.muteVideo.getValue());
    previewMediaView.setRepeat(settings.repeatVideo.getValue());
  }

  /**
   * Initializes the predictive text fields with option listeners
   */
  private void initPredictiveTextFields() {
    initEditTagsTextField();
    initSearchTextField();
  }

  private void initSearchTextField() {
    searchTextField.setTop(false);
    searchTextField.setOptionsListener(new SearchTextFieldOptionsListener(menagerie));
  }

  private void initEditTagsTextField() {
    editTagsTextField.setOptionsListener(
        new EditTagsTextFieldOptionsListener(menagerie, itemGridView));
  }

  /**
   * Initializes the cell factory for the explorer tag list
   */
  private void initExplorerTagListCellFactory() {
    tagListView.setCellFactory(param -> {
      TagListCell c = new TagListCell(
          new TagListCellAddListener(searchTextField),
          new TagListCellRemoveListener(searchTextField));
      registerCellOnContextMenuEvent(c);
      registerCellOnMouseEvent(c);
      return c;
    });
  }

  private void registerCellOnContextMenuEvent(TagListCell c) {
    c.setOnContextMenuRequested(new TagListCellContextMenuListener(c, screenPane,
        e1 -> tagSelectedAction(c), e2 -> untagSelectedAction(c)));
  }

  private void untagSelectedAction(TagListCell c) {
    Map<Item, List<Tag>> removed = TagUtil.removeTags(c, itemGridView);
    tagEditHistory.push(new TagEditEvent(null, removed));
  }

  private void tagSelectedAction(TagListCell c) {
    Map<Item, List<Tag>> added = TagUtil.addTags(c, itemGridView);
    tagEditHistory.push(new TagEditEvent(added, null));
  }

  private void registerCellOnMouseEvent(TagListCell c) {
    c.setOnMouseClicked(new TagListCellMouseClickListener(c, searchTextField));
  }

  /**
   * Initializes listeners and etc. of the itemGridView
   */
  private void initExplorerItemGridView() {
    itemGridView.addSelectionListener(image -> Platform.runLater(() -> previewItem(image)));
    itemGridView.setCellFactory(param -> {
      ItemGridCell c = new ItemGridCell();
      registerCellDragDetectedEvent(c);
      registerCellDragDoneEvent(c);
      registerCellDragOverEvent(c);
      registerCellDragDroppedEvent(c);
      registerCellMouseReleasedEvent(c);
      registerCellContextMenuEvent(c);
      registerCellMouseClickEvent(c);
      return c;
    });
    itemGridView.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.DELETE) {
        if (event.isControlDown()) {
          forgetFilesDialog(itemGridView.getSelected());
        } else {
          deleteFilesDialog(itemGridView.getSelected());
        }
        event.consume();
      } else if (event.getCode() == KeyCode.G && event.isControlDown()) {
        groupDialog(itemGridView.getSelected());
      } else if (event.getCode() == KeyCode.U && event.isControlDown()) {
        ungroupDialog(itemGridView.getSelected());
      }
    });
    itemGridView.getSelected().addListener(
        (ListChangeListener<? super Item>) c -> resultCountLabel.setText(
            itemGridView.getSelected().size() + " / " + currentSearch.getResults().size()));
    itemGridView.getItems().addListener((ListChangeListener<? super Item>) c -> {
      while (c.next()) {
        if (c.wasAdded()) {
          itemGridView.getItems().sort(currentSearch.getComparator());
          break;
        }
      }
    });
  }

  private void registerCellMouseClickEvent(ItemGridCell c) {
    c.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
        Items.get(ItemOpener.class, c.getItem()).
            ifPresent(itemOpener -> itemOpener.open(c.getItem(), this));
      }
    });
  }

  private void registerCellContextMenuEvent(ItemGridCell c) {
    c.setOnContextMenuRequested(event -> {
      if (!itemGridView.isSelected(c.getItem())) {
        itemGridView.select(c.getItem(), false, false);
      }
      constructGridCellContextMenu(itemGridView.getSelected()).show(c.getScene().getWindow(),
          event.getScreenX(), event.getScreenY());
      event.consume();
    });
  }

  private void registerCellMouseReleasedEvent(ItemGridCell c) {
    c.setOnMouseReleased(event -> {
      if (!itemGridViewDragging && event.getButton() == MouseButton.PRIMARY) {
        itemGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());
        event.consume();
      }
    });
  }

  private void registerCellDragDroppedEvent(ItemGridCell c) {
    c.setOnDragDropped(event -> {
      if (event.getGestureSource() instanceof ItemGridCell &&
          !itemGridView.getSelected().isEmpty()) {
        List<MediaItem> list = new ArrayList<>();
        itemGridView.getSelected().forEach(item -> list.add((MediaItem) item));
        list.sort(Comparator.comparingInt(MediaItem::getPageIndex));

        boolean before = false;
        if (c.sceneToLocal(event.getSceneX(), event.getSceneY()).getX() <
            (double) Thumbnail.THUMBNAIL_SIZE / 2) {
          before = true;
        }
        if (currentSearch.isDescending()) {
          before = !before;
        }
        if (((MediaItem) c.getItem()).getGroup()
            .moveElements(list, (MediaItem) c.getItem(), before)) {
          currentSearch.sort();
          itemGridView.getItems().sort(currentSearch.getComparator());
          event.consume();
        }
      }
    });
  }

  private void registerCellDragOverEvent(ItemGridCell c) {
    c.setOnDragOver(event -> {
      if (event.getGestureSource() instanceof ItemGridCell &&
          currentSearch instanceof GroupSearch && !event.getGestureSource().equals(c)) {
        event.acceptTransferModes(TransferMode.ANY);
        event.consume();
      }
    });
  }

  private void registerCellDragDoneEvent(ItemGridCell c) {
    c.setOnDragDone(event -> {
      itemGridViewDragging = false;
      event.consume();
    });
  }

  private void registerCellDragDetectedEvent(ItemGridCell c) {
    c.setOnDragDetected(event -> {
      if (!itemGridView.getSelected().isEmpty() && event.isPrimaryButtonDown()) {
        Items.get(ItemGridViewSelector.class, c.getItem()).
            ifPresent(itemGridViewSelector -> itemGridViewSelector.select(c.getItem(), itemGridView, event));

        Dragboard db = c.startDragAndDrop(TransferMode.ANY);
        GridViewUtil.doDragAndDrop(db, itemGridView);

        List<File> files = GridViewUtil.getSelectedFiles(itemGridView);
        clipboard.putFiles(files);
        db.setContent(clipboard);

        itemGridViewDragging = true;
        event.consume();
      }
    });
  }

  /**
   * Applies window properties to the window and starts listeners for changes to update the settings objects
   */
  private void initWindowPropertiesAndListeners() {
    LOGGER.info("Initializing window properties and listeners");

    Stage stage = ((Stage) explorerRootPane.getScene().getWindow());
    final var rootPaneSettings = new RootPaneSettings(stage, settings);
    rootPaneSettings.synchronize();
    rootPaneSettings.bind();

    stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        if (previewMediaView.isPlaying()) {
          previewMediaView.pause();
          playVideoAfterFocusGain = true;
        }
      } else if (playVideoAfterFocusGain) {
        previewMediaView.play();
        playVideoAfterFocusGain = false;
      }
    });
  }

  // ---------------------------------- GUI Action Methods ---------------------------

  /**
   * Constructs a context menu for a given set of items. Different combinations of GroupItem and MediaItem will give different context menu items.
   *
   * @param selected Set of items that this context menu with operate on.
   * @return A context menu ready to be shown for the given set of items.
   */
  private ContextMenu constructGridCellContextMenu(List<Item> selected) {
    if (selected == null || selected.isEmpty()) {
      MenuItem mi = new MenuItem("Nothing selected");
      mi.setDisable(true);
      return new ContextMenu(mi);
    }

    ContextMenu cm = new ContextMenu();

    int groupCount = 0, mediaCount = 0, itemsInGroupCount = 0;
    for (Item item : selected) {
      Optional<ItemProperties> itemProps = Items.get(ItemProperties.class, item);
      if (itemProps.isPresent()) {
        groupCount += itemProps.get().isGroup(item) ? 1 : 0;
        mediaCount += itemProps.get().isMedia(item) ? 1 : 0;
        itemsInGroupCount += itemProps.get().isInGroup(item) ? 1 : 0;
      }
    }

    if (groupCount == 1 && selected.size() == 1) {
      singleGroupCount(selected, cm);
    }
    if (groupCount > 1 || mediaCount > 0) {
      combineGroups(selected, cm);
    }
    if (itemsInGroupCount > 0) {
      ItemUtil.removeFromGroup(selected, cm);
    }

    if (groupCount > 0 || mediaCount > 0) {
      MenuItem grabbed = new MenuItem("Selected");
      grabbed.setOnAction(event -> openSlideShow(selected, false));
      MenuItem searched = new MenuItem("Searched");
      searched.setOnAction(event -> openSlideShow(currentSearch.getResults(), true));
      MenuItem all = new MenuItem("All");
      all.setOnAction(event -> openSlideShow(menagerie.getItems(), true));
      Menu slideshow = new Menu("Slideshow...", null, grabbed, searched, all);

      MenuItem moveFiles = new MenuItem("Move Files");
      moveFiles.setOnAction(event -> moveFilesScreen.open(screenPane, selected));

      MenuItem findOnline = new MenuItem("Online");
      findOnline.setOnAction(event -> findOnlineDialog(selected));
      MenuItem findDupes = new MenuItem("Duplicates");
      findDupes.setOnAction(event -> duplicateOptionsScreen.open(screenPane, menagerie, selected,
          currentSearch.getResults(), menagerie.getItems()));
      Menu find = new Menu("Find...", null, findOnline, findDupes);

      cm.getItems().addAll(find, slideshow, moveFiles);
    }

    if (mediaCount > 0) {
      MenuItem openDefault = new MenuItem("Open");
      openDefault.setOnAction(event -> FileExplorer.openDefault(selected));
      MenuItem explorer = new MenuItem("Open in Explorer");
      explorer.setOnAction(event -> FileExplorer.openExplorer(selected));
      cm.getItems().addAll(openDefault, explorer);
    }

    if (groupCount > 0 || mediaCount > 0) {
      ungroup(selected, cm, groupCount);
    }

    return cm;
  }

  private void ungroup(List<Item> selected, ContextMenu cm, int groupCount) {
    cm.getItems().add(new SeparatorMenuItem());
    if (groupCount > 0) {
      MenuItem ungroup = new MenuItem("Ungroup");
      ungroup.setOnAction(event -> ungroupDialog(selected));
      cm.getItems().add(ungroup);
    }
    MenuItem forget = new MenuItem("Forget files");
    forget.setOnAction(event -> forgetFilesDialog(selected));
    forget.setStyle("-fx-text-fill: red;");
    MenuItem delete = new MenuItem("Delete files");
    delete.setOnAction(event -> deleteFilesDialog(selected));
    delete.setStyle("-fx-text-fill: red;");
    cm.getItems().addAll(forget, delete);
  }

  private void combineGroups(List<Item> selected, ContextMenu cm) {
    MenuItem combineGroups = new MenuItem("Combine into Group");
    combineGroups.setOnAction(event -> groupDialog(selected));
    cm.getItems().add(combineGroups);
  }

  private void singleGroupCount(List<Item> selected, ContextMenu cm) {
    MenuItem elementTags = new MenuItem("Sync element tags to group");
    elementTags.setOnAction(event -> {
      GroupItem group = (GroupItem) selected.get(0);
      group.getElements().forEach(item -> item.getTags().forEach(group::addTag));
    });
    cm.getItems().add(elementTags);
    MenuItem reverse = new MenuItem("Reverse element order");
    reverse.setOnAction(event -> ((GroupItem) selected.get(0)).reverseElements());
    cm.getItems().add(reverse);
    MenuItem rename = new MenuItem("Rename group");
    rename.setOnAction(event -> openGroupRenameDialog((GroupItem) selected.get(0)));
    cm.getItems().add(rename);
    cm.getItems().add(new SeparatorMenuItem());
  }

  /**
   * Opens a dialog to allow the user to rename a group
   *
   * @param group Group to rename
   */
  public void openGroupRenameDialog(GroupItem group) {
    new TextDialogScreen().open(screenPane, "Rename group", "Current: " + group.getTitle(),
        group.getTitle(), group::setTitle, null);
  }

  /**
   * Attempts to display an item's media in the preview viewport.
   *
   * @param item The item to display. Displays nothing when item is a GroupItem.
   */
  private void previewItem(Item item) {
    if (currentlyPreviewing != null) {
      currentlyPreviewing.getTags().removeListener(previewTagListener);
    }
    currentlyPreviewing = item;

    previewMediaView.preview(item);
    itemInfoBox.setItem(item);

    tagListView.getItems().clear();
    if (item != null) {
      tagListView.getItems().addAll(item.getTags());
      tagListView.getItems().sort(Comparator.comparing(Tag::getName));
    }

    if (item != null) {
      item.getTags().addListener(previewTagListener);
    }
  }

  /**
   * Opens the "find online" dialog. Uses existing plugins to reverse image search and find duplicates online.
   *
   * @param selected Items to search online for
   */
  private void findOnlineDialog(List<Item> selected) {
    List<MediaItem> items = ItemUtil.flattenGroups(selected);
    List<DuplicateFinder> finders = plugins.getAllDuplicateFinders();
    findOnlineScreen.open(screenPane, items, finders, this::selectItemInGridView);
  }

  /**
   * Opens a dialog asking for user confirmation to forget files from the menagerie without deleting the file.
   *
   * @param toForget Set of items to forget if user confirms.
   */
  private void forgetFilesDialog(List<Item> toForget) {
    List<Item> items = new ArrayList<>(toForget);
    ItemUtil.addGroupElements(items);
    stopPreview(items);
    new ConfirmationScreen().open(screenPane, "Forget files", String.format(
        "Remove selected files from database? (%d files)\n\n" + "This action CANNOT be undone",
        items.size()), () -> menagerie.forgetItems(items), null);
  }

  private void stopPreview(List<Item> items) {
    if (items.contains(currentlyPreviewing)) {
      Items.get(ItemPreview.class, currentlyPreviewing).ifPresent(itemPreview ->
          itemPreview.stop(previewMediaView, currentlyPreviewing));
    }
  }

  /**
   * Opens a dialog asking for user confirmation to delete files from the menagerie.
   * Will delete files from the local disk.
   *
   * @param toDelete Set of items to forget and delete if user confirms.
   */
  private void deleteFilesDialog(List<Item> toDelete) {
    List<Item> items = new ArrayList<>(toDelete);
    ItemUtil.addGroupElements(items);
    stopPreview(items);
    new ConfirmationScreen().open(screenPane, "Delete files", """
            Permanently delete selected files? (%d files)
            This action CANNOT be undone (files will be deleted)""".formatted(items.size()),
        () -> menagerie.deleteItems(items), null);
  }

  // REENG: improve dialog handling (e.g. new ConfirmationScreen() ...)

  /**
   * Opens a dialog asking for user confirmation to ungroup given groups.
   * Items that are not a GroupItem will be ignored.
   *
   * @param items Set of items to ungroup if user confirms.
   */
  private void ungroupDialog(List<Item> items) {
    List<GroupItem> groups = ItemUtil.getGroupItems(items);
    new ConfirmationScreen().open(screenPane, "Ungroup group?",
        String.format("Are you sure you want to ungroup %d groups?", groups.size()),
        () -> groups.forEach(GroupItem::ungroup), null);
  }

  /**
   * Opens a dialog asking for user confirmation to group items into a new group.
   * GroupItems will be merged.
   *
   * @param toGroup Set of items to group if user confirms.
   */
  private void groupDialog(List<Item> toGroup) {
    String title = ItemUtil.getFirstGroupTitle(toGroup);
    groupDialogScreen.open(screenPane, menagerie, title, toGroup, group -> {
      LOGGER.info("Created group: " + group);
      Platform.runLater(() -> {
        if (currentSearch.getResults().contains(group)) {
          itemGridView.select(group, false, false);
        }
      });
    });
  }

  /**
   * Opens the log screen and clears error/warning flags
   */
  private void openLog() {
    logError.set(false);
    logWarning.set(false);
    screenPane.open(logScreen);
  }

  /**
   * Opens the slideshow screen.
   *
   * @param items Set of items to show in the slideshow.
   */
  private void openSlideShow(List<Item> items, boolean reversed) {
    items = ItemUtil.flattenGroups(items, reversed);
    slideshowScreen.open(screenPane, menagerie, items);
  }

  /**
   * Attempts to revert to the previous search.
   */
  private void explorerGoBack() {
    if (searchHistory.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }

    SearchHistory history = searchHistory.pop();

    listDescendingToggleButton.setSelected(history.isDescending());
    showGroupedToggleButton.setSelected(history.isShowGrouped());
    shuffledSearchButton.setSelected(history.isShuffled());
    searchTextField.setText(history.getSearch());
    applySearch(history.getSearch(), history.getGroupScope(), history.isDescending(),
        history.isShowGrouped(), history.isShuffled());
    searchHistory.pop(); // Pop history item that was JUST created by the new search.

    if (searchHistory.isEmpty()) {
      backButton.setDisable(true);
    }

    itemGridView.clearSelection();
    itemGridView.getSelected().addAll(history.getSelected());
    if (history.getSelected() != null && !history.getSelected().isEmpty()) {
      // Unselect and reselect last item
      itemGridView.select(history.getSelected().get(history.getSelected().size() - 1), true,
          false);
      itemGridView.select(history.getSelected().get(history.getSelected().size() - 1), true,
          false);
    }
  }

  /**
   * Sets the search scope to a group search and applies an empty search.
   *
   * @param group Group scope.
   */
  public void explorerOpenGroup(GroupItem group) {
    searchTextField.setText(null);
    if (settings.explorerGroupAscending.getValue()) {
      listDescendingToggleButton.setSelected(false);
    }
    applySearch(searchTextField.getText(), group, listDescendingToggleButton.isSelected(), true,
        false);
  }

  /**
   * Parses a search string, applies the search, updates grid, registers search listeners, and previews first item.
   *
   * @param search      Search string to parse rules from.
   * @param descending  Order results in descending order.
   * @param showGrouped Show MediaItems that are in a group.
   */
  private void applySearch(String search, GroupItem groupScope, boolean descending,
                           boolean showGrouped, boolean shuffled) {
    LOGGER.info(() ->
        "Searching: \"" + search + "\", group:" + groupScope + ", descending:" + descending +
            ", showGrouped:" + showGrouped + ", shuffled:" + shuffled);

    cleanupPreviousSearch();

    // Create new search
    boolean inGroup = groupScope != null;
    shuffledSearchButton.setDisable(inGroup);
    shuffledSearchButton.setSelected(shuffled && !inGroup);
    showGroupedToggleButton.setDisable(inGroup);
    showGroupedToggleButton.setSelected(showGrouped || inGroup);
    if (inGroup) {
      currentSearch = new GroupSearch(search, groupScope, descending, shuffled);
      scopeLabel.setText("Scope: " + groupScope.getTitle());
      Tooltip tt = new Tooltip(groupScope.getTitle());
      tt.setWrapText(true);
      scopeLabel.setTooltip(tt);
    } else {
      currentSearch = new Search(search, descending, showGrouped, shuffled);
      scopeLabel.setText("Scope: All");
      scopeLabel.setTooltip(null);
    }
    menagerie.registerSearch(currentSearch);
    currentSearch.refreshSearch(menagerie.getItems());
    currentSearch.getResults().addListener(searchChangeListener);

    itemGridView.clearSelection();
    itemGridView.getItems().clear();
    itemGridView.getItems().addAll(currentSearch.getResults());

    if (!itemGridView.getItems().isEmpty()) {
      itemGridView.select(itemGridView.getItems().get(0), false, false);
    }
  }

  private void cleanupPreviousSearch() {
    if (currentSearch != null) {
      GroupItem scope = null;
      if (currentSearch instanceof GroupSearch) {
        scope = ((GroupSearch) currentSearch).getGroup();
      }
      searchHistory.push(
          new SearchHistory(currentSearch.getSearchString(), scope, itemGridView.getSelected(),
              currentSearch.isDescending(), currentSearch.isShowGrouped(),
              currentSearch.isShuffled()));
      backButton.setDisable(false);

      menagerie.unregisterSearch(currentSearch);
      currentSearch.getResults().removeListener(searchChangeListener);
    }
    previewItem(null);
  }

  /**
   * Parses a string and applies tag edits to currently selected items.
   * Opens a confirmation dialog if 100 or more items will be modified by this operation.
   *
   * @param input Tag edit string.
   */
  private void editTagsOfSelected(String input) {
    if (input == null || input.isEmpty() || itemGridView.getSelected().isEmpty()) {
      return;
    }
    lastEditTagString = input.trim();

    if (itemGridView.getSelected().size() < 100) {
      editTagsUtility(input);
    } else {
      new ConfirmationScreen().open(screenPane, "Editing large number of items",
          "You are attempting to edit " + itemGridView.getSelected().size() + " items. Continue?",
          () -> editTagsUtility(input), null);
    }
  }

  // REENG: still too big

  /**
   * Actual workhorse tag editing method. Parses tag edit string, makes changes, and verifies changed items against the search.
   *
   * @param input Tag edit string.
   */
  private void editTagsUtility(String input) {
    List<Item> changed = new ArrayList<>();
    Map<Item, List<Tag>> added = new HashMap<>();
    Map<Item, List<Tag>> removed = new HashMap<>();

    final var inputTokens = input.split("\\s+");
    for (String token : inputTokens) {
      if (token.startsWith("-")) {
        Tag t = menagerie.getTagByName(token.substring(1));
        if (t != null) {
          for (Item item : itemGridView.getSelected()) {
            if (item.removeTag(t)) {
              changed.add(item);
              removed.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
            }
          }
        }
      } else {
        Tag t = menagerie.getTagByName(token);
        if (t == null) {
          t = menagerie.createTag(token);
        }
        for (Item item : itemGridView.getSelected()) {
          if (item.addTag(t)) {
            changed.add(item);
            added.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
          }
        }
      }
    }

    if (!changed.isEmpty()) {
      menagerie.refreshInSearches(changed);
      tagEditHistory.push(new TagEditEvent(added, removed));
    }
  }

  // REENG: extract as well?

  /**
   * Selects an item in the grid view. If the item is hidden in a group, selects the group instead.
   *
   * @param item Item to select
   */
  private void selectItemInGridView(Item item) {
    Optional<ItemProperties> itemProps = Items.get(ItemProperties.class, item);
    if (itemGridView.getItems().contains(item)) {
      itemGridView.select(item, false, false);
    } else if (itemProps.isPresent() && itemProps.get().isInGroup(item)) {
      itemGridView.select(itemProps.get().getParentGroup(item), false, false);
    } else {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  // ---------------------------------- Compute Utilities -----------------------------

  /**
   * Starts a folder watcher thread. Kills an active folder watcher thread first, if present.
   *
   * @param folder        Target folder to watch for new files.
   * @param moveToDefault Move found files to default folder as specified by settings object.
   */
  private void startWatchingFolderForImages(String folder, boolean moveToDefault) {
    File watchFolder = new File(folder);
    if (watchFolder.exists() && watchFolder.isDirectory()) {
      LOGGER.info(() -> "Starting folder watcher in folder: " + watchFolder);
      folderWatcherThread =
          new FolderWatcherThread(watchFolder, Filters.FILE_NAME_FILTER, 30000,
              files -> FolderWatcherUtil.folderWatchListener(moveToDefault, files,
                  settings.defaultFolder.getValue(), importer));
      folderWatcherThread.setDaemon(true);
      folderWatcherThread.start();
    }
  }

  /**
   * Cleanly exits the JFX application and releases all threads and resources.
   *
   * @param revertDatabase Revert database to last backup.
   */
  private void cleanExit(boolean revertDatabase) {
    LOGGER.info("Attempting clean exit");

    Platform.runLater(() -> {
      rootPane.getChildren().clear();
      Platform.exit();
    });

    DynamicVideoView.releaseAllVLCJ();
    Thumbnail.releaseVLCJResources();

    plugins.closeAll();
    settings.save();
    DatabaseUtil.shutDownDatabase(revertDatabase, menagerie, settings.dbUrl.getValue());
  }

  // ---------------------------------- Action Event Handlers --------------------------

  @FXML
  private void searchButtonOnAction(ActionEvent event) {
    searchOnAction(event);
  }

  @FXML
  private void searchTextFieldOnAction(ActionEvent event) {
    searchOnAction(event);
  }

  private void searchOnAction(ActionEvent event) {
    GroupItem scope = null;
    if (currentSearch instanceof GroupSearch) {
      scope = ((GroupSearch) currentSearch).getGroup();
    }
    applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(),
        showGroupedToggleButton.isSelected(), shuffledSearchButton.isSelected());
    itemGridView.requestFocus();
    event.consume();
  }

  @FXML
  private void importFilesMenuButtonOnAction(ActionEvent event) {
    screenPane.open(importDialogScreen);
    event.consume();
  }

  @FXML
  private void pruneFileLessMenuButtonOnAction(ActionEvent event) {
    ProgressScreen ps = new ProgressScreen();
    CancellableThread ct = new PruneFileLessMenuButtonAction(menagerie, ps, screenPane);
    ps.open(screenPane, "Pruning Items",
        "Finding and pruning items that have become detached from their file...", ct::cancel);
    ct.setName("Fileless Pruner");
    ct.setDaemon(true);
    ct.start();

    event.consume();
  }

  @FXML
  private void rebuildSimilarityCacheMenuButtonOnAction(ActionEvent event) {
    ProgressScreen ps = new ProgressScreen();
    CancellableThread ct = new RebuildSimilarityCacheMenuButtonAction(menagerie, ps);
    ps.open(screenPane, "Building similarity cache",
        "Caching items that have no possible similar items", ct::cancel);
    ct.setDaemon(true);
    ct.setName("Similarity Cache Builder");
    ct.start();

    event.consume();
  }

  @FXML
  private void settingsMenuButtonOnAction(ActionEvent event) {
    settingsScreen.open(screenPane, settings);
    event.consume();
  }

  @FXML
  private void helpMenuButtonOnAction(ActionEvent event) {
    screenPane.open(helpScreen);
    event.consume();
  }

  @FXML
  private void viewSlideShowSearchedMenuButtonOnAction(ActionEvent event) {
    slideshowScreen.open(screenPane, menagerie, currentSearch.getResults());
    event.consume();
  }

  @FXML
  private void viewSlideShowSelectedMenuButtonOnAction(ActionEvent event) {
    slideshowScreen.open(screenPane, menagerie, itemGridView.getSelected());
    event.consume();
  }

  @FXML
  private void viewSlideShowAllMenuButtonOnAction(ActionEvent event) {
    slideshowScreen.open(screenPane, menagerie, menagerie.getItems());
    event.consume();
  }

  @FXML
  private void viewTagsMenuButtonOnAction(ActionEvent event) {
    tagListScreen.open(screenPane, menagerie.getTags());
    event.consume();
  }

  @FXML
  private void revertDatabaseMenuButtonOnAction(ActionEvent event) {
    File database = DatabaseUtil.resolveDatabaseFile(settings.dbUrl.getValue());
    File backup = new File(database + ".bak");
    if (backup.exists()) {
      new ConfirmationScreen().open(screenPane, "Revert database",
          "Revert to latest backup? (" + new Date(backup.lastModified()) +
              ")\n\nLatest backup: \"" + backup + "\"\n\nNote: Files will not be deleted!",
          () -> cleanExit(true), null);
    }
    event.consume();
  }

  @FXML
  private void importsButtonOnAction(ActionEvent event) {
    screenPane.open(importerScreen);
    event.consume();
  }

  @FXML
  private void logButtonOnAction(ActionEvent event) {
    openLog();
    event.consume();
  }

  @FXML
  private void backButtonOnAction(ActionEvent event) {
    explorerGoBack();
    event.consume();
  }

  // -------------------------------- Misc Event Handlers --------------------------------

  // REENG: reduce complexity
  private void explorerRootPaneOnDragDropped(DragEvent event) {
    List<File> files = event.getDragboard().getFiles();
    String url = event.getDragboard().getUrl();

    rootPane.getScene().getWindow().requestFocus();

    if (files != null && !files.isEmpty()) {
      for (File file : files) {
        if (files.size() == 1 && file.isDirectory()) {
          screenPane.open(importDialogScreen);
          importDialogScreen.setGroupName(file.getName());
          importDialogScreen.setFolder(file);
        } else if (Filters.FILE_NAME_FILTER.accept(file)) {
          importer.addJob(new ImportJob(file, null));
        }
      }
    } else if (url != null && !url.isEmpty()) {
      try {
        String folder = settings.defaultFolder.getValue();
        String filename = new URL(url).getPath().replaceAll("^.*/", "");
        File target;
        if (!settings.urlFilename.getValue() || folder == null || folder.isEmpty() ||
            !Files.isDirectory(Paths.get(folder))) {
          do {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save as");
            fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
            if (folder != null && !folder.isEmpty()) {
              fc.setInitialDirectory(new File(folder));
            }
            fc.setInitialFileName(filename);

            target = fc.showSaveDialog(rootPane.getScene().getWindow());

            if (target == null) {
              return;
            }
          } while (target.exists() || !target.getParentFile().exists());
        } else {
          target = FileUtil.resolveDuplicateFilename(new File(folder, filename));
        }
        if (Filters.FILE_NAME_FILTER.accept(target)) {
          importer.addJob(new ImportJob(new URL(url), target, null));
        }
      } catch (MalformedURLException e) {
        LOGGER.log(Level.WARNING, "File dragged from web has bad URL", e);
      }
    }
    event.consume();
  }

  private void explorerRootPaneOnDragOver(DragEvent event) {
    if (event.getGestureSource() == null &&
        (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
      event.acceptTransferModes(TransferMode.ANY);
      event.consume();
    }
  }

  private void explorerRootPaneDisabledChanged(Boolean newValue) {
    if (newValue) {
      if (previewMediaView.isPlaying()) {
        previewMediaView.pause();
        playVideoAfterExplorerEnabled = true;
      }
    } else if (playVideoAfterExplorerEnabled) {
      previewMediaView.play();
      playVideoAfterExplorerEnabled = false;
    }
  }

  // ---------------------------------- Key Event Handlers -------------------------------

  @FXML
  private void explorerRootPaneOnKeyPressed(KeyEvent event) {
    if (event.isControlDown()) {
      onKeyDownPressed(event);
      return;
    }

    switch (event.getCode()) {
      case S:
        handleKeyS(); // REENG: no break; OK?
      case ESCAPE:
        itemGridView.requestFocus();
        event.consume();
        break;
      case ENTER:
        handleKeyEnter(event);
        break;
      case BACK_SPACE:
        explorerGoBack();
        event.consume();
        break;
    }

  }

  private void handleKeyEnter(KeyEvent event) {
    if (itemGridView.getSelected().size() == 1) {
      Item item = itemGridView.getSelected().get(0);
      Items.get(ItemOpener.class, item).ifPresent(itemOpener -> itemOpener.open(item, this));
      event.consume();
    }
  }

  private void handleKeyS() {
    double scale = previewMediaView.getImageView().getScale().get();

    if (scale > 2 && !previewMediaView.getImageView().isScaleApplied()) {
      BufferedImage bimg =
          SwingFXUtils.fromFXImage(previewMediaView.getImageView().getTrueImage(), null);

      ResampleOp resizeOp =
          new ResampleOp((int) (bimg.getWidth() / scale), (int) (bimg.getHeight() / scale));
      resizeOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
      resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
      BufferedImage scaledImage = resizeOp.filter(bimg, bimg);

      previewMediaView.getImageView()
          .setAppliedScaleImage(SwingFXUtils.toFXImage(scaledImage, null));
    }
  }

  private void onKeyDownPressed(KeyEvent event) {
    switch (event.getCode()) {
      case F:
        searchTextField.requestFocus();
        event.consume();
        break;
      case E:
        editTagsTextField.setText(lastEditTagString);
        editTagsTextField.requestFocus();
        event.consume();
        break;
      case Q:
        menagerie.getDatabaseManager().enqueue(() -> cleanExit(false));
        event.consume();
        break;
      case S:
        settingsScreen.open(screenPane, settings);
        event.consume();
        break;
      case T:
        tagListScreen.open(screenPane, menagerie.getTags());
        event.consume();
        break;
      case I:
        screenPane.open(importDialogScreen);
        event.consume();
        break;
      case H:
        screenPane.open(helpScreen);
        event.consume();
        break;
      case D:
        handleKeyD(event);
        break;
      case Z:
        handleKeyZ(event);
        break;
      case N:
        screenPane.open(importerScreen);
        event.consume();
        break;
      case L:
        openLog();
        event.consume();
        break;
      case M:
        moveFilesScreen.open(screenPane, itemGridView.getSelected());
        event.consume();
        break;
      case R:
        handleKeyR(event);
        break;
      default:
        break;
    }
  }

  private void handleKeyR(KeyEvent event) {
    if (itemGridView.getSelected().size() == 1) {
      Item item = itemGridView.getSelected().get(0);
      Items.get(ItemRenamer.class, item).ifPresent(itemRenamer -> itemRenamer.rename(item, this));
    }
    event.consume();
  }

  private void handleKeyZ(KeyEvent event) {
    if (tagEditHistory.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
    } else {
      TagEditEvent peek = tagEditHistory.peek();
      new ConfirmationScreen().open(screenPane, "Undo last tag edit?",
          "Tags were added to " + peek.getAdded().keySet().size() +
              " items.\nTags were removed from " + peek.getRemoved().keySet().size() + " others.",
          () -> {
            TagEditEvent pop = tagEditHistory.pop();
            pop.revertAction();

            List<Item> list = new ArrayList<>();
            pop.getAdded().keySet().forEach(item -> {
              if (!list.contains(item)) {
                list.add(item);
              }
            });
            pop.getRemoved().keySet().forEach(item -> {
              if (!list.contains(item)) {
                list.add(item);
              }
            });
            menagerie.refreshInSearches(list);
          }, null);
    }
    event.consume();
  }

  private void handleKeyD(KeyEvent event) {
    if (event.isShiftDown() &&
        duplicateOptionsScreen.getDuplicatesScreen().getPairs() != null &&
        !duplicateOptionsScreen.getDuplicatesScreen().getPairs().isEmpty()) {
      duplicateOptionsScreen.getDuplicatesScreen().openWithOldPairs(screenPane, menagerie);
    } else {
      duplicateOptionsScreen.open(screenPane, menagerie, itemGridView.getSelected(),
          currentSearch.getResults(), menagerie.getItems());
    }
    event.consume();
  }

  @FXML
  private void editTagsTextFieldOnKeyPressed(KeyEvent event) {
    switch (event.getCode()) {
      case ENTER -> {
        editTagsOfSelected(editTagsTextField.getText());
        editTagsTextField.setText(null);
        itemGridView.requestFocus();
        event.consume();
      }
      case ESCAPE -> {
        editTagsTextField.setText(null);
        itemGridView.requestFocus();
        event.consume();
      }
      default -> {
      }
    }
  }

  @FXML
  private void searchVBoxOnKeyPressed(KeyEvent event) {
    if (event.isControlDown()) {
      switch (event.getCode()) {
        case D -> {
          listDescendingToggleButton.setSelected(!listDescendingToggleButton.isSelected());
          event.consume();
        }
        case G -> {
          showGroupedToggleButton.setSelected(!showGroupedToggleButton.isSelected());
          event.consume();
        }
        case S, R -> {
          shuffledSearchButton.setSelected(!shuffledSearchButton.isSelected());
          event.consume();
        }
      }
    }
  }
}
