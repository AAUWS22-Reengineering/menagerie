package menagerie.gui.util;

import javafx.collections.FXCollections;
import javafx.scene.input.Dragboard;
import menagerie.gui.UITest;
import menagerie.gui.grid.ItemGridView;
import menagerie.model.menagerie.ItemTestUtils;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class GridViewUtilTest extends UITest {

  private final MediaItem m = new MediaItem(null, 1, 1, ItemTestUtils.WHITE_IMAGE_FILE);

  private ItemGridView getItemGridViewMock() {
    ItemGridView itemGridViewMock = mock(ItemGridView.class);
    when(itemGridViewMock.getSelected())
        .thenReturn(FXCollections.observableArrayList(
            ItemTestUtils.getGroup(),
            m));
    return itemGridViewMock;
  }

  @Test
  void testDragAndDrop() {
    ItemGridView itemGridView = getItemGridViewMock();
    // force thumbnail creation
    m.getThumbnail().want();

    await()
        .atMost(Duration.ofMillis(UITest.MAX_WAIT_TIME))
        .until(() -> m.getThumbnail().isLoaded());

    Dragboard dbMock = mock(Dragboard.class);
    GridViewUtil.doDragAndDrop(dbMock, itemGridView);

    verify(dbMock, times(1)).setDragView(m.getThumbnail().getImage());
  }

  @Test
  void testGetSelectedFiles() {
    ItemGridView itemGridView = getItemGridViewMock();

    List<File> selectedFiles = GridViewUtil.getSelectedFiles(itemGridView);
    assertEquals(1, selectedFiles.size());
    assertEquals(m.getFile(), selectedFiles.get(0));
  }
}
