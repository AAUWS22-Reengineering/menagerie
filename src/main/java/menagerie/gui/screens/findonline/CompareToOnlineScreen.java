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

package menagerie.gui.screens.findonline;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import menagerie.duplicates.Match;
import menagerie.gui.Main;
import menagerie.gui.media.PanZoomImageView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.CancellableThread;
import menagerie.util.Util;
import menagerie.util.listeners.PokeListener;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class CompareToOnlineScreen extends Screen {

    private final static Insets ALL5 = new Insets(5);

    private final PanZoomImageView itemView = new PanZoomImageView(), matchView = new PanZoomImageView();
    private final ProgressIndicator loadingIndicator = new ProgressIndicator(-1);
    private final StackPane rightStackPane;

    private MediaItem currentItem = null;
    private Match currentMatch = null;
    private final ObjectProperty<File> tempImageFile = new SimpleObjectProperty<>();

    private final Set<PokeListener> successListeners = new HashSet<>();


    CompareToOnlineScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        setPadding(new Insets(25));
        BorderPane root = new BorderPane();
        root.getStyleClass().addAll(ROOT_STYLE_CLASS);
        setCenter(root);

        BorderPane header = new BorderPane();
        root.setTop(header);

        Label title = new Label("Find duplicates online");
        header.setLeft(title);
        BorderPane.setMargin(title, ALL5);

        Button exit = new Button("X");
        header.setRight(exit);
        exit.setOnAction(event -> close());

        header.setBottom(new Separator());

        BorderPane leftBorderPane = new BorderPane(itemView);
        VBox.setVgrow(leftBorderPane, Priority.ALWAYS);
        VBox leftVBox = new VBox(5, new Label("Your image"), leftBorderPane);
        loadingIndicator.setMaxSize(50, 50);
        rightStackPane = new StackPane(matchView, loadingIndicator);
        rightStackPane.setAlignment(Pos.CENTER);
        VBox.setVgrow(rightStackPane, Priority.ALWAYS);
        VBox rightVBox = new VBox(5, new Label("Online Match"), rightStackPane);
        rightVBox.setAlignment(Pos.TOP_RIGHT);
        SplitPane sp = new SplitPane(leftVBox, rightVBox);
        root.setCenter(sp);


        Button openPage = new Button("Open web page");
        openPage.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create(currentMatch.getPageURL()));
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Unable to open page url: " + currentMatch.getPageURL(), e);
            }
        });
        Button replace = new Button("Replace local");
        replace.setDisable(true);
        replace.setOnAction(event -> replaceOnAction());
        tempImageFile.addListener((observable, oldValue, newValue) -> replace.setDisable(newValue == null));
        BorderPane bottom = new BorderPane(null, null, replace, null, openPage);
        bottom.setPadding(ALL5);
        root.setBottom(bottom);
    }

    private void replaceOnAction() {
        if (tempImageFile.get() != null && tempImageFile.get().exists()) {
            if (tempImageFile.get().renameTo(currentItem.getFile())) {
                reInitCurrentItem();
                pokeSuccessListeners();
                close();
            } else {
                File temp = Paths.get(System.getProperty("java.io.tmpdir")).resolve("menagerie-compare-temp").toFile();
                if (temp.delete()) Main.log.info("Deleted existing temp file: " + temp);

                File target = new File(currentItem.getFile().getAbsolutePath());

                if (currentItem.getFile().renameTo(temp)) {
                    if (tempImageFile.get().renameTo(target)) {
                        if (!temp.delete()) Main.log.warning("Failed to delete temp file: " + temp);
                        reInitCurrentItem();
                        pokeSuccessListeners();
                        close();
                    } else {
                        if (!currentItem.getFile().renameTo(target)) Main.log.severe("Failed to put original file (" + currentItem.getFile() + ") back in place: " + target);
                        new AlertDialogScreen().open(getManager(), "Unable to replace", "Failed to replace file. System does not allow file replace", null);
                    }
                } else {
                    new AlertDialogScreen().open(getManager(), "Unable to replace", "Failed to replace file. System does not allow file replace", null);
                }
            }
        }
    }

    private void reInitCurrentItem() {
        currentItem.initializeMD5();
        currentItem.initializeHistogram();
        currentItem.setHasNoSimilar(false);
        currentItem.purgeThumbnail();
        currentItem.purgeImage();
    }

    public void open(ScreenPane manager, MediaItem item, Match match) {
        if (item == null || match == null) {
            throw new NullPointerException("Must not be null");
        }

        manager.open(this);
        this.currentItem = item;
        this.currentMatch = match;
        tempImageFile.set(null);

        itemView.setImage(item.getImage());
        matchView.setImage(null);
        loadingIndicator.setProgress(0);
        rightStackPane.getChildren().remove(loadingIndicator);

        if (match.getImageURL() != null && !match.getImageURL().isEmpty()) {
            rightStackPane.getChildren().add(loadingIndicator);
            CancellableThread ct = new CancellableThread() {
                long lastUpdate = 0;
                final long UPDATE_INTERVAL = 17;

                @Override
                public void run() {
                    try {
                        tempImageFile.set(File.createTempFile("menagerie", match.getImageURL().substring(match.getImageURL().lastIndexOf("."))));

                        // Download to temp file
                        HttpURLConnection conn = (HttpURLConnection) new URL(match.getImageURL()).openConnection();
                        conn.addRequestProperty("User-Agent", "Mozilla/4.0");
                        try (ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream())) {
                            try (FileOutputStream fos = new FileOutputStream(tempImageFile.get())) {
                                final long size = conn.getContentLengthLong();
                                final int chunkSize = 4096;
                                for (int i = 0; i < size; i += chunkSize) {
                                    if (!Util.equalsNullable(match, currentMatch)) cancel();
                                    if (!running) break;
                                    fos.getChannel().transferFrom(rbc, i, chunkSize);

                                    final long time = System.currentTimeMillis();
                                    if (time > lastUpdate + UPDATE_INTERVAL) {
                                        lastUpdate = time;
                                        final double finalI = i;
                                        Platform.runLater(() -> loadingIndicator.setProgress(finalI / size));
                                    }
                                }
                            }
                        } finally {
                            conn.disconnect();
                        }

                        tempImageFile.get().deleteOnExit();
                        Platform.runLater(() -> {
                            if (running) {
                                matchView.setImage(new Image(tempImageFile.get().toURI().toString()));
                                Platform.runLater(matchView::fitImageToView);
                            }

                            rightStackPane.getChildren().remove(loadingIndicator);
                        });
                    } catch (IOException e) {
                        Main.log.log(Level.SEVERE, "Failed to download image", e);
                    }
                }
            };
            ct.setName("FindOnline Image Downloader");
            ct.setDaemon(true);
            ct.start();
        } else {
            matchView.setImage(new Image(match.getThumbnailURL()));
        }
        Platform.runLater(itemView::fitImageToView);
    }

    @Override
    protected void onClose() {
        File f = tempImageFile.get();
        if (f != null) {
            if (f.exists() && !f.delete()) Main.log.warning("Failed to delete image temp file: " + f);
            tempImageFile.set(null);
        }
    }

    private void pokeSuccessListeners() {
        successListeners.forEach(PokeListener::poke);
    }

    public boolean addSuccessListener(PokeListener listener) {
        return successListeners.add(listener);
    }

    public boolean removeSuccessListener(PokeListener listener) {
        return successListeners.remove(listener);
    }

}
