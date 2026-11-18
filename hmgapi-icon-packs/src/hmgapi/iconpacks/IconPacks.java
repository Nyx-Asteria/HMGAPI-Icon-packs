package hmgapi.iconpacks;

import hmgapi.hook.LoadedApp;
import hmgapi.hook.ModContext;
import hmgapi.hook.ModEntry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * PNG-first overrides for HMG-API-managed launcher entries.
 * Original factory APKs and their resources are never modified.
 *
 * Load PNGs during zygote initialization, when mod.dir is accessible.
 * Patch the four HMG-API LauncherEntry icon byte arrays in launcher process.
 */
public final class IconPacks extends ModEntry {
    private static final String[] NAMES = {"hmgapi", "tiles", "nemesis", "comfort"};
    private static final String[] PACKAGES = {
        "hmgapi.app", "dev.lemoyne.tiles", "com.nemesis.manager", "dev.lemoyne.comfort"
    };
    private final byte[][] pictures = new byte[NAMES.length][];
    private boolean initialized;

    @Override public void zygoteInit(ModContext mod) {
        for (int i = 0; i < NAMES.length; i++) {
            File file = new File(mod.dir, "icons/" + NAMES[i] + ".png");
            try {
                FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                } finally {
                    in.close();
                }
                byte[] png = out.toByteArray();
                if (png.length < 8 || png[0] != (byte) 137 || png[1] != 80
                        || png[2] != 78 || png[3] != 71) {
                    throw new IllegalStateException("Not a PNG: " + file);
                }
                pictures[i] = png;
                mod.log("Loaded icon " + NAMES[i] + " (" + png.length + " bytes)");
            } catch (Throwable error) {
                mod.error("Cannot load icon " + NAMES[i], error);
            }
        }
    }

    @Override public void appLoaded(ModContext mod, LoadedApp app) {
        if (!"com.android.launcher3".equals(app.packageName) || initialized) return;
        initialized = true;
        if (!mod.settingBoolean("enable_overrides", true)) {
            mod.log("Icon overrides disabled");
            return;
        }

        try {
            Class<?> mods = Class.forName("hmgapi.hook.Mods");
            Method entries = mods.getDeclaredMethod("launcherEntries");
            entries.setAccessible(true);
            List<?> list = (List<?>) entries.invoke(null);
            int replaced = 0;
            for (Object entry : list) {
                Class<?> type = entry.getClass();
                Field packageField = type.getDeclaredField("packageName");
                packageField.setAccessible(true);
                String packageName = (String) packageField.get(entry);
                for (int i = 0; i < PACKAGES.length; i++) {
                    if (!PACKAGES[i].equals(packageName) || pictures[i] == null) continue;
                    for (String fieldName : new String[] {
                            "iconDay", "iconNight", "iconDisabledDay", "iconDisabledNight"
                    }) {
                        Field field = type.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        field.set(entry, pictures[i]);
                    }
                    replaced++;
                    mod.log("Replaced icon for " + packageName);
                    break;
                }
            }
            mod.log("PNG icon overrides applied: " + replaced + "/" + PACKAGES.length);
        } catch (Throwable error) {
            mod.error("Failed to apply PNG icon overrides", error);
        }
    }
}
