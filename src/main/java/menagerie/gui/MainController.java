package menagerie.gui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import menagerie.gui.errors.ErrorListCell;
import menagerie.gui.errors.TrackedError;
import menagerie.gui.grid.ImageGridCell;
import menagerie.gui.grid.ImageGridView;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.predictive.PredictiveTextField;
import menagerie.gui.screens.*;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.gui.thumbnail.VideoThumbnailThread;
import menagerie.model.SimilarPair;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.search.Search;
import menagerie.model.search.SearchUpdateListener;
import menagerie.model.search.rules.*;
import menagerie.model.settings.Settings;
import menagerie.util.Filters;
import menagerie.util.SimplePokeListener;
import menagerie.util.folderwatcher.FolderWatcherThread;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class MainController {

    public StackPane rootPane;
    public StackPane screensStackPane;

    public BorderPane explorerRootPane;
    public ToggleButton listDescendingToggleButton;
    public PredictiveTextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicMediaView previewMediaView;
    public Label resultCountLabel;
    public Label imageInfoLabel;
    public Label fileNameLabel;
    public ListView<Tag> tagListView;
    public PredictiveTextField editTagsTextField;
    public MenuBar menuBar;
    public Button showErrorsButton;

    public BorderPane settings_rootPane;
    public CheckBox settings_computeMDCheckbox;
    public CheckBox settings_computeHistCheckbox;
    public CheckBox settings_autoImportWebCheckbox;
    public CheckBox settings_duplicateComputeHistCheckbox;
    public CheckBox settings_duplicateComputeMD5Checkbox;
    public CheckBox settings_duplicateConsolidateTagsCheckbox;
    public CheckBox settings_backupDatabaseCheckBox;
    public CheckBox settings_autoImportFolderCheckBox;
    public CheckBox settings_autoImportFromFolderToDefaultCheckBox;
    public CheckBox settings_duplicateCompareBlackAndWhiteCheckbox;
    public TextField settings_defaultFolderTextField;
    public TextField settings_dbURLTextField;
    public TextField settings_dbUserTextField;
    public TextField settings_dbPassTextField;
    public TextField settings_histConfidenceTextField;
    public TextField settings_importFromFolderTextField;
    public Button settings_cancelButton;
    public Button settings_importFromFolderBrowseButton;
    public ChoiceBox<Integer> settings_gridWidthChoiceBox;
    public CheckBox settings_muteVideoCheckBox;
    public CheckBox settings_repeatVideoCheckBox;

    public BorderPane duplicate_rootPane;
    public Label duplicate_similarityLabel;
    public Label duplicate_leftInfoLabel;
    public Label duplicate_rightInfoLabel;
    public TextField duplicate_leftPathTextField;
    public TextField duplicate_rightPathTextField;
    public DynamicMediaView duplicate_leftMediaView;
    public DynamicMediaView duplicate_rightMediaView;
    public ListView<Tag> duplicate_leftTagListView;
    public ListView<Tag> duplicate_rightTagListView;

    // ----------------------------------- Screens ---------------------------------------------------------------------

    public ScreenPane screenPane;
    private TagListScreen tagListScreen;
    private HelpScreen helpScreen;
    private SlideshowScreen slideshowScreen;
    private ErrorsScreen errorsScreen;


    //Menagerie vars
    private Menagerie menagerie;

    //Explorer screen vars
    private Search currentSearch = null;
    private ImageInfo currentlyPreviewing = null;
    private String lastEditTagString = null;
    private final ClipboardContent explorer_clipboard = new ClipboardContent();
    private boolean explorer_imageGridViewDragging = false;
    private ContextMenu explorer_cellContextMenu;

    //Duplicate screen vars
    private List<SimilarPair> duplicate_pairs = null;
    private SimilarPair duplicate_previewingPair = null;
    private ContextMenu duplicate_contextMenu;

    //Threads
    private FolderWatcherThread folderWatcherThread = null;

    //Settings var
    private final Settings settings = new Settings(new File("menagerie.properties"));

    private static final FileFilter FILE_FILTER = Filters.FILE_NAME_FILTER;

    private boolean playVideoAfterFocusGain = false;
    private boolean playVideoAfterExplorerEnabled = false;


    // ---------------------------------- Initializers ------------------------------------

    @FXML
    public void initialize() {

        //Initialize settings
        try {
            settings.loadFromFile();
        } catch (IOException e) {
            trySaveSettings();
        }

        //Backup database
        if (settings.isBackupDatabase()) backupDatabase();

        //Initialize the menagerie
        initMenagerie();

        //Init screens
        initScreens();

        //Things to run on first "tick"
        Platform.runLater(() -> {
            //Apply window props and listeners
            initWindowPropertiesAndListeners();

            //Init closeRequest handling on window
            rootPane.getScene().getWindow().setOnCloseRequest(event -> cleanExit(false));
        });

        //Apply a default search
        applySearch(null, listDescendingToggleButton.isSelected());

        //Init folder watcher
        startWatchingFolderForImages();

    }

    private void backupDatabase() {
        try {
            backUpDatabase(settings.getDbUrl());
        } catch (IOException e) {
            e.printStackTrace();
            Main.showErrorMessage("Error", "Error while trying to back up the database: " + settings.getDbUrl(), e.getLocalizedMessage());
        }
    }

    private void initScreens() {
        initExplorerScreen();
        initSettingsScreen();
        initDuplicateScreen();
        initErrorsScreen();
        initTagListScreen();
        initSlideShowScreen();
        helpScreen = new HelpScreen();
        screenPane.getChildren().addListener((ListChangeListener<? super Node>) c -> explorerRootPane.setDisable(!c.getList().isEmpty())); //Init disable listener for explorer screen
    }

    private void initMenagerie() {
        try {
            Connection db = DriverManager.getConnection("jdbc:h2:" + settings.getDbUrl(), settings.getDbUser(), settings.getDbPass());
            DatabaseVersionUpdater.updateDatabase(db);

            menagerie = new Menagerie(db);

            menagerie.getUpdateQueue().setErrorListener(e -> Platform.runLater(() -> {
                errorsScreen.addError(new TrackedError(e, TrackedError.Severity.HIGH, "Error while updating database", "An exception as thrown while trying to update the database", "Concurrent modification error or SQL statement out of date"));
            }));
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }
    }

    private void initSlideShowScreen() {
        MenuItem showInSearchMenuItem = new MenuItem("Show in search");
        showInSearchMenuItem.setOnAction(event -> {
            slideshowScreen.close();
            imageGridView.select(slideshowScreen.getShowing(), false, false);
        });
        MenuItem forgetCurrentMenuItem = new MenuItem("Forget");
        forgetCurrentMenuItem.setOnAction(event -> tryDeleteCurrentSlideShowImage(false));
        MenuItem deleteCurrentMenuItem = new MenuItem("Delete");
        deleteCurrentMenuItem.setOnAction(event -> tryDeleteCurrentSlideShowImage(true));

        slideshowScreen = new SlideshowScreen();
        slideshowScreen.setItemContextMenu(new ContextMenu(showInSearchMenuItem, new SeparatorMenuItem(), forgetCurrentMenuItem, deleteCurrentMenuItem));
        slideshowScreen.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                tryDeleteCurrentSlideShowImage(!event.isControlDown());
                event.consume();
            }
        });
    }

    private void initErrorsScreen() {
        errorsScreen = new ErrorsScreen();
        errorsScreen.getErrors().addListener((ListChangeListener<? super TrackedError>) c -> {
            final int count = c.getList().size();

            if (count == 0) {
                showErrorsButton.setStyle("-fx-background-color: transparent;");
            } else {
                showErrorsButton.setStyle("-fx-background-color: red;");
            }

            showErrorsButton.setText("" + count);
        });
    }

    private void initSettingsScreen() {
        //Initialize grid width setting choicebox
        Integer[] elements = new Integer[Settings.MAX_IMAGE_GRID_WIDTH - Settings.MIN_IMAGE_GRID_WIDTH + 1];
        for (int i = 0; i < elements.length; i++) elements[i] = i + Settings.MIN_IMAGE_GRID_WIDTH;
        settings_gridWidthChoiceBox.getItems().addAll(elements);
        settings_gridWidthChoiceBox.getSelectionModel().clearAndSelect(0);
    }

    private void initDuplicateScreen() {
        duplicate_leftTagListView.setCellFactory(param -> new TagListCell());
        duplicate_rightTagListView.setCellFactory(param -> new TagListCell());
        settings_histConfidenceTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double d = Double.parseDouble(settings_histConfidenceTextField.getText());
                    if (d <= 0 || d > 1) {
                        settings_histConfidenceTextField.setText("0.95");
                    }
                } catch (NullPointerException | NumberFormatException e) {
                    settings_histConfidenceTextField.setText("0.95");
                }
            }
        });

        MenuItem showInSearchMenuItem = new MenuItem("Show in search");
        showInSearchMenuItem.setOnAction(event -> {
            closeDuplicateScreen();
            imageGridView.select((ImageInfo) duplicate_contextMenu.getUserData(), false, false);
        });
        MenuItem forgetMenuItem = new MenuItem("Forget");
        forgetMenuItem.setOnAction(event -> {
            ImageInfo toKeep = duplicate_previewingPair.getImg1();
            if (toKeep.equals(duplicate_contextMenu.getUserData())) toKeep = duplicate_previewingPair.getImg2();
            duplicate_deleteImage((ImageInfo) duplicate_contextMenu.getUserData(), toKeep, false);
        });
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(event -> {
            ImageInfo toKeep = duplicate_previewingPair.getImg1();
            if (toKeep.equals(duplicate_contextMenu.getUserData())) toKeep = duplicate_previewingPair.getImg2();
            duplicate_deleteImage((ImageInfo) duplicate_contextMenu.getUserData(), toKeep, false);
        });

        duplicate_contextMenu = new ContextMenu(showInSearchMenuItem, new SeparatorMenuItem(), forgetMenuItem, deleteMenuItem);
        duplicate_leftMediaView.setOnContextMenuRequested(event -> {
            duplicate_contextMenu.setUserData(duplicate_previewingPair.getImg1());
            duplicate_contextMenu.show(duplicate_leftMediaView, event.getScreenX(), event.getScreenY());
        });
        duplicate_rightMediaView.setOnContextMenuRequested(event -> {
            duplicate_contextMenu.setUserData(duplicate_previewingPair.getImg2());
            duplicate_contextMenu.show(duplicate_rightMediaView, event.getScreenX(), event.getScreenY());
        });
    }

    private void initTagListScreen() {
        tagListScreen = new TagListScreen();
        tagListScreen.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                MenuItem i1 = new MenuItem("Search this tag");
                i1.setOnAction(event1 -> {
                    searchTextField.setText(c.getItem().getName());
                    searchTextField.positionCaret(searchTextField.getText().length());
                    tagListScreen.close();
                    applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
                });
                ContextMenu m = new ContextMenu(i1);
                m.show(c, event.getScreenX(), event.getScreenY());
            });
            return c;
        });
    }

    private void initExplorerScreen() {
        //Set image grid width from settings
        setGridWidth(settings.getImageGridWidth());

        //Init image grid
        imageGridView.setSelectionListener(image -> Platform.runLater(() -> previewImage(image)));
        imageGridView.setCellFactory(param -> {
            ImageGridCell c = new ImageGridCell();
            c.setOnDragDetected(event -> {
                if (!imageGridView.getSelected().isEmpty() && event.isPrimaryButtonDown()) {
                    if (!imageGridView.isSelected(c.getItem()))
                        imageGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());

                    Dragboard db = c.startDragAndDrop(TransferMode.ANY);

                    for (ImageInfo img : imageGridView.getSelected()) {
                        String filename = img.getFile().getName().toLowerCase();
                        if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp")) {
                            if (img.getThumbnail().isLoaded()) {
                                db.setDragView(img.getThumbnail().getImage());
                                break;
                            }
                        }
                    }

                    List<File> files = new ArrayList<>();
                    imageGridView.getSelected().forEach(img -> files.add(img.getFile()));
                    explorer_clipboard.putFiles(files);
                    db.setContent(explorer_clipboard);

                    explorer_imageGridViewDragging = true;
                    event.consume();
                }
            });
            c.setOnDragDone(event -> {
                explorer_imageGridViewDragging = false;
                event.consume();
            });
            c.setOnMouseReleased(event -> {
                if (!explorer_imageGridViewDragging && event.getButton() == MouseButton.PRIMARY) {
                    imageGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                if (explorer_cellContextMenu.isShowing()) explorer_cellContextMenu.hide();
                explorer_cellContextMenu.show(c, event.getScreenX(), event.getScreenY());
                event.consume();
            });
            return c;
        });
        imageGridView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DELETE:
                    final boolean deleteFiles = !event.isControlDown();
                    final SimplePokeListener onFinish = () -> {
                        previewImage(null);
                        menagerie.removeImages(imageGridView.getSelected(), deleteFiles);
                    };
                    if (deleteFiles) {
                        new ConfirmationScreen().open(screenPane, "Delete files", "Permanently delete selected files? (" + imageGridView.getSelected().size() + " files)\n\n" +
                                "This action CANNOT be undone (files will be deleted)", onFinish, null);
                    } else {
                        new ConfirmationScreen().open(screenPane, "Forget files", "Remove selected files from database? (" + imageGridView.getSelected().size() + " files)\n\n" +
                                "This action CANNOT be undone", onFinish, null);
                    }
                    event.consume();
                    break;
            }
        });
        imageGridView.getSelected().addListener((ListChangeListener<? super ImageInfo>) c -> resultCountLabel.setText(imageGridView.getSelected().size() + " / " + currentSearch.getResults().size()));
        initExplorerGridCellContextMenu();

        //Init drag/drop handlers
        explorerRootPane.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (previewMediaView.isPlaying()) {
                    previewMediaView.pause();
                    playVideoAfterExplorerEnabled = true;
                }
            } else if (playVideoAfterExplorerEnabled) {
                previewMediaView.play();
                playVideoAfterExplorerEnabled = false;
            }
        });
        explorerRootPane.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        explorerRootPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();

            if (files != null && !files.isEmpty()) {
                List<Runnable> queue = new ArrayList<>();
                files.forEach(file -> queue.add(() -> {
                    try {
                        menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport());
                    } catch (Exception e) {
                        Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to import file", "Exception was thrown while trying to import a file: " + file, "Unknown")));
                    }
                }));

                if (!queue.isEmpty()) {
                    new ProgressScreen().open(screenPane, "Importing files", "Importing " + queue.size() + " files...", queue, null, null);
                }
            } else if (url != null && !url.isEmpty()) {
                Platform.runLater(() -> {
                    String folder = settings.getDefaultFolder();
                    if (!folder.endsWith("/") && !folder.endsWith("\\")) folder += "/";
                    String filename = URI.create(url).getPath().replaceAll("^.*/", "");
                    File target = resolveDuplicateFilename(new File(folder + filename));

                    while (!settings.isAutoImportFromWeb() || !target.getParentFile().exists() || target.exists() || !FILE_FILTER.accept(target)) {
                        target = openSaveImageDialog(explorerRootPane.getScene().getWindow(), new File(settings.getDefaultFolder()), filename);
                        if (target == null) return;
                        if (target.exists())
                            Main.showErrorMessage("Error", "File already exists, cannot be overwritten", target.getAbsolutePath());
                    }

                    final File finalTarget = target;
                    new Thread(() -> {
                        try {
                            downloadAndSaveFile(url, finalTarget);
                            Platform.runLater(() -> {
                                ImageInfo img = menagerie.importImage(finalTarget, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport());
                                if (img == null) {
                                    if (!finalTarget.delete())
                                        System.out.println("Tried to delete a downloaded file, as it couldn't be imported, but failed: " + finalTarget);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> Main.showErrorMessage("Unexpected error", "Error while trying to download file", e.getLocalizedMessage()));
                        }
                    }).start();
                });
            }
            event.consume();
        });

        //Init tag list cell factory
        tagListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                if (c.getItem() != null) {
                    MenuItem i1 = new MenuItem("Add to search");
                    i1.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " " + c.getItem().getName());
                        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
                    });
                    MenuItem i2 = new MenuItem("Exclude from search");
                    i2.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " -" + c.getItem().getName());
                        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
                    });
                    MenuItem i3 = new MenuItem("Remove from selected");
                    i3.setOnAction(event1 -> imageGridView.getSelected().forEach(img -> img.removeTag(c.getItem())));
                    ContextMenu m = new ContextMenu(i1, i2, new SeparatorMenuItem(), i3);
                    m.show(c, event.getScreenX(), event.getScreenY());
                }
            });
            return c;
        });

        editTagsTextField.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags;
            if (negative) tags = new ArrayList<>(tagListView.getItems());
            else tags = new ArrayList<>(menagerie.getTags());
            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) results.add("-" + tag.getName());
                    else results.add(tag.getName());
                }

                if (results.size() >= 8) break;
            }

            return results;
        });

        searchTextField.setTop(false);
        searchTextField.setOptionsListener(editTagsTextField.getOptionsListener());

        previewMediaView.setMute(settings.isMuteVideoPreview());
        previewMediaView.setRepeat(settings.isRepeatVideoPreview());
    }

    private void initExplorerGridCellContextMenu() {
        MenuItem slideShowSelectedMenuItem = new MenuItem("Selected");
        slideShowSelectedMenuItem.setOnAction(event1 -> slideshowScreen.open(screenPane, imageGridView.getSelected()));
        MenuItem slideShowSearchedMenuItem = new MenuItem("Searched");
        slideShowSearchedMenuItem.setOnAction(event1 -> slideshowScreen.open(screenPane, imageGridView.getItems()));
        Menu slideShowMenu = new Menu("Slideshow", null, slideShowSelectedMenuItem, slideShowSearchedMenuItem);

        MenuItem openInExplorerMenuItem = new MenuItem("Open in Explorer");
        openInExplorerMenuItem.setOnAction(event1 -> {
            if (!imageGridView.getSelected().isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + imageGridView.getLastSelected().getFile().getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Unexpected Error", "Error opening file explorer", e.getLocalizedMessage());
                }
            }
        });

        MenuItem buildMD5HashMenuItem = new MenuItem("Build MD5 Hash");
        buildMD5HashMenuItem.setOnAction(event1 -> {
            List<Runnable> queue = new ArrayList<>();
            imageGridView.getSelected().forEach(img -> {
                if (img.getMD5() == null) {
                    queue.add(() -> {
                        try {
                            img.initializeMD5();
                            img.commitMD5ToDatabase();
                        } catch (Exception e) {
                            Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute MD5", "Exception was thrown while trying to compute an MD5 for file: " + img, "Unknown")));
                        }
                    });
                }
            });
            if (!queue.isEmpty()) {
                new ProgressScreen().open(screenPane, "Building MD5s", "Building MD5 hashes for " + queue.size() + " files...", queue, null, null);
            }
        });
        MenuItem buildHistogramMenuItem = new MenuItem("Build Histogram");
        buildHistogramMenuItem.setOnAction(event1 -> {
            List<Runnable> queue = new ArrayList<>();
            imageGridView.getSelected().forEach(img -> {
                String filename = img.getFile().getName().toLowerCase();
                if (img.getHistogram() == null && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
                    queue.add(() -> {
                        try {
                            img.initializeHistogram();
                            img.commitHistogramToDatabase();
                        } catch (Exception e) {
                            Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute histogram", "Exception was thrown while trying to compute a histogram for image: " + img, "Unknown")));
                        }
                    });
                }
            });
            if (!queue.isEmpty()) {
                new ProgressScreen().open(screenPane, "Building Histograms", "Building image histograms for " + queue.size() + " files...", queue, null, null);
            }
        });

        MenuItem findDuplicatesMenuItem = new MenuItem("Find Duplicates");
        findDuplicatesMenuItem.setOnAction(event1 -> duplicate_compareAndShow(imageGridView.getSelected()));

        MenuItem moveToFolderMenuItem = new MenuItem("Move To...");
        moveToFolderMenuItem.setOnAction(event1 -> {
            if (!imageGridView.getSelected().isEmpty()) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Move files to folder...");
                File result = dc.showDialog(rootPane.getScene().getWindow());

                if (result != null) {
                    List<Runnable> queue = new ArrayList<>();

                    imageGridView.getSelected().forEach(img -> queue.add(() -> {
                        File f = result.toPath().resolve(img.getFile().getName()).toFile();
                        if (!img.getFile().equals(f)) {
                            File dest = MainController.resolveDuplicateFilename(f);

                            if (!img.renameTo(dest)) {
                                Platform.runLater(() -> errorsScreen.addError(new TrackedError(null, TrackedError.Severity.HIGH, "Error moving file", "An exception was thrown while trying to move a file\nFrom: " + img.getFile() + "\nTo: " + dest, "Unknown")));
                            }
                        }
                    }));

                    if (!queue.isEmpty()) {
                        new ProgressScreen().open(screenPane, "Moving files", "Moving " + queue.size() + " files...", queue, null, null);
                    }
                }
            }
        });

        MenuItem removeImagesMenuItem = new MenuItem("Remove");
        removeImagesMenuItem.setOnAction(event1 -> {
            new ConfirmationScreen().open(screenPane, "Forget files", "Remove selected files from database? (" + imageGridView.getSelected().size() + " files)\n\n" +
                    "This action CANNOT be undone", () -> menagerie.removeImages(imageGridView.getSelected(), false), null);
        });
        MenuItem deleteImagesMenuItem = new MenuItem("Delete");
        deleteImagesMenuItem.setOnAction(event1 -> new ConfirmationScreen().open(screenPane, "Delete files", "Permanently delete selected files? (" + imageGridView.getSelected().size() + " files)\n\n" +
                "This action CANNOT be undone (files will be deleted)", () -> {
            previewImage(null);
            menagerie.removeImages(imageGridView.getSelected(), true);
        }, null));

        explorer_cellContextMenu = new ContextMenu(slideShowMenu, new SeparatorMenuItem(), openInExplorerMenuItem, new SeparatorMenuItem(), buildMD5HashMenuItem, buildHistogramMenuItem, new SeparatorMenuItem(), findDuplicatesMenuItem, new SeparatorMenuItem(), moveToFolderMenuItem, new SeparatorMenuItem(), removeImagesMenuItem, deleteImagesMenuItem);
    }

    private void initWindowPropertiesAndListeners() {
        Stage stage = ((Stage) explorerRootPane.getScene().getWindow());
        stage.setMaximized(settings.isWindowMaximized());
        if (settings.getWindowWidth() > 0) stage.setWidth(settings.getWindowWidth());
        if (settings.getWindowHeight() > 0) stage.setHeight(settings.getWindowHeight());
        if (settings.getWindowX() >= 0) stage.setX(settings.getWindowX());
        if (settings.getWindowY() >= 0) stage.setY(settings.getWindowY());

        //Bind window properties to settings
        stage.maximizedProperty().addListener((observable, oldValue, newValue) -> settings.setWindowMaximized(newValue));
        stage.widthProperty().addListener((observable, oldValue, newValue) -> settings.setWindowWidth(newValue.intValue()));
        stage.heightProperty().addListener((observable, oldValue, newValue) -> settings.setWindowHeight(newValue.intValue()));
        stage.xProperty().addListener((observable, oldValue, newValue) -> settings.setWindowX(newValue.intValue()));
        stage.yProperty().addListener((observable, oldValue, newValue) -> settings.setWindowY(newValue.intValue()));

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

    // ---------------------------------- Screen openers ------------------------------------

    private void openSettingsScreen() {
        //Update settings fx nodes
        settings_defaultFolderTextField.setText(settings.getDefaultFolder());
        settings_importFromFolderTextField.setText(settings.getImportFromFolderPath());
        settings_dbURLTextField.setText(settings.getDbUrl());
        settings_dbUserTextField.setText(settings.getDbUser());
        settings_dbPassTextField.setText(settings.getDbPass());

        settings_autoImportWebCheckbox.setSelected(settings.isAutoImportFromWeb());
        settings_computeMDCheckbox.setSelected(settings.isComputeMD5OnImport());
        settings_computeHistCheckbox.setSelected(settings.isComputeHistogramOnImport());
        settings_duplicateComputeMD5Checkbox.setSelected(settings.isComputeMD5ForSimilarity());
        settings_duplicateComputeHistCheckbox.setSelected(settings.isComputeHistogramForSimilarity());
        settings_duplicateConsolidateTagsCheckbox.setSelected(settings.isConsolidateTags());
        settings_backupDatabaseCheckBox.setSelected(settings.isBackupDatabase());
        settings_autoImportFolderCheckBox.setSelected(settings.isAutoImportFromFolder());
        settings_autoImportFromFolderToDefaultCheckBox.setSelected(settings.isAutoImportFromFolderToDefault());
        settings_duplicateCompareBlackAndWhiteCheckbox.setSelected(settings.isCompareBlackAndWhiteHists());
        settings_repeatVideoCheckBox.setSelected(settings.isRepeatVideoPreview());
        settings_muteVideoCheckBox.setSelected(settings.isMuteVideoPreview());

        settings_histConfidenceTextField.setText("" + settings.getSimilarityThreshold());

        settings_gridWidthChoiceBox.getSelectionModel().select((Integer) settings.getImageGridWidth());

        settings_updateAutoImportFolderDisabledStatus();

        //Enable pane
        explorerRootPane.setDisable(true);
        settings_rootPane.setDisable(false);
        settings_rootPane.setOpacity(1);
        settings_cancelButton.requestFocus();
    }

    private void closeSettingsScreen(boolean saveChanges) {
        //Disable pane
        explorerRootPane.setDisable(false);
        settings_rootPane.setDisable(true);
        settings_rootPane.setOpacity(0);
        imageGridView.requestFocus();

        if (saveChanges) {
            //Save settings to settings object
            settings.setDefaultFolder(settings_defaultFolderTextField.getText());
            settings.setDbUrl(settings_dbURLTextField.getText());
            settings.setDbUser(settings_dbUserTextField.getText());
            settings.setDbPass(settings_dbPassTextField.getText());
            settings.setImportFromFolderPath(settings_importFromFolderTextField.getText());

            settings.setAutoImportFromWeb(settings_autoImportWebCheckbox.isSelected());
            settings.setComputeMD5OnImport(settings_computeMDCheckbox.isSelected());
            settings.setComputeHistogramOnImport(settings_computeHistCheckbox.isSelected());
            settings.setComputeMD5ForSimilarity(settings_duplicateComputeMD5Checkbox.isSelected());
            settings.setComputeHistogramForSimilarity(settings_duplicateComputeHistCheckbox.isSelected());
            settings.setConsolidateTags(settings_duplicateConsolidateTagsCheckbox.isSelected());
            settings.setBackupDatabase(settings_backupDatabaseCheckBox.isSelected());
            settings.setAutoImportFromFolder(settings_autoImportFolderCheckBox.isSelected());
            settings.setAutoImportFromFolderToDefault(settings_autoImportFromFolderToDefaultCheckBox.isSelected());
            settings.setCompareBlackAndWhiteHists(settings_duplicateCompareBlackAndWhiteCheckbox.isSelected());
            settings.setMuteVideoPreview(settings_muteVideoCheckBox.isSelected());
            settings.setRepeatVideoPreview(settings_repeatVideoCheckBox.isSelected());

            settings.setSimilarityThreshold(Double.parseDouble(settings_histConfidenceTextField.getText()));

            settings.setImageGridWidth(settings_gridWidthChoiceBox.getValue());

            setGridWidth(settings_gridWidthChoiceBox.getValue());

            startWatchingFolderForImages();

            previewMediaView.setMute(settings.isMuteVideoPreview());
            previewMediaView.setRepeat(settings.isRepeatVideoPreview());
        }

        trySaveSettings();
    }

    private void openDuplicateScreen(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) return;

        duplicate_pairs = new ArrayList<>();
        List<Runnable> queue = new ArrayList<>();

        for (int actualI = 0; actualI < images.size(); actualI++) {
            final int i = actualI;
            queue.add(() -> {
                ImageInfo i1 = images.get(i);
                for (int j = i + 1; j < images.size(); j++) {
                    ImageInfo i2 = images.get(j);

                    try {
                        double similarity = i1.getSimilarityTo(i2, settings.isCompareBlackAndWhiteHists());

                        if (similarity >= settings.getSimilarityThreshold())
                            duplicate_pairs.add(new SimilarPair(i1, i2, similarity));
                    } catch (Exception e) {
                        Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compare images", "Exception was thrown while trying to compare two images: (" + i1 + ", " + i2 + ")", "Unknown")));
                    }
                }
            });
        }

        if (queue.size() > 5000) {
            new ProgressScreen().open(screenPane, "Comparing images", "Checking comparisons for " + queue.size() + " images...", queue, total -> Platform.runLater(() -> {
                if (duplicate_pairs.isEmpty()) return;

                duplicate_previewPair(duplicate_pairs.get(0));

                explorerRootPane.setDisable(true);
                duplicate_rootPane.setDisable(false);
                duplicate_rootPane.setOpacity(1);
                duplicate_rootPane.requestFocus();
            }), null);
        } else {
            queue.forEach(Runnable::run);

            if (duplicate_pairs.isEmpty()) return;

            duplicate_previewPair(duplicate_pairs.get(0));

            explorerRootPane.setDisable(true);
            duplicate_rootPane.setDisable(false);
            duplicate_rootPane.requestFocus();
            duplicate_rootPane.setOpacity(1);
        }
    }

    private void closeDuplicateScreen() {
        duplicate_previewPair(null);

        explorerRootPane.setDisable(false);
        duplicate_rootPane.setDisable(true);
        imageGridView.requestFocus();
        duplicate_rootPane.setOpacity(0);
    }

    // -------------------------------- Dialog Openers ---------------------------------------

    private void openImportFolderDialog() {
        DirectoryChooser dc = new DirectoryChooser();
        if (settings.getDefaultFolder() != null && !settings.getDefaultFolder().isEmpty())
            dc.setInitialDirectory(new File(settings.getDefaultFolder()));
        File result = dc.showDialog(rootPane.getScene().getWindow());

        if (result != null) {
            List<Runnable> queue = new ArrayList<>();
            List<File> files = getFilesRecursively(result, FILE_FILTER);
            menagerie.getImages().forEach(img -> files.remove(img.getFile()));
            files.forEach(file -> queue.add(() -> {
                try {
                    menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport());
                } catch (Exception e) {
                    Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to import file", "Exception was thrown while trying to import an file: " + file, "Unknown")));
                }
            }));

            if (!queue.isEmpty()) {
                new ProgressScreen().open(screenPane, "Importing files", "Importing " + queue.size() + " files...", queue, null, null);
            }
        }
    }

    private void openImportFilesDialog() {
        FileChooser fc = new FileChooser();
        if (settings.getDefaultFolder() != null && !settings.getDefaultFolder().isEmpty())
            fc.setInitialDirectory(new File(settings.getDefaultFolder()));
        fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
        List<File> results = fc.showOpenMultipleDialog(rootPane.getScene().getWindow());

        if (results != null && !results.isEmpty()) {
            final List<File> finalResults = new ArrayList<>(results);
            menagerie.getImages().forEach(img -> finalResults.remove(img.getFile()));

            List<Runnable> queue = new ArrayList<>();
            finalResults.forEach(file -> queue.add(() -> {
                try {
                    menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport());
                } catch (Exception e) {
                    Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to import file", "Exception was thrown while trying to import an file: " + file, "Unknown")));
                }
            }));

            if (!queue.isEmpty()) {
                new ProgressScreen().open(screenPane, "Importing files", "Importing " + queue.size() + " files...", queue, null, null);
            }
        }
    }

    private static File openSaveImageDialog(Window window, File folder, String filename) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save file from web");
        if (filename != null) fc.setInitialFileName(filename);
        if (folder != null) fc.setInitialDirectory(folder);
        fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
        return fc.showSaveDialog(window);
    }

    // ---------------------------------- GUI Action Methods ------------------------------------

    @SuppressWarnings("SameParameterValue")
    private void previewImage(ImageInfo image) {
        if (currentlyPreviewing != null) currentlyPreviewing.setTagListener(null);
        currentlyPreviewing = image;

        if (!previewMediaView.preview(image)) {
            errorsScreen.addError(new TrackedError(null, TrackedError.Severity.NORMAL, "Unsupported preview filetype", "Tried to preview a filetype that isn't supposed", "An unsupported filetype somehow got added to the system"));
        }

        updateTagList(image);

        updateImageInfoLabel(image, imageInfoLabel);

        if (image != null) {
            image.setTagListener(() -> updateTagList(image));

            fileNameLabel.setText(image.getFile().toString());
        } else {
            fileNameLabel.setText("N/A");
        }
    }

    private void updateTagList(ImageInfo image) {
        tagListView.getItems().clear();
        if (image != null) {
            tagListView.getItems().addAll(image.getTags());
            tagListView.getItems().sort(Comparator.comparing(Tag::getName));
        }
    }

    private static void updateImageInfoLabel(ImageInfo image, Label label) {
        if (image == null) {
            label.setText("Size: N/A - Res: N/A");

            return;
        }

        if (image.getImage().isBackgroundLoading() && image.getImage().getProgress() != 1) {
            label.setText("Size: N/A - Res: N/A");

            image.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1 && !image.getImage().isError()) updateImageInfoLabel(image, label);
            });
        } else {
            //Find size string
            double size = image.getFile().length();
            String sizeStr;
            if (size > 1024 * 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024 / 1024) + "GB";
            else if (size > 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024) + "MB";
            else if (size > 1024) sizeStr = String.format("%.2f", size / 1024) + "KB";
            else sizeStr = String.format("%.2f", size) + "B";

            label.setText("Size: " + sizeStr + " - Res: " + (int) image.getImage().getWidth() + "x" + (int) image.getImage().getHeight());
        }
    }

    private void settings_updateAutoImportFolderDisabledStatus() {
        if (settings_autoImportFolderCheckBox.isSelected()) {
            settings_importFromFolderTextField.setDisable(false);
            settings_importFromFolderBrowseButton.setDisable(false);
            settings_autoImportFromFolderToDefaultCheckBox.setDisable(false);
        } else {
            settings_importFromFolderTextField.setDisable(true);
            settings_importFromFolderBrowseButton.setDisable(true);
            settings_autoImportFromFolderToDefaultCheckBox.setDisable(true);
        }
    }

    private void applySearch(String search, boolean descending) {
        if (currentSearch != null) currentSearch.close();
        previewImage(null);

        currentSearch = new Search(menagerie, constructRuleSet(search), descending);
        currentSearch.setListener(new SearchUpdateListener() {
            @Override
            public void imagesAdded(List<ImageInfo> images) {
                Platform.runLater(() -> {
                    imageGridView.getItems().addAll(0, images);

//                    imageGridView.getItems().sort(currentSearch.getComparator()); // This causes some gridcells to glitch out and get stuck visually
                });
            }

            @Override
            public void imagesRemoved(List<ImageInfo> images) {
                Platform.runLater(() -> {
                    final int oldLastIndex = imageGridView.getItems().indexOf(imageGridView.getLastSelected()) + 1;
                    int newIndex = oldLastIndex;
                    for (ImageInfo image : images) {
                        final int i = imageGridView.getItems().indexOf(image);
                        if (i < 0) continue;

                        if (i < oldLastIndex) {
                            newIndex--;
                        }
                    }

                    imageGridView.getItems().removeAll(images);
                    if (images.contains(currentlyPreviewing)) previewImage(null);

                    if (!imageGridView.getItems().isEmpty()) {
                        if (newIndex >= imageGridView.getItems().size())
                            newIndex = imageGridView.getItems().size() - 1;
                        imageGridView.setLastSelected(imageGridView.getItems().get(newIndex));
                    }
                });
            }
        });

        imageGridView.clearSelection();
        imageGridView.getItems().clear();
        imageGridView.getItems().addAll(currentSearch.getResults());

        if (!imageGridView.getItems().isEmpty())
            imageGridView.select(imageGridView.getItems().get(0), false, false);
    }

    private List<SearchRule> constructRuleSet(String str) {
        if (str == null) str = "";
        List<SearchRule> rules = new ArrayList<>();
        for (String arg : str.split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            boolean inverted = false;
            if (arg.charAt(0) == '-') {
                inverted = true;
                arg = arg.substring(1);
            }

            if (arg.startsWith("id:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                IDRule.Type type = IDRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = IDRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = IDRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new IDRule(type, Integer.parseInt(temp), inverted));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for ID rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("date:") || arg.startsWith("time:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                DateAddedRule.Type type = DateAddedRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = DateAddedRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = DateAddedRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new DateAddedRule(type, Long.parseLong(temp), inverted));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting long value for date added rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("md5:")) {
                rules.add(new MD5Rule(arg.substring(arg.indexOf(':') + 1), inverted));
            } else if (arg.startsWith("path:") || arg.startsWith("file:")) {
                rules.add(new FilePathRule(arg.substring(arg.indexOf(':') + 1), inverted));
            } else if (arg.startsWith("missing:")) {
                String type = arg.substring(arg.indexOf(':') + 1);
                switch (type.toLowerCase()) {
                    case "md5":
                        rules.add(new MissingRule(MissingRule.Type.MD5, inverted));
                        break;
                    case "file":
                        rules.add(new MissingRule(MissingRule.Type.FILE, inverted));
                        break;
                    case "histogram":
                    case "hist":
                        rules.add(new MissingRule(MissingRule.Type.HISTOGRAM, inverted));
                        break;
                }
            } else if (arg.startsWith("tags:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                TagCountRule.Type type = TagCountRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = TagCountRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = TagCountRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new TagCountRule(type, Integer.parseInt(temp), inverted));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for tag count rule", e.getLocalizedMessage());
                }
            } else {
                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(-1, arg);
                rules.add(new TagRule(tag, inverted));
            }
        }
        return rules;
    }

    private void setGridWidth(int n) {
        final double width = 18 + (Thumbnail.THUMBNAIL_SIZE + ImageGridView.CELL_BORDER * 2 + imageGridView.getHorizontalCellSpacing() * 2) * n;
        imageGridView.setMinWidth(width);
        imageGridView.setMaxWidth(width);
        imageGridView.setPrefWidth(width);
    }

    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || imageGridView.getSelected().isEmpty()) return;
        lastEditTagString = input.trim();

        List<ImageInfo> changed = new ArrayList<>();

        for (String text : input.split("\\s+")) {
            if (text.startsWith("-")) {
                Tag t = menagerie.getTagByName(text.substring(1));
                if (t != null) {
                    imageGridView.getSelected().forEach(img -> {
                        img.removeTag(t);
                        changed.add(img);
                    });
                }
            } else {
                Tag t = menagerie.getTagByName(text);
                if (t == null) t = menagerie.createTag(text);
                for (ImageInfo img : imageGridView.getSelected()) {
                    img.addTag(t);
                    changed.add(img);
                }
            }
        }

        if (!changed.isEmpty()) menagerie.checkImagesStillValidInSearches(changed);
    }

    private void duplicate_previewPair(SimilarPair pair) {
        if (pair == null) {
            duplicate_previewingPair = null;

            duplicate_leftMediaView.preview(null);
            duplicate_leftTagListView.getItems().clear();
            duplicate_leftPathTextField.setText("N/A");
            updateImageInfoLabel(null, duplicate_leftInfoLabel);

            duplicate_rightMediaView.preview(null);
            duplicate_rightTagListView.getItems().clear();
            duplicate_rightPathTextField.setText("N/A");
            updateImageInfoLabel(null, duplicate_rightInfoLabel);

            duplicate_similarityLabel.setText("N/A% Match");
        } else {
            duplicate_previewingPair = pair;

            duplicate_leftMediaView.preview(pair.getImg1());
            duplicate_leftPathTextField.setText(pair.getImg1().getFile().toString());
            duplicate_leftTagListView.getItems().clear();
            duplicate_leftTagListView.getItems().addAll(pair.getImg1().getTags());
            duplicate_leftTagListView.getItems().sort(Comparator.comparing(Tag::getName));
            updateImageInfoLabel(pair.getImg1(), duplicate_leftInfoLabel);

            duplicate_rightMediaView.preview(pair.getImg2());
            duplicate_rightPathTextField.setText(pair.getImg2().getFile().toString());
            duplicate_rightTagListView.getItems().clear();
            duplicate_rightTagListView.getItems().addAll(pair.getImg2().getTags());
            duplicate_rightTagListView.getItems().sort(Comparator.comparing(Tag::getName));
            updateImageInfoLabel(pair.getImg2(), duplicate_rightInfoLabel);

            DecimalFormat df = new DecimalFormat("#.##");
            duplicate_similarityLabel.setText((duplicate_pairs.indexOf(pair) + 1) + "/" + duplicate_pairs.size() + " - " + df.format(pair.getSimilarity() * 100) + "% Match");
        }
    }

    private void duplicate_previewLastPair() {
        if (duplicate_pairs == null || duplicate_pairs.isEmpty()) return;

        if (duplicate_previewingPair == null) {
            duplicate_previewPair(duplicate_pairs.get(0));
        } else {
            int i = duplicate_pairs.indexOf(duplicate_previewingPair);
            if (i > 0) {
                duplicate_previewPair(duplicate_pairs.get(i - 1));
            } else {
                duplicate_previewPair(duplicate_pairs.get(0));
            }
        }
    }

    private void duplicate_previewNextPair() {
        if (duplicate_pairs == null || duplicate_pairs.isEmpty()) return;

        if (duplicate_previewingPair == null) {
            duplicate_previewPair(duplicate_pairs.get(0));
        } else {
            int i = duplicate_pairs.indexOf(duplicate_previewingPair);
            if (i >= 0) {
                if (i + 1 < duplicate_pairs.size()) duplicate_previewPair(duplicate_pairs.get(i + 1));
            } else {
                duplicate_previewPair(duplicate_pairs.get(0));
            }
        }
    }

    private void duplicate_deleteImage(ImageInfo toDelete, ImageInfo toKeep, boolean deleteFile) {
        int index = duplicate_pairs.indexOf(duplicate_previewingPair);

        menagerie.removeImages(Collections.singletonList(toDelete), deleteFile);

        //Consolidate tags
        if (settings.isConsolidateTags()) {
            toDelete.getTags().forEach(toKeep::addTag);
        }

        //Remove other pairs containing the deleted image
        for (SimilarPair pair : new ArrayList<>(duplicate_pairs)) {
            if (toDelete.equals(pair.getImg1()) || toDelete.equals(pair.getImg2())) {
                int i = duplicate_pairs.indexOf(pair);
                duplicate_pairs.remove(pair);
                if (i < index) {
                    index--;
                }
            }
        }

        if (index > duplicate_pairs.size() - 1) index = duplicate_pairs.size() - 1;

        if (duplicate_pairs.isEmpty()) {
            closeDuplicateScreen();
        } else {
            duplicate_previewPair(duplicate_pairs.get(index));
        }
    }

    private void duplicate_compareAndShow(List<ImageInfo> images) {
        if (settings.isComputeMD5ForSimilarity()) {
            List<Runnable> queue = new ArrayList<>();

            images.forEach(i -> {
                if (i.getMD5() == null) queue.add(() -> {
                    try {
                        i.initializeMD5();
                        i.commitMD5ToDatabase();
                    } catch (Exception e) {
                        Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute MD5", "Exception was thrown while trying to compute MD5 for file: " + i, "Unknown")));
                    }
                });
            });

            new ProgressScreen().open(screenPane, "Building MD5s", "Building MD5 hashes for " + queue.size() + " files...", queue, total -> {
                //TODO: Fix this. If md5 computing is disabled, histogram building won't happen
                if (settings.isComputeHistogramForSimilarity()) {
                    List<Runnable> queue2 = new ArrayList<>();

                    images.forEach(i -> {
                        String filename = i.getFile().getName().toLowerCase();
                        if (i.getHistogram() == null && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp")))
                            queue2.add(() -> {
                                try {
                                    i.initializeHistogram();
                                    i.commitHistogramToDatabase();
                                } catch (Exception e) {
                                    Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute histogram", "Exception was thrown while trying to compute a histogram for file: " + i, "Unknown")));
                                }
                            });
                    });

                    Platform.runLater(() -> new ProgressScreen().open(screenPane, "Building Histograms", "Building histograms for " + queue2.size() + " files...", queue2, total1 -> Platform.runLater(() -> openDuplicateScreen(images)), null));
                } else {
                    Platform.runLater(() -> openDuplicateScreen(images));
                }
            }, null);
        } else {
            openDuplicateScreen(images);
        }
    }

    private void startWatchingFolderForImages() {
        if (folderWatcherThread != null) {
            folderWatcherThread.stopWatching();
        }

        if (settings.isAutoImportFromFolder()) {
            File watchFolder = new File(settings.getImportFromFolderPath());
            if (watchFolder.exists() && watchFolder.isDirectory()) {
                folderWatcherThread = new FolderWatcherThread(watchFolder, FILE_FILTER, 30000, files -> {
                    for (File file : files) {
                        if (settings.isAutoImportFromFolderToDefault()) {
                            String folder = settings.getDefaultFolder();
                            if (!folder.endsWith("/") && !folder.endsWith("\\")) folder += "/";
                            File f = new File(folder + file.getName());
                            if (file.equals(f)) continue; //File is being "moved" to same folder

                            File dest = resolveDuplicateFilename(f);

                            if (!file.renameTo(dest)) {
                                continue;
                            }

                            file = dest;
                        }

                        if (menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport()) == null) {
                            if (!file.delete())
                                System.out.println("Failed to delete file after it was denied by the Menagerie");
                        }
                    }
                });
                folderWatcherThread.setDaemon(true);
                folderWatcherThread.start();
            }
        }
    }

    private void cleanExit(boolean revertDatabase) {
        previewMediaView.releaseMediaPlayer();
        slideshowScreen.releaseMediaPlayer();
        duplicate_leftMediaView.releaseMediaPlayer();
        duplicate_rightMediaView.releaseMediaPlayer();
        VideoThumbnailThread.releaseThreads();

        trySaveSettings();

        new Thread(() -> {
            try {
                System.out.println("Attempting to shut down Menagerie database and defragment the file");
                menagerie.getDatabase().createStatement().executeUpdate("SHUTDOWN DEFRAG;");
                System.out.println("Done defragging database file");

                if (revertDatabase) {
                    File database = getDatabaseFile(settings.getDbUrl());
                    File backup = new File(database + ".bak");
                    try {
                        Files.move(backup.toPath(), database.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();

        Platform.exit();
    }

    private void tryDeleteCurrentSlideShowImage(boolean deleteFile) {
        //TODO: Extract this functionality to the slideshow screen

        SimplePokeListener onFinish = () -> {
            menagerie.removeImages(Collections.singletonList(slideshowScreen.getShowing()), deleteFile);
            slideshowScreen.removeCurrent();
        };

        if (deleteFile) {
            new ConfirmationScreen().open(screenPane, "Delete files", "Permanently delete selected files? (1 file)\n\n" +
                    "This action CANNOT be undone (files will be deleted)", onFinish, null);
        } else {
            new ConfirmationScreen().open(screenPane, "Forget files", "Remove selected files from database? (1 file)\n\n" +
                    "This action CANNOT be undone", onFinish, null);
        }
    }

    // ---------------------------------- Compute Utilities ------------------------------------

    private static void downloadAndSaveFile(String url, File target) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.addRequestProperty("User-Agent", "Mozilla/4.0");
        ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
        FileOutputStream fos = new FileOutputStream(target);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        conn.disconnect();
        rbc.close();
        fos.close();
    }

    private static List<File> getFilesRecursively(File folder, FileFilter filter) {
        File[] files = folder.listFiles();
        List<File> results = new ArrayList<>();
        if (files == null) return results;

        for (File file : files) {
            if (file.isDirectory()) {
                results.addAll(getFilesRecursively(file, filter));
            } else {
                if (filter.accept(file)) results.add(file);
            }
        }
        return results;
    }

    private static void backUpDatabase(String databaseURL) throws IOException {
        File dbFile = getDatabaseFile(databaseURL);

        if (dbFile.exists()) {
            System.out.println("Backing up database at: " + dbFile);
            File backupFile = new File(dbFile.getAbsolutePath() + ".bak");
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Successfully backed up database to: " + backupFile);
        }
    }

    private static File getDatabaseFile(String databaseURL) {
        String path = databaseURL + ".mv.db";
        if (path.startsWith("~")) {
            String temp = System.getProperty("user.home");
            if (!temp.endsWith("/") && !temp.endsWith("\\")) temp += "/";
            path = path.substring(1);
            if (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);

            path = temp + path;
        }

        return new File(path);
    }

    private static File resolveDuplicateFilename(File file) {
        while (file.exists()) {
            String name = file.getName();
            if (name.matches(".*\\s\\([0-9]+\\)\\..*")) {
                int count = Integer.parseInt(name.substring(name.lastIndexOf('(') + 1, name.lastIndexOf(')')));
                name = name.substring(0, name.lastIndexOf('(') + 1) + (count + 1) + name.substring(name.lastIndexOf(')'));
            } else {
                name = name.substring(0, name.lastIndexOf('.')) + " (2)" + name.substring(name.lastIndexOf('.'));
            }

            String parent = file.getParent();
            if (!parent.endsWith("/") && !parent.endsWith("\\")) parent += "/";
            file = new File(parent + name);
        }

        return file;
    }

    private void trySaveSettings() {
        try {
            settings.saveToFile();
        } catch (IOException e1) {
            Platform.runLater(() -> errorsScreen.addError(new TrackedError(e1, TrackedError.Severity.HIGH, "Unable to save properties", "IO Exception thrown while trying to save properties file", "1.) Application may not have write privileges\n2.) File may already be in use")));
        }
    }

    // ---------------------------------- Action Event Handlers ------------------------------------

    public void searchButtonOnAction(ActionEvent event) {
        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
        imageGridView.requestFocus();
        event.consume();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
        imageGridView.requestFocus();
        event.consume();
    }

    public void importFilesMenuButtonOnAction(ActionEvent event) {
        openImportFilesDialog();
        event.consume();
    }

    public void importFolderMenuButtonOnAction(ActionEvent event) {
        openImportFolderDialog();
        event.consume();
    }

    public void settingsMenuButtonOnAction(ActionEvent event) {
        openSettingsScreen();
        event.consume();
    }

    public void helpMenuButtonOnAction(ActionEvent event) {
        screenPane.open(helpScreen);
        event.consume();
    }

    public void slideShowSearchedMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, currentSearch.getResults());
        event.consume();
    }

    public void slideShowSelectedMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, imageGridView.getSelected());
        event.consume();
    }

    public void viewTagsMenuButtonOnAction(ActionEvent event) {
        tagListScreen.open(screenPane, menagerie.getTags());
        event.consume();
    }

    public void settings_acceptButtonOnAction(ActionEvent event) {
        closeSettingsScreen(true);
        event.consume();
    }

    public void settings_cancelButtonOnAction(ActionEvent event) {
        closeSettingsScreen(false);
        event.consume();
    }

    public void settings_defaultFolderBrowseButtonOnAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose default save folder");
        if (settings_defaultFolderTextField.getText() != null && !settings_defaultFolderTextField.getText().isEmpty()) {
            File folder = new File(settings_defaultFolderTextField.getText());
            if (folder.exists() && folder.isDirectory()) dc.setInitialDirectory(folder);
        }
        File result = dc.showDialog(settings_rootPane.getScene().getWindow());

        if (result != null) {
            settings_defaultFolderTextField.setText(result.getAbsolutePath());
        }

        event.consume();
    }

    public void settings_importFromFolderBrowseButtonOnAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose auto-import folder");
        if (settings_importFromFolderTextField.getText() != null && !settings_importFromFolderTextField.getText().isEmpty()) {
            File folder = new File(settings_importFromFolderTextField.getText());
            if (folder.exists() && folder.isDirectory()) dc.setInitialDirectory(folder);
        }
        File result = dc.showDialog(settings_rootPane.getScene().getWindow());

        if (result != null) {
            settings_importFromFolderTextField.setText(result.getAbsolutePath());
        }

        event.consume();
    }

    public void settings_autoImportFolderCheckBoxOnAction(ActionEvent event) {
        settings_updateAutoImportFolderDisabledStatus();
        event.consume();
    }

    public void duplicate_leftDeleteButtonOnAction(ActionEvent event) {
        if (duplicate_previewingPair != null)
            duplicate_deleteImage(duplicate_previewingPair.getImg1(), duplicate_previewingPair.getImg2(), true);

        event.consume();
    }

    public void duplicate_rightDeleteButtonOnAction(ActionEvent event) {
        if (duplicate_previewingPair != null)
            duplicate_deleteImage(duplicate_previewingPair.getImg2(), duplicate_previewingPair.getImg1(), true);

        event.consume();
    }

    public void duplicate_closeButtonOnAction(ActionEvent event) {
        closeDuplicateScreen();
        event.consume();
    }

    public void duplicate_prevPairButtonOnAction(ActionEvent event) {
        duplicate_previewLastPair();
        event.consume();
    }

    public void duplicate_nextPairButtonOnAction(ActionEvent event) {
        duplicate_previewNextPair();
        event.consume();
    }

    public void showErrorsButtonOnAction(ActionEvent event) {
        screenPane.open(errorsScreen);
        event.consume();
    }

    public void revertDatabaseMenuButtonOnAction(ActionEvent event) {
        File database = getDatabaseFile(settings.getDbUrl());
        File backup = new File(database + ".bak");
        if (backup.exists()) {
            new ConfirmationScreen().open(screenPane, "Revert database", "Revert to latest backup? (" + new Date(backup.lastModified()) + ")\n\nLatest backup: \"" + backup + "\"\n\nNote: Files will not be deleted!", () -> cleanExit(true), null);
        }
        event.consume();
    }

    // ---------------------------------- Key Event Handlers ----------------------------------------

    public void explorerRootPaneOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
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
                    menagerie.getUpdateQueue().enqueueUpdate(() -> cleanExit(false));
                    menagerie.getUpdateQueue().commit();
                    event.consume();
                    break;
                case S:
                    openSettingsScreen();
                    event.consume();
                    break;
                case T:
                    tagListScreen.open(screenPane, menagerie.getTags());
                    event.consume();
                    break;
                case I:
                    if (event.isShiftDown())
                        openImportFolderDialog();
                    else
                        openImportFilesDialog();
                    event.consume();
                    break;
                case H:
                    screenPane.open(helpScreen);
                    event.consume();
                    break;
                case D:
                    duplicate_compareAndShow(imageGridView.getSelected());
                    event.consume();
                    break;
            }
        }

        switch (event.getCode()) {
            case ESCAPE:
                imageGridView.requestFocus();
                event.consume();
                break;
            case ALT:
                event.consume();
                break;
        }
    }

    public void explorerRootPaneOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.ALT) {
            if (menuBar.isFocused()) {
                imageGridView.requestFocus();
            } else {
                menuBar.requestFocus();
            }
            event.consume();
        }
    }

    public void editTagsTextFieldOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case SPACE:
                editTagsOfSelected(editTagsTextField.getText());
                Platform.runLater(() -> editTagsTextField.setText(null));
                event.consume();
                break;
            case ENTER:
                editTagsOfSelected(editTagsTextField.getText());
                editTagsTextField.setText(null);
                imageGridView.requestFocus();
                event.consume();
                break;
            case ESCAPE:
                editTagsTextField.setText(null);
                imageGridView.requestFocus();
                event.consume();
                break;
        }
    }

    public void settings_rootPaneKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeSettingsScreen(false);
                event.consume();
                break;
            case ENTER:
                closeSettingsScreen(true);
                event.consume();
                break;
            case S:
                if (event.isControlDown()) {
                    closeSettingsScreen(false);
                    event.consume();
                }
                break;
        }
    }

    public void duplicate_rootPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeDuplicateScreen();
                event.consume();
                break;
            case LEFT:
                duplicate_previewLastPair();
                event.consume();
                break;
            case RIGHT:
                duplicate_previewNextPair();
                event.consume();
                break;
        }
    }

    // --------------------------- Mouse Event Handlers -----------------------------------------------

    public void duplicate_imagesPaneMouseEntered(MouseEvent event) {
        duplicate_leftTagListView.setDisable(false);
        duplicate_rightTagListView.setDisable(false);
        duplicate_leftTagListView.setOpacity(0.75);
        duplicate_rightTagListView.setOpacity(0.75);
        event.consume();
    }

    public void duplicate_imagesPaneMouseExited(MouseEvent event) {
        duplicate_leftTagListView.setDisable(true);
        duplicate_rightTagListView.setDisable(true);
        duplicate_leftTagListView.setOpacity(0);
        duplicate_rightTagListView.setOpacity(0);
        event.consume();
    }

}
