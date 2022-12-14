package menagerie.gui;

import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StartupIT extends UITest {

    @BeforeAll
    static void setup() throws Exception {
        ApplicationTest.launch(Main.class);
    }

    @Test
    void checkMainGuiVisible(FxRobot robot) throws TimeoutException, InterruptedException {
        // Wait for main GUI to start
        WaitForAsyncUtils.waitFor(5000, TimeUnit.MILLISECONDS, () -> robot.lookup("#menuFile") != null);
        // Without this, the next line fails to find "#menuFile"
        Thread.sleep(1000);

        // Click on Menu and verify that submenu is visible
        robot.clickOn("#menuFile");
        WaitForAsyncUtils.waitFor(100, TimeUnit.MILLISECONDS, () ->
                robot.lookup("#menuItemImportFile") != null);
        Thread.sleep(100);
        FxAssert.verifyThat("#menuItemImportFile", Node::isVisible);
    }
}
