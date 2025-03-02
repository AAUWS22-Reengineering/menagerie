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

package menagerie.settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class Settings {

  private static final Logger LOGGER = Logger.getLogger(Settings.class.getName());

  private static final String VERSION_KEY = "version";
  private static final String SETTINGS_KEY = "settings";

  private final List<Setting> storedSettings = new ArrayList<>();
  private final int version = 1;


  public void load(File file) throws IOException, SettingsException, JSONException {
    String fileText = String.join("\n", Files.readAllLines(file.toPath()));
    JSONObject json = new JSONObject(fileText);

    if (!json.has(VERSION_KEY)) {
      throw new SettingsException("No version tag");
    }
    if (json.getInt(VERSION_KEY) != version) {
      throw new SettingsException("Unknown settings version");
    }

    if (json.has(SETTINGS_KEY)) {
      JSONArray arr = json.getJSONArray(SETTINGS_KEY);
      for (int i = 0; i < arr.length(); i++) {
        JSONObject j = arr.getJSONObject(i);

        Setting setting = getSetting(j.getString(Setting.ID_KEY));
        if (setting != null) {
          setting.initFromJSON(j);
        } else {
          LOGGER.warning("Unexpected setting: " + j);
        }
      }
    }
  }

  public Setting getSetting(String identifier) {
    for (Setting setting : storedSettings) {
      if (setting.getID().equalsIgnoreCase(identifier)) {
        return setting;
      } else if (setting instanceof GroupSetting) {
        Setting child = ((GroupSetting) setting).getChild(identifier);
        if (child != null) {
          return child;
        }
      }
    }

    return null;
  }

  public FileSetting getFile(String id) {
    return (FileSetting) getSetting(id);
  }

  public List<Setting> getSettings() {
    return storedSettings;
  }

  public int getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Settings) {
      if (getVersion() != ((Settings) obj).getVersion()) {
        return false;
      }
      if (getSettings().size() != ((Settings) obj).getSettings().size()) {
        return false;
      }

      for (int i = 0; i < getSettings().size(); i++) {
        if (!getSettings().get(i).equals(((Settings) obj).getSettings().get(i))) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(storedSettings, version);
  }

  public void save(File file) throws IOException {
    JSONObject json = new JSONObject();
    json.put(VERSION_KEY, getVersion());

    for (Setting setting : storedSettings) {
      json.append(SETTINGS_KEY, setting.toJSON());
    }

    try (FileWriter fw = new FileWriter(file)) {
      json.write(fw, 2, 0);
    }
  }

}
