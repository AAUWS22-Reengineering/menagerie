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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.menagerie.db.DatabaseUtil;
import menagerie.model.menagerie.db.DatabaseVersionUpdater;
import menagerie.model.menagerie.db.MenagerieDatabaseLoadListener;
import menagerie.settings.MenagerieSettings;
import menagerie.settings.OldSettings;
import menagerie.settings.SettingsException;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SplashController {

  private static final Logger LOGGER = Logger.getLogger(SplashController.class.getName());

  private static final String MSG_SEE_LOG = "See log for more details";
  private static final int PROGRESS_UPDATE_INTERVAL = 16;

  @FXML
  private StackPane rootPane;
  @FXML
  private ImageView backgroundImageView;
  @FXML
  private Label titleLabel;
  @FXML
  private Label statusLabel;
  @FXML
  private ProgressBar progressBar;

  private final List<Image> icons;
  private final Image splashBackground;

  private long lastProgressUpdate = 0;


  public SplashController(List<Image> icons, Image splashBackground) {
    this.icons = icons;
    this.splashBackground = splashBackground;
  }

  @FXML
  public void initialize() {
    backgroundImageView.setImage(splashBackground);

    // Set graphic
    initSetGraphic();

    // ------------------------------------------ Startup thread ---------------------------------------------------
    new Thread(() -> {
      final MenagerieSettings settings = new MenagerieSettings();
      loadSettings(settings);

      // --------------------------------------------- Load VLCJ -------------------------------------------------
      loadVLCJ(settings);

      // ----------------------------------------- Back up database ----------------------------------------------
      if (!tryBackupDatabase(settings)) {
        // backup was required but failed
        return;
      }

      // ---------------------------------------- Connect to database --------------------------------------------
      Connection database = connectToDatabase(settings);
      if (database == null) {
        // connection failed
        return;
      }

      // -------------------------------------- Verify/upgrade database ------------------------------------------
      if (!verifyDatabase(settings, database)) {
        // verification failed
        return;
      }

      // ----------------------------------- Connect database manager --------------------------------------------
      DatabaseManager databaseManager = getDatabaseManager(database);
      if (databaseManager == null) {
        // connection failed
        return;
      }
      databaseManager.setLoadListener(getDatabaseLoadListener());
      databaseManager.setDaemon(true);
      databaseManager.start();

      // ------------------------------------ Construct Menagerie ------------------------------------------------
      Menagerie menagerie = constructMenagerie(databaseManager);
      if (menagerie == null) return;

      // --------------------------------- Open main application window ------------------------------------------
      final Menagerie finalMenagerie = menagerie;
      Platform.runLater(() -> openMain(finalMenagerie, settings));

    }, "Startup Thread").start();
  }

  private Menagerie constructMenagerie(DatabaseManager databaseManager) {
    Menagerie menagerie;
    try {
      menagerie = new Menagerie(databaseManager);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Error initializing Menagerie", e);
      Platform.runLater(() -> {
        Main.showErrorMessage("Error while loading data into Menagerie",
            MSG_SEE_LOG, e.getLocalizedMessage());
        Platform.exit();
        System.exit(1);
      });
      return null;
    }
    return menagerie;
  }

  private MenagerieDatabaseLoadListener getDatabaseLoadListener() {
    return new MenagerieDatabaseLoadListener() {
      @Override
      public void startedItemLoading(int total) {
        Platform.runLater(() -> statusLabel.setText("Loading " + total + " items..."));
      }

      @Override
      public void gettingItemList() {
        Platform.runLater(() -> {
          statusLabel.setText("Getting list of items from database...");
          progressBar.setProgress(-1);
        });
      }

      @Override
      public void itemsLoading(int count, int total) {
        updateProgress(count, total);
      }

      @Override
      public void startTagLoading(int total) {
        Platform.runLater(() -> statusLabel.setText("Loading " + total + " tags..."));
      }

      @Override
      public void tagsLoading(int count, int total) {
        updateProgress(count, total);
      }

      private void updateProgress(int count, int total) {
        long time = System.currentTimeMillis();
        if (time - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL) {
          lastProgressUpdate = time;
          Platform.runLater(() -> progressBar.setProgress((double) count / total));
        }
      }

      @Override
      public void gettingNonDupeList() {
        Platform.runLater(() -> {
          statusLabel.setText("Getting non-duplicates list from database...");
          progressBar.setProgress(-1);
        });
      }

      @Override
      public void startNonDupeLoading(int total) {
        Platform.runLater(() -> statusLabel.setText("Loading " + total + " non-duplicates..."));
      }

      @Override
      public void nonDupeLoading(int count, int total) {
        updateProgress(count, total);
      }
    };
  }

  private DatabaseManager getDatabaseManager(Connection database) {
    Platform.runLater(() -> statusLabel.setText("Plugging in database manager..."));
    DatabaseManager databaseManager;
    try {
      databaseManager = new DatabaseManager(database);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Unexpected error while connecting database manager to database",
          e);
      Platform.runLater(() -> {
        Main.showErrorMessage("Error while plugging manager into database",
            MSG_SEE_LOG, e.getLocalizedMessage());
        Platform.exit();
        System.exit(1);
      });
      return null;
    }
    return databaseManager;
  }

  private boolean verifyDatabase(MenagerieSettings settings, Connection database) {
    Platform.runLater(() -> statusLabel.setText(
        "Verifying and upgrading database: " + settings.dbUrl.getValue() + "..."));
    try {
      DatabaseVersionUpdater.updateDatabase(database);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Unexpected error while attempting to verify or upgrade database",
          e);
      Platform.runLater(() -> {
        Main.showErrorMessage("Error while verifying or upgrading database",
            MSG_SEE_LOG, e.getLocalizedMessage());
        Platform.exit();
        System.exit(1);
      });
      return false;
    }
    return true;
  }

  private Connection connectToDatabase(MenagerieSettings settings) {
    Platform.runLater(() -> statusLabel.setText(
        "Connecting to database: " + settings.dbUrl.getValue() + "..."));
    Connection database;
    try {
      database = DriverManager.getConnection("jdbc:h2:" + settings.dbUrl.getValue(),
          settings.dbUser.getValue(), settings.dbPass.getValue());
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, String.format("Error connecting to database: %s", settings.dbUrl.getValue()), e);
      Platform.runLater(() -> {
        Main.showErrorMessage("Error connecting to database",
            "Database is most likely open in another application", e.getLocalizedMessage());
        Platform.exit();
        System.exit(1);
      });
      return null;
    }
    return database;
  }

  private boolean tryBackupDatabase(MenagerieSettings settings) {
    if (settings.dbBackup.getValue()) {
      Platform.runLater(() -> statusLabel.setText("Backing up database..."));
      try {
        backupDatabase(settings.dbUrl.getValue());
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to backup database. Unexpected error occurred.", e);
        LOGGER.info("DB URL: " + settings.dbUrl.getValue());

        Platform.runLater(() -> {
          Main.showErrorMessage("Error while backing up database", MSG_SEE_LOG,
              e.getLocalizedMessage());
          Platform.exit();
          System.exit(1);
        });
        return false;
      }
    }
    return true;
  }

  private void loadVLCJ(MenagerieSettings settings) {
    String vlcj = settings.vlcFolder.getValue();
    if (vlcj != null && vlcj.isEmpty()) {
        vlcj = null;
    }
    Main.loadVLCJ(vlcj);
  }

  private void loadSettings(MenagerieSettings settings) {
    try {
      settings.load(new File(Main.SETTINGS_PATH));
    } catch (FileNotFoundException e) {
      LOGGER.warning("Settings file does not exist");
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error reading settings file", e);
    } catch (JSONException e) {
      LOGGER.warning("JSON error, attempting to read old style settings");
      settings.loadFrom(new OldSettings(new File(Main.SETTINGS_PATH)));
    } catch (SettingsException e) {
      LOGGER.log(Level.SEVERE, "Invalid settings file", e);
    }
  }

  private void initSetGraphic() {
    titleLabel.setGraphicTextGap(10);
    for (Image icon : icons) {
      if (icon.getWidth() == 64) {
        titleLabel.setGraphic(new ImageView(icon));
      }
    }
  }

  private void openMain(Menagerie menagerie, MenagerieSettings settings) {
    try {
      LOGGER.info(String.format("Loading FXML: %s", Main.MAIN_FXML));
      FXMLLoader loader = new FXMLLoader(getClass().getResource(Main.MAIN_FXML));
      loader.setControllerFactory(param -> new MainController(menagerie, settings));
      Parent root = loader.load();
      Scene scene = new Scene(root);
      scene.getStylesheets().add(Main.CSS);

      Stage newStage = new Stage();
      newStage.setScene(scene);
      newStage.setTitle(Main.MAIN_TITLE);
      newStage.getIcons().addAll(icons);
      newStage.show();

      // Close this splash screen
      ((Stage) rootPane.getScene().getWindow()).close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to load FXML: " + Main.MAIN_FXML, e);
      Main.showErrorMessage("Error on initialization",
          "Unknown error occurred while loading main FXML. See log for more info.",
          e.getLocalizedMessage());
      System.exit(1);
    }
  }

  /**
   * Attempts to back up the database file as specified in the settings object
   *
   * @throws IOException When copy fails.
   */
  private static void backupDatabase(String databaseURL) throws IOException {
    File dbFile = DatabaseUtil.resolveDatabaseFile(databaseURL);

    if (dbFile.exists()) {
      LOGGER.info("Backing up database at: " + dbFile);
      File backupFile = new File(dbFile.getAbsolutePath() + ".bak");
      Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      LOGGER.info("Successfully backed up database to: " + backupFile);
    } else {
      LOGGER.warning("Cannot backup nonexistent database file at: " + dbFile);
    }
  }

}
