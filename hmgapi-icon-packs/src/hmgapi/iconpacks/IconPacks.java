package hmgapi.iconpacks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
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
    private final java.util.Set<Integer> setupIds = new java.util.HashSet<Integer>();
    private int setupRequests;
    private int setupReplacements;

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

        // RC8: observe factory entries without changing their constructor arguments.
        mod.hook(app.classLoader, "launcher.menu_entry.new", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                try {
                    if (p.args.length < 7) return;
                    String pkg = String.valueOf(p.args[2]);
                    String activity = String.valueOf(p.args[3]);
                    int enabled = ((Number) p.args[4]).intValue();
                    int disabled = ((Number) p.args[5]).intValue();
                    String identity = (pkg + " " + activity).toLowerCase(java.util.Locale.US);
                    if (identity.contains("setting") || identity.contains("setup")) {
                        if (enabled != 0) setupIds.add(enabled);
                        if (disabled != 0) setupIds.add(disabled);
                        mod.log("RC8 Setup candidate " + pkg + "/" + activity
                                + " enabled=0x" + Integer.toHexString(enabled)
                                + " disabled=0x" + Integer.toHexString(disabled));
                    } else {
                        mod.log("RC8 factory entry " + pkg + "/" + activity
                                + " enabled=0x" + Integer.toHexString(enabled));
                    }
                } catch (Throwable error) {
                    mod.error("RC8 entry inspection", error);
                }
            }
        });
        // Existing framework drawable point: replace only IDs observed on Setup.
        mod.hook(null, "framework.resources.get_drawable", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                if (p.hasThrowable() || !mod.settingBoolean("enable_overrides", true)) return;
                try {
                    int id = ((Number) p.args[0]).intValue();
                    if (!setupIds.contains(id)) return;
                    Resources res = (Resources) p.thisObject;
                    setupRequests++;
                    // Resource IDs can overlap between packages. Log the resource
                    // owner; only replace launcher-owned resource IDs.
                    String owner = res.getResourcePackageName(id);
                    if (!"com.android.launcher3".equals(owner)) {
                        if (setupRequests <= 20)
                            mod.log("RC8 Setup skipped resource owner=" + owner);
                        return;
                    }
                    p.setResult(new BitmapDrawable(res, renderSetupTest()));
                    setupReplacements++;
                    if (setupReplacements <= 20)
                        mod.log("RC8 Setup drawable replaced id=0x" + Integer.toHexString(id));
                } catch (Throwable error) {
                    mod.error("RC8 Setup drawable probe", error);
                }
            }
        });
        mod.hook(app.classLoader, "launcher.menu_list.load", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                mod.log("RC8 menu loaded; Setup IDs=" + setupIds.size()
                        + " drawable requests=" + setupRequests
                        + " replacements=" + setupReplacements);
            }
        });

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
    /** Deliberately conspicuous factory-icon test: purple tile and green check. */
    private static Bitmap renderSetupTest() {
        Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(28, 8, 46));
        canvas.drawRoundRect(new RectF(3, 3, 125, 125), 24, 24, paint);
        paint.setColor(Color.MAGENTA);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        canvas.drawRoundRect(new RectF(6, 6, 122, 122), 22, 22, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        Path gear = new Path();
        for (int i = 0; i < 32; i++) {
            double angle = 2 * Math.PI * i / 32 - Math.PI / 2;
            float radius = (i % 4 == 0 || i % 4 == 3) ? 43 : 34;
            float x = 64 + (float) Math.cos(angle) * radius;
            float y = 64 + (float) Math.sin(angle) * radius;
            if (i == 0) gear.moveTo(x, y); else gear.lineTo(x, y);
        }
        gear.close();
        canvas.drawPath(gear, paint);
        paint.setColor(Color.rgb(28, 8, 46));
        canvas.drawCircle(64, 64, 22, paint);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(9);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Path check = new Path();
        check.moveTo(49, 64);
        check.lineTo(60, 76);
        check.lineTo(82, 51);
        canvas.drawPath(check, paint);
        return bitmap;
    }
}
