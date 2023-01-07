package menagerie.gui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

@ExtendWith(ApplicationExtension.class)
public abstract class UITest {

  public static final int SLEEP_TIME = 350;

  @BeforeAll
  static void setupHeadless() {
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
}
