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
 * Patch HMG-API LauncherEntry icon byte arrays in the launcher process.
 */
public final class IconPacks extends ModEntry {
    private static final String[] NAMES = {"hmgapi", "tiles", "nemesis", "comfort", "buttonmapper", "brakeindicator"};
    private static final String[] PACKAGES = {
        "hmgapi.app", "dev.lemoyne.tiles", "com.nemesis.manager", "dev.lemoyne.comfort",
        "com.deflight.buttonmapper", "dev.lemoyne.brakeindicator"
    };
    private final byte[][] pictures = new byte[NAMES.length][];
    private boolean initialized;
    private final java.util.Map<Integer, String> factoryIds = new java.util.HashMap<Integer, String>();
    private boolean hyundai;
    private int factoryReplacements;

    @Override public void zygoteInit(ModContext mod) {
        hyundai = "hyundai".equals(mod.settingString("icon_pack", "kia"));
        mod.log("Icon pack selected: " + (hyundai ? "hyundai" : "kia"));
        for (int i = 0; i < NAMES.length; i++) {
            File file = new File(mod.dir, "icons/" + (hyundai ? "hyundai" : "kia")
                    + "/" + NAMES[i] + ".png");
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
                    String key = factoryKey(pkg, activity);
                    if (key != null) {
                        if (enabled != 0) factoryIds.put(enabled, key);
                        if (disabled != 0) factoryIds.put(disabled, key);
                        mod.log("RC9 factory " + key + " " + pkg + "/" + activity);
                    } else {
                        mod.log("RC9 unmatched " + pkg + "/" + activity);
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
                    String key = factoryIds.get(id);
                    if (key == null) return;
                    Resources res = (Resources) p.thisObject;
                    if (!"com.android.launcher3".equals(res.getResourcePackageName(id))) return;
                    p.setResult(new BitmapDrawable(res, drawIcon(key, hyundai)));
                    factoryReplacements++;
                    if (factoryReplacements <= 50)
                        mod.log("RC9 factory replaced " + key + " id=0x" + Integer.toHexString(id));
                } catch (Throwable error) {
                    mod.error("RC8 Setup drawable probe", error);
                }
            }
        });
        mod.hook(app.classLoader, "launcher.menu_list.load", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                mod.log("RC9 menu loaded: factory IDs=" + factoryIds.size()
                        + " replacements=" + factoryReplacements);
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

    private static String factoryKey(String pkg, String activity) {
        String p = pkg == null ? "" : pkg.toLowerCase(java.util.Locale.US);
        String a = activity == null ? "" : activity.toLowerCase(java.util.Locale.US);
        if (p.contains("mnsoft.navi")) return a.contains("mapapp") ? "map" : "navigation";
        if (p.equals("com.android.settings")) return a.contains("webmanual") ? "manual" : "setup";
        if (p.contains("bluetooth")) return "phone";
        if (p.contains("radioha")) return "radio";
        if (p.contains("usbmusic")) return "media";
        if (p.contains("voicememo")) return "voicememo";
        if (p.contains("hddata")) return "hddata";
        if (p.contains("vehiclesettings")) return "memoryseat";
        if (p.contains("vrm.main")) return "kiaconnect";
        if (a.contains("rearclimate")) return "rearclimate";
        if (a.contains("climate")) return "climate";
        if (a.contains("seatheat") || a.contains("ventcontrol")) return "warmer";
        if (p.contains("projection") || a.contains("projection")) return "projection";
        if (p.contains("valet") || a.contains("valet")) return "valet";
        if (p.contains("notification") || a.contains("notification")) return "notifications";
        if (p.contains("quiet") || a.contains("quiet")) return "quietmode";
        if (a.contains("evmenu") || a.equals("ev")) return "ev";
        return null;
    }

    /**
     * RC9 preview artwork. Transparent 128x128 bitmap, no tile background.
     * Kia: cyan-purple-magenta strokes. Hyundai: blue-cyan-green gradient
     * and selective fills. No OEM icons are embedded or extracted.
     */
    private static Bitmap drawIcon(String key, boolean hyundai) {
        Bitmap b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(hyundai ? 8 : 6);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        int[] colors = hyundai
                ? new int[]{Color.rgb(21, 126, 250), Color.rgb(37, 220, 230), Color.rgb(74, 238, 143)}
                : new int[]{Color.rgb(35, 211, 255), Color.rgb(135, 83, 245), Color.rgb(250, 57, 209)};
        p.setShader(new android.graphics.LinearGradient(15, 15, 110, 110,
                colors, null, android.graphics.Shader.TileMode.CLAMP));
        if (key.equals("setup") || key.equals("nemesis")) {
            if (key.equals("setup")) {
                c.drawCircle(64, 64, 31, p);
                c.drawCircle(64, 64, 13, p);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    c.drawLine(64 + (float)Math.cos(a)*32, 64 + (float)Math.sin(a)*32,
                               64 + (float)Math.cos(a)*46, 64 + (float)Math.sin(a)*46, p);
                }
            } else {
                c.drawRoundRect(new RectF(33, 56, 95, 106), 10, 10, p);
                c.drawArc(new RectF(39, 20, 87, 79), 170, 250, false, p);
                c.drawCircle(64, 77, 7, p);
                c.drawLine(64, 84, 64, 93, p);
            }
        } else if (key.equals("hmgapi")) {
            c.drawRoundRect(new RectF(19, 47, 109, 100), 9, 9, p);
            c.drawRoundRect(new RectF(44, 29, 84, 49), 5, 5, p);
            c.drawLine(20, 65, 108, 65, p);
            c.drawLine(43, 78, 57, 90, p);
            c.drawLine(57, 90, 83, 72, p);
        } else if (key.equals("tiles") || key.equals("manual")) {
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++)
                c.drawRoundRect(new RectF(23+x*42, 23+y*42, 56+x*42, 56+y*42), 5, 5, p);
            if (key.equals("tiles")) {
                c.drawLine(70, 90, 80, 100, p);
                c.drawLine(80, 100, 108, 70, p);
            } else c.drawCircle(102, 101, 10, p);
        } else if (key.equals("comfort") || key.equals("warmer") || key.equals("rearclimate") || key.equals("memoryseat")) {
            c.drawLine(31, 48, 31, 92, p);
            c.drawLine(31, 92, 100, 92, p);
            c.drawLine(48, 76, 82, 76, p);
            c.drawLine(82, 76, 100, 32, p);
            if (!key.equals("memoryseat")) {
                for (int x = 45; x <= 85; x += 20) {
                    Path w = new Path();
                    w.moveTo(x, 56);
                    w.cubicTo(x-12, 43, x+12, 33, x, 19);
                    c.drawPath(w, p);
                }
            }
        } else if (key.equals("buttonmapper")) {
            c.drawRoundRect(new RectF(25, 22, 103, 48), 14, 14, p);
            c.drawRoundRect(new RectF(25, 79, 103, 105), 14, 14, p);
            c.drawLine(64, 51, 64, 77, p);
            c.drawLine(53, 66, 64, 77, p);
            c.drawLine(75, 66, 64, 77, p);
        } else if (key.equals("brakeindicator")) {
            c.drawCircle(64, 64, 31, p);
            c.drawArc(new RectF(16, 16, 112, 112), 190, 125, false, p);
            c.drawArc(new RectF(16, 16, 112, 112), 10, 125, false, p);
        } else if (key.equals("phone")) {
            c.drawArc(new RectF(20, 20, 108, 108), 55, 240, false, p);
            c.drawLine(31, 38, 49, 52, p);
            c.drawLine(82, 80, 96, 94, p);
        } else if (key.equals("radio") || key.equals("hddata")) {
            c.drawRoundRect(new RectF(18, 42, 110, 99), 9, 9, p);
            c.drawLine(30, 31, 95, 20, p);
            c.drawLine(32, 60, 69, 60, p);
            c.drawLine(32, 75, 61, 75, p);
            c.drawCircle(89, 74, 12, p);
        } else if (key.equals("media") || key.equals("voicememo")) {
            if (key.equals("media")) {
                c.drawLine(80, 24, 80, 86, p);
                c.drawLine(80, 24, 104, 32, p);
                c.drawOval(new RectF(32, 80, 79, 103), p);
            } else {
                c.drawRoundRect(new RectF(50, 20, 78, 76), 13, 13, p);
                c.drawArc(new RectF(36, 38, 92, 93), 0, 180, false, p);
                c.drawLine(64, 91, 64, 108, p);
            }
        } else if (key.equals("climate")) {
            c.drawCircle(64, 64, 12, p);
            for (int i=0; i<5; i++) {
                c.save(); c.rotate(i*72, 64, 64);
                c.drawOval(new RectF(52, 18, 76, 53), p);
                c.restore();
            }
        } else if (key.equals("map") || key.equals("navigation")) {
            Path arrow = new Path();
            arrow.moveTo(64, 17);
            arrow.lineTo(102, 106);
            arrow.lineTo(64, 83);
            arrow.lineTo(26, 106);
            arrow.close();
            c.drawPath(arrow, p);
            if (key.equals("navigation")) c.drawCircle(64, 65, 8, p);
        } else if (key.equals("projection")) {
            c.drawRoundRect(new RectF(20, 22, 74, 105), 7, 7, p);
            c.drawRoundRect(new RectF(70, 48, 111, 85), 7, 7, p);
        } else if (key.equals("valet")) {
            c.drawRoundRect(new RectF(20, 53, 108, 89), 12, 12, p);
            c.drawCircle(39, 92, 6, p);
            c.drawCircle(89, 92, 6, p);
            c.drawArc(new RectF(69, 15, 104, 51), 180, 300, false, p);
        } else if (key.equals("kiaconnect") || key.equals("ev")) {
            if (key.equals("ev")) {
                c.drawOval(new RectF(27, 23, 68, 78), p);
                c.drawOval(new RectF(63, 23, 104, 78), p);
                c.drawLine(64, 67, 64, 105, p);
            } else {
                c.drawRoundRect(new RectF(20, 47, 108, 90), 11, 11, p);
                c.drawCircle(38, 92, 6, p);
                c.drawCircle(90, 92, 6, p);
                c.drawArc(new RectF(42, 16, 88, 61), 210, 120, false, p);
            }
        } else if (key.equals("notifications")) {
            c.drawArc(new RectF(31, 21, 97, 95), 180, 180, false, p);
            c.drawLine(30, 85, 98, 85, p);
            c.drawCircle(64, 99, 5, p);
        } else if (key.equals("quietmode")) {
            c.drawCircle(64, 65, 31, p);
            c.drawLine(35, 95, 95, 35, p);
        }
        return b;
    }
}
