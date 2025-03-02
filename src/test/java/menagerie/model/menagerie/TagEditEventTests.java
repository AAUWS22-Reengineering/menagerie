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

package menagerie.model.menagerie;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TagEditEventTests {

  private final Map<Item, List<Tag>> added = new HashMap<>();
  private final Map<Item, List<Tag>> removed = new HashMap<>();

  private final Tag t1 = new Tag(null, 1, "tag1", null);
  private final Tag t2 = new Tag(null, 2, "tag2", null);
  private final Tag t3 = new Tag(null, 3, "tag3", null);
  private final Tag t4 = new Tag(null, 4, "tag4", null);

  private final Item i1 = new MediaItem(null, 1, 1, null);
  private final Item i2 = new MediaItem(null, 2, 1, null);

  private final TagEditEvent editEvent;

  TagEditEventTests() {
    i1.addTag(t1);
    i1.addTag(t2);
    added.put(i1, Arrays.asList(t1, t2));

    i2.addTag(t3);
    i2.addTag(t2);
    added.put(i2, Collections.singletonList(t2));

    removed.put(i1, Arrays.asList(t3, t4));
    removed.put(i2, Arrays.asList(t1, t4));

    // i1 before: t3, t4
    // i1 after:  t1, t2
    // i2 before: t1, t3, t4
    // i2 after:  t2, t3

    editEvent = new TagEditEvent(added, removed);
  }

  @Test
  void mapsCorrect() {
    for (Map.Entry<Item, List<Tag>> entry : added.entrySet()) {
      assertTrue(editEvent.getAdded().containsKey(entry.getKey()));

      List<Tag> tags = added.get(entry.getKey());
      assertEquals(tags.size(), entry.getValue().size());
      for (Tag tag : entry.getValue()) {
        assertTrue(tags.contains(tag));
      }
    }

    for (Map.Entry<Item, List<Tag>> entry : removed.entrySet()) {
      assertTrue(editEvent.getRemoved().containsKey(entry.getKey()));

      List<Tag> tags = removed.get(entry.getKey());
      assertEquals(tags.size(), entry.getValue().size());
      for (Tag tag : entry.getValue()) {
        assertTrue(tags.contains(tag));
      }
    }
  }

  @Test
  void revert() {
    assertTrue(i1.hasTag(t1));
    assertTrue(i1.hasTag(t2));
    assertFalse(i1.hasTag(t3));
    assertFalse(i1.hasTag(t4));

    assertFalse(i2.hasTag(t1));
    assertTrue(i2.hasTag(t2));
    assertTrue(i2.hasTag(t3));
    assertFalse(i2.hasTag(t4));

    editEvent.revertAction();

    assertFalse(i1.hasTag(t1));
    assertFalse(i1.hasTag(t2));
    assertTrue(i1.hasTag(t3));
    assertTrue(i1.hasTag(t4));

    assertTrue(i2.hasTag(t1));
    assertFalse(i2.hasTag(t2));
    assertTrue(i2.hasTag(t3));
    assertTrue(i2.hasTag(t4));
  }

}
