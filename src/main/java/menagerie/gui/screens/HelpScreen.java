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

package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import menagerie.model.menagerie.Tag;
import menagerie.util.Filters;

import java.util.Map;

public class HelpScreen extends Screen {

  private static final String[] GENERAL = {
      "- Tagging", "- Searching tags and various properties",
      String.format("- Support for image formats: %s", String.join(", ", Filters.IMAGE_EXTS)),
      String.format("- Support for video formats (requires VLC to be installed): %s",
          String.join(", ", Filters.VIDEO_EXTS)), "- Exact file duplicate detection",
      "- Image fuzzy duplicate detection", "- Item groups", "- Auto-importing from a folder",
      "- Dragging and dropping files to and from local disk and web", "- Slideshow viewing",
      "- Etc."
  };
  private static final String[] SEARCHING =
      new String[] {"LEGEND:", "  (___) = Optional ___", "  [___] = Required ___", "  | = Or",
          " ", "[TAG]", "-[RULE]", "id:(<|>)[INT]", "[time|date]:(<|>)[LONG]",
          "[path|file]:[STRING]", "missing:[md5|hist|histogram|file]", "type:[group|media]",
          "tags:(<|>)[INT]"};

  private static final String EXPLORER_TAB_TITLE =
      "The explorer is the main screen; the unified hub for viewing, searching, and editing everything in your Menagerie.\n ";
  private static final Map<String, String> EXPLORER_TAB_CONTENTS = Map.of(
      "Preview info",
      "- Click the info box in the bottom right of the preview to toggle the full view with more info about the selected item.",
      "Previewing images",
      "- Click to toggle between 100% zoom and fitted zoom\n- Click and drag to pan the image around\n- Use the scroll wheel to zoom in increments",
      "Previewing videos",
      "- Left click the video to pause or resume it\n- Right click the video to toggle mute\n- Scroll with mouse over the video to advance or retreat 10 seconds of video"
  );
  private static final String GENERAL_TAB_TITLE =
      "Welcome to Menagerie, an organizational tool images and videos.\n\nMenagerie is a tag based system that tracks local files on your PC, allowing you organize, search, and view them faster and more efficiently.\n ";
  private static final String TAG_TAB_NOTE_1 = String.format(
      "Notes:\n    - When editing tags with the tag edit field (bottom left of the screen), edit will affect all selected items.\n    - Tag names must match the regex: \"%s\"\n        Or, in other words, all characters must be alphanumerical or standard US keyboard symbols.\n    - Tag edits can be undone by pressing CTRL+Z.\n ",
      Tag.NAME_REGEX);
  private static final String TAG_TAB_NOTE_2 =
      "Useful hotkeys:\n    Ctrl+E - Focuses the tag editor\n    Ctrl+Space or Tab - Autocomplete a tag name\n    Ctrl+Enter - Autocomplete a tag and commit the edit\n ";

  private static final Map<String, String> TAG_TAB_CONTENTS = Map.of(
      "Adding tags",
      "To add a tag, type a tag name into the tag editor field and press enter.\nMultiple tags can be typed out (separated by spaces) before committing the changes.\nAdding a tag name that does not exist will create a new tag.",
      "Removing tags",
      "To remove a tag, type a tag name into the tag editor field preceded by a dash (aka. Minus, or '-') and press enter.\nMultiple tags can be typed out (separated by spaces) before committing the changes.",
      "Auto completion",
      "When typing tags into the tag editor field, partial tag names can be auto-completed to existing tag names.\nPress TAB or CTRL+SPACE to complete the tag name with the first prediction.\nPress CTRL+ENTER to complete the tag name with the first prediction and then commit the edit.\nUse UP or DOWN arrow keys and SPACE or ENTER to choose other predictions."
  );
  private static final String SEARCHING_TAB_NOTE_1 =
      "Notes:\n    - Searching parses the input as a space-separated set of rules.\n    - A rule either has a prefix defining a special rule, or it is treated as a tag rule.\n ";
  private static final String SEARCHING_TAB_NOTE_2 =
      "Useful hotkeys:\n    Ctrl+F - Focuses the search field\n    Ctrl+Space or Tab - Autocomplete a tag name\n    Ctrl+Enter - Autocomplete a tag and search\n ";

  private static final Map<String, String> SEARCHING_TAB_CONTENTS = Map.of(
      "Excluding items",
      "Prefix any search rule with a dash (a.k.a. minus, '-') to logically invert it.\n\nExamples:\n\"-tagme\" - excludes all items tagged with 'tagme'\n\"-type:group\" - excludes all items that are group items",
      "Requiring tags",
      "Use the exact name of a tag to require it.\n\nExamples:\n\"tagme\" - includes only items tagged with tagme\n\"test_tag_1 test_tag_2\" - includes only items tagged with test_tag_1 AND test_tag_2",
      "Specifying types",
      "Use the type modifier (type:) to include specific types of items.\n\nExamples:\n\"type:group\" - includes only group items\n\"type:media\" - includes only media items",
      "Specifying IDs",
      "Use the ID modifier (id:) to search for specific IDs or IDs in a range.\n\nExamples:\n\"id:>1234\" - includes only items whose ID is greater than 1234\n\"id:4321\" - includes only the item whose id is 4321",
      "Search by time",
      "Use the time modifier (time:) to search for a specific time that items were added.\n\nExamples:\n\"time:<1541361629900\" - includes only items that were added before the given time in milliseconds after epoch\n\"time:1541361629900\" - includes only items that were added at exactly the given time after epoch",
      "Search by file path",
      "Use the path modifier (path:) to search for file paths that contain the a string.\n\nExamples:\n\"path:C:/Users/PERSON/Documents\" - includes only files in PERSON's Documents folder",
      "Missing attributes",
      "Use the missing modifier (missing:) to search for items that missing certain attributes.\n\nExamples:\n\"missing:md5\" - only includes items that are missing an MD5 hash\n\"missing:file\" - includes only items that point to a non-existent file\n\"missing:histogram\" - includes only items that are missing a histogram"
  );

