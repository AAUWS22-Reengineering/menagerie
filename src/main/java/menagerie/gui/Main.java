package menagerie.gui;

import com.sun.jna.NativeLibrary;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.version.LibVlcVersion;
import uk.co.caprica.vlcj.version.Version;

import java.io.IOException;

public class Main extends Application {

    public static boolean VLCJ_LOADED = false;


    public static void showErrorMessage(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    public void start(Stage stage) {
        try {
            NativeLibrary.addSearchPath("libvlc", new DefaultWindowsNativeDiscoveryStrategy().discover());
            System.out.println("LibVLC Version: " + LibVlcVersion.getVersion());

            VLCJ_LOADED = true;
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("Error loading vlcj");

            VLCJ_LOADED = false;
        }

        final String splash = "/fxml/splash.fxml";
        final String fxml = "/fxml/main.fxml";
        final String css = "/fxml/dark.css";
        final String title = "Menagerie";

        try {
            Parent root = FXMLLoader.load(getClass().getResource(splash));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(css);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Error", "Unable to load FXML: " + splash, e.getLocalizedMessage());
        }

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(css);

                Stage newStage = new Stage();
                newStage.setScene(scene);
                newStage.setTitle(title);
                newStage.show();
                stage.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });

    }

    public static void main(String[] args) {
        launch(args);
    }

}
