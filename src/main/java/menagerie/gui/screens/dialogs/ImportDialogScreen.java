package menagerie.gui.screens.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import menagerie.gui.Main;
import menagerie.gui.screens.Screen;
import menagerie.model.Settings;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.util.Filters;

import java.io.File;
import java.util.*;

public class ImportDialogScreen extends Screen {

    private enum Order {
        Default, Alphabetical, Date_Modified
    }

    private final TextField filesTextField = new TextField();
    private final ChoiceBox<Order> orderChoiceBox = new ChoiceBox<>();
    private final CheckBox recursiveCheckBox = new CheckBox("Recursively import folders");
    private final CheckBox tagWithParentCheckBox = new CheckBox("Tag with parent folder name");
    private final CheckBox tagWithTagsCheckBox = new CheckBox("Tag with (space-separated tags):");
    private final CheckBox renameWithHashCheckBox = new CheckBox("Rename file to hash after import");
    private final TextField tagWithTagsTextField = new TextField();
    private final ImporterThread importer;
    private final Menagerie menagerie;
    private List<File> files = new ArrayList<>();
    private File lastFolder = null;

    public ImportDialogScreen(Settings settings, Menagerie menagerie, ImporterThread importer) {
        this.menagerie = menagerie;
        this.importer = importer;

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.O && event.isControlDown()) {
                if (event.isShiftDown()) {
                    browseFoldersDialog(settings);
                } else {
                    browseFilesDialog(settings);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                importOnAction();
                event.consume();
            }
        });


        // ---------------------------------- Header ---------------------------------------
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        BorderPane header = new BorderPane(null, null, exit, new Separator(), new Label("Import local files and folders"));
        setPadding(new Insets(0, 0, 0, 5));

        // ---------------------------------- Center ---------------------------------------
        // Files option
        filesTextField.setEditable(false);
        HBox.setHgrow(filesTextField, Priority.ALWAYS);
        Button browseFiles = new Button("Browse Files");
        browseFiles.setOnAction(event -> browseFilesDialog(settings));
        Button browseFolders = new Button("Browse Folders");
        browseFolders.setOnAction(event -> browseFoldersDialog(settings));
        HBox fileHBox = new HBox(5, filesTextField, browseFiles, browseFolders);
        // Order option
        orderChoiceBox.getItems().addAll(Order.values());
        orderChoiceBox.getSelectionModel().selectFirst();
        HBox orderHBox = new HBox(5, new Label("Import files in order:"), orderChoiceBox);
        orderHBox.setAlignment(Pos.CENTER_LEFT);
        // Tag on import
        HBox.setHgrow(tagWithTagsTextField, Priority.ALWAYS);
        tagWithTagsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> tagWithTagsTextField.setDisable(!newValue));
        tagWithTagsTextField.setDisable(true);
        HBox tagHBox = new HBox(5, tagWithTagsCheckBox, tagWithTagsTextField);
        tagHBox.setAlignment(Pos.CENTER_LEFT);
        VBox center = new VBox(5, fileHBox, orderHBox, recursiveCheckBox, tagWithParentCheckBox, renameWithHashCheckBox, tagHBox);
        center.setPadding(new Insets(5));

        // ----------------------------------- Bottom --------------------------------------
        Button accept = new Button("Import");
        accept.setOnAction(event -> importOnAction());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, accept, cancel);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(5));

        // --------------------------------- Root ------------------------------------------
        BorderPane root = new BorderPane(center, header, null, bottom, null);
        root.setMaxSize(600, 250);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(accept);
    }

    private void browseFoldersDialog(Settings settings) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Import folder...");
        if (lastFolder != null) {
            dc.setInitialDirectory(lastFolder);
        } else if (settings.getString(Settings.Key.DEFAULT_FOLDER) != null) {
            File f = new File(settings.getString(Settings.Key.DEFAULT_FOLDER));
            if (f.isDirectory()) dc.setInitialDirectory(f);
        }
        File folder = dc.showDialog(getScene().getWindow());
        if (folder != null) {
            files = new ArrayList<>();
            files.add(folder);
            lastFolder = folder.getParentFile();
            filesTextField.setText(folder.toString());
        }
    }

    private void browseFilesDialog(Settings settings) {
        FileChooser fc = new FileChooser();
        fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
        fc.setTitle("Import files...");

        if (lastFolder != null) {
            fc.setInitialDirectory(lastFolder);
        } else if (settings.getString(Settings.Key.DEFAULT_FOLDER) != null) {
            File f = new File(settings.getString(Settings.Key.DEFAULT_FOLDER));
            if (f.isDirectory()) fc.setInitialDirectory(f);
        }
        List<File> result = fc.showOpenMultipleDialog(getScene().getWindow());
        if (result != null && !result.isEmpty()) {
            files = new ArrayList<>(result);
            if (files.size() > 1) {
                filesTextField.setText(String.format("%d files", files.size()));
            } else {
                filesTextField.setText(files.get(0).toString());
            }
        }
    }

    @Override
    protected void onOpen() {
        filesTextField.setText(null);
    }

    private void importOnAction() {
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            if (file.isDirectory()) {
                for (File f2 : Objects.requireNonNull(file.listFiles())) {
                    if (Filters.FILE_NAME_FILTER.accept(f2) || (recursiveCheckBox.isSelected() && f2.isDirectory())) {
                        files.add(f2);
                    }
                }
                files.remove(i);
                i--;
            } else if (menagerie.isFilePresent(file)) {
                files.remove(i);
                i--;
            } else if (!Filters.FILE_NAME_FILTER.accept(file)) {
                files.remove(i);
                i--;
            }
        }

        switch (orderChoiceBox.getValue()) {
            case Date_Modified:
                files.sort(Comparator.comparingLong(File::lastModified));
                break;
            case Alphabetical:
                files.sort(Comparator.comparing(File::getName));
                break;
            case Default:
            default:
                break;
        }

        for (File file : files) {
            final ImportJob job = new ImportJob(file);
            final List<String> tagsToAdd = new ArrayList<>();

            if (tagWithParentCheckBox.isSelected()) tagsToAdd.add(file.getParentFile().getName().toLowerCase());
            if (tagWithTagsCheckBox.isSelected() && tagWithTagsTextField.getText() != null && !tagWithTagsTextField.getText().isEmpty()) {
                tagsToAdd.addAll(Arrays.asList(tagWithTagsTextField.getText().toLowerCase().split("\\s")));
            }

            final boolean renameToHash = renameWithHashCheckBox.isSelected();

            if (!tagsToAdd.isEmpty() || renameToHash) {
                job.addStatusListener(status -> {
                    if (status == ImportJob.Status.SUCCEEDED) {
                        // Add tags
                        for (String tagName : tagsToAdd) {
                            if (tagName.contains(" "))
                                tagName = tagName.replaceAll("\\s", "_"); // Replace all whitespace

                            if (!tagName.matches(Tag.NAME_REGEX)) continue;

                            Tag t = menagerie.getTagByName(tagName);
                            if (t == null) t = menagerie.createTag(tagName);
                            job.getItem().addTag(t);
                        }

                        // Rename to hash
                        if (renameToHash && job.getItem().getMD5() != null) {
                            File dest = new File(job.getFile().getParentFile(), job.getItem().getMD5() + job.getFile().getName().substring(job.getFile().getName().lastIndexOf('.')));
                            if (job.getItem().moveFile(dest)) {
                                Main.log.info(String.format("Renamed file \"%s\" to \"%s\"", job.getFile().getName(), dest.getName()));
                            } else {
                                Main.log.warning(String.format("Failed to rename file \"%s\" to \"%s\"", job.getFile(), dest));
                            }
                        }
                    }
                });
            }

            importer.addJob(job);
        }

        close();
    }

}
