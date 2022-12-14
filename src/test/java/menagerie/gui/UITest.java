package menagerie.gui;

import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/**
 * Base class for all UI tests.
 */
@ExtendWith(ApplicationExtension.class)
public abstract class UITest {

    @BeforeAll
    static void setupHeadless() throws Exception {
        /* Set "headless" property to true to enable headless testing (using -Dheadless=true)
           PLEASE NOTE:
           Headless tests must be run at a MAX resolution of 1280x800.
           Higher resolutions cause buffer overflows */
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }
    }

    @Start
    public void start(Stage stage) {
        stage.show();
    }

    @AfterEach
    public void afterEach() throws Exception {
        FxToolkit.hideStage();
    }
}