  private static final Map<String, String> HOT_KEYS = Map.ofEntries(
      Map.entry("Ctrl+H", "- Show this menu"),
      Map.entry("Ctrl+F", "- Focus search bar"),
      Map.entry("Ctrl+E", "- Focus tag editor"),
      Map.entry("Ctrl+Q", "- Quit"),
      Map.entry("Ctrl+S", "- Show settings"),
      Map.entry("Ctrl+T", "- Show tag list"),
      Map.entry("Ctrl+N", "- Show import notifications"),
      Map.entry("Ctrl+D", "- Find duplicates among currently selected items"),
      Map.entry("Ctrl+I", "- Show import dialog for files"),
      Map.entry("Ctrl+Shift+I", "- Show import dialog for folders (recursive)"),
      Map.entry("Del", "- Delete currently selected items"),
      Map.entry("Ctrl+Del", "- Forget currently selected items from database"),
      Map.entry("Ctrl+G", "- Group all selected items together"),
      Map.entry("Ctrl+U", "- Ungroup all selected groups")
  );

  private final Font boldItalic = new Font("System Bold Italic", 12);
  private final Insets left20 = new Insets(0, 0, 0, 20);
  private final Insets all5 = new Insets(5);

  public HelpScreen() {
    initHelpScreen();
  }

  private void initHelpScreen() {
    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        close();
      }
    });

    setPadding(new Insets(25));
    BorderPane root = new BorderPane();
    root.setPrefSize(800, 800);
    root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    root.getStyleClass().addAll(ROOT_STYLE_CLASS);
    setCenter(root);

    BorderPane header = new BorderPane();
    root.setTop(header);

    Label title = new Label("Help");
    header.setLeft(title);
    BorderPane.setMargin(title, all5);

    Button exit = new Button("X");
    header.setRight(exit);
    exit.setOnAction(event -> close());

    header.setBottom(new Separator());

    // ---------------------------------- Search help tab ----------------------------------------------------------
    TabPane tabPane =
        new TabPane(constructGeneralTab(), constructExplorerTab(), constructTagEditingTab(),
            constructSearchingTab(), constructHotKeysTab());
    root.setCenter(tabPane);
  }

  private Tab constructExplorerTab() {
    VBox v = new VBox(5);
    addSection(v, EXPLORER_TAB_TITLE);
    EXPLORER_TAB_CONTENTS.forEach((t, d) -> addLabels(v, t, d));
    final var sp = new ScrollPane(v);
    return createTab(sp, "Explorer");
  }

  private Tab constructGeneralTab() {
    VBox v = new VBox(5);
    addSection(v, GENERAL_TAB_TITLE);
    addHeading(v, "Features");
    for (String str : GENERAL) {
      addDescription(v, str);
    }
    final var sp = new ScrollPane(v);
    return createTab(sp, "General");
  }

  private Tab constructTagEditingTab() {
    VBox v = new VBox(5);
    addSection(v, TAG_TAB_NOTE_1);
    addSection(v, TAG_TAB_NOTE_2);
    TAG_TAB_CONTENTS.forEach((t, d) -> addLabels(v, t, d));
    final var sp = new ScrollPane(v);
    return createTab(sp, "Editing Tags");
  }

  private Tab constructSearchingTab() {
    VBox v = new VBox(5);
    addSection(v, SEARCHING_TAB_NOTE_1);
    addSection(v, SEARCHING_TAB_NOTE_2);
    SEARCHING_TAB_CONTENTS.forEach((t, d) -> addLabels(v, t, d));
    v.getChildren().add(new Separator());
    addHeading(v, "Search Rules:");
    for (String str : SEARCHING) {
      addDescription(v, str);
    }
    final var sp = new ScrollPane(v);
    return createTab(sp, "Searching");
  }

  private Tab constructHotKeysTab() {
    VBox v1 = new VBox();
    VBox v2 = new VBox();
    HOT_KEYS.forEach((h, d) -> addHotKeysLabel(v1, v2, h, d));
    HBox h = new HBox(5, v1, v2);
    return createTab(new ScrollPane(h), "Hotkeys");
  }

  private static void addSection(VBox v, String header) {
    Label l = new Label(header);
    l.setWrapText(true);
    v.getChildren().addAll(l, new Separator());
  }

  private void addHotKeysLabel(VBox v1, VBox v2, String hotkey, String description) {
    addHeading(v1, hotkey);
    v2.getChildren().add(new Label(description));
  }

  private Tab createTab(ScrollPane v, String tabTitle) {
    v.setFitToWidth(true);
    v.setPadding(all5);
    Tab t = new Tab(tabTitle, v);
    t.setClosable(false);
    return t;
  }

  private void addLabels(VBox v, String title, String description) {
    addHeading(v, title);
    addDescription(v, description);
  }

  private void addHeading(VBox v, String heading) {
    Label l = new Label(heading);
    l.setFont(boldItalic);
    v.getChildren().add(l);
  }

  private void addDescription(VBox v, String description) {
    Label l = new Label(description);
    l.setPadding(left20);
    l.setWrapText(true);
    v.getChildren().add(l);
  }

}
