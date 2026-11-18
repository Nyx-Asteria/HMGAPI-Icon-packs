package hmgapi.iconpacks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import hmgapi.hook.ModEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * RC7: PNG-first icon override through HMG-API's existing in-memory launcher
 * entry image fields. No synthetic resource IDs or persistent APK changes.
 * Internal field access is experimental and version-dependent.
 */
public final class IconPacks extends ModEntry {
    private final byte[][] pictures = new byte[4][];
    private boolean hooked;
    private int pngPatched;
    private final boolean[] patched = new boolean[4];
    private int canvasFallback;
    private int drawableSeen;

    private static String packageOf(int kind) {
        switch (kind) {
            case 0: return "hmgapi.app";
            case 1: return "dev.lemoyne.tiles";
            case 2: return "com.nemesis.manager";
            case 3: return "dev.lemoyne.comfort";
            default: return null;
        }
    }

    private static int kindOf(String pkg) {
        for (int i = 0; i < 4; i++) if (packageOf(i).equals(pkg)) return i;
        return -1;
    }

    @Override public void zygoteInit(ModContext mod) {
        String[] names = {"hmgapi", "tiles", "nemesis", "comfort"};
        for (int i = 0; i < names.length; i++) {
            File f = new File(mod.dir, "icons/" + names[i] + ".png");
            try {
                FileInputStream in = new FileInputStream(f);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                } finally { in.close(); }
                byte[] bytes = out.toByteArray();
                if (bytes.length < 8 || bytes[0] != (byte)137 || bytes[1] != 80
                        || bytes[2] != 78 || bytes[3] != 71)
                    throw new IllegalStateException("Not a PNG");
                pictures[i] = bytes;
                mod.log("PNG loaded " + names[i] + " bytes=" + bytes.length);
            } catch (Throwable t) {
                mod.error("PNG preload failed for " + names[i], t);
            }
        }
    }

    @Override public void appLoaded(final ModContext mod, LoadedApp app) {
        if (!"com.android.launcher3".equals(app.packageName) || hooked) return;
        hooked = true;
        mod.log("RC7 PNG-first launcher experiment started");
        if (mod.settingBoolean("enable_overrides", true)) {
            try {
                // These entries are the source HMG-API's own drawable resolver uses.
                // Their icon bytes are loaded by HMG-API before the launcher is bound.
                Class<?> mods = Class.forName("hmgapi.hook.Mods");
                Method get = mods.getDeclaredMethod("launcherEntries");
                get.setAccessible(true);
                List<?> list = (List<?>) get.invoke(null);
                for (Object entry : list) {
                    Class<?> cls = entry.getClass();
                    Field pkgField = cls.getDeclaredField("packageName");
                    pkgField.setAccessible(true);
                    String pkg = (String) pkgField.get(entry);
                    int kind = kindOf(pkg);
                    if (kind < 0) continue;
                    byte[] png = pictures[kind];
                    if (png == null) {
                        mod.log("PNG missing for " + pkg + "; Canvas fallback remains enabled");
                        continue;
                    }
                    for (String fieldName : new String[]{"iconDay", "iconNight", "iconDisabledDay", "iconDisabledNight"}) {
                        Field f = cls.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(entry, png);
                    }
                    pngPatched++;
                    patched[kind] = true;
                    mod.log("PNG PRIMARY patched " + pkg + " bytes=" + png.length);
                }
                mod.log("PNG PRIMARY summary patched=" + pngPatched + "/4");
            } catch (Throwable t) {
                mod.error("PNG PRIMARY HMG-API entry patch failed", t);
            }
        }

        // Fallback: keep the ORIGINAL HMG-API synthetic IDs. No constructor mutation.
        // This path may be bypassed by HMG-API's earlier resource hook.
        mod.hook(null, "framework.resources.get_drawable", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                if (!mod.settingBoolean("enable_overrides", true) || p.hasThrowable()) return;
                try {
                    int id = ((Number) p.args[0]).intValue();
                    int kind = -1;
                    if (id == 0x7f7e0001 || id == 0x7f7e0002) kind = 0;
                    if (id == 0x7f7e0007 || id == 0x7f7e0008) kind = 1;
                    if (id == 0x7f7e000a || id == 0x7f7e000b) kind = 2;
                    if (id == 0x7f7e0004 || id == 0x7f7e0005) kind = 3;
                    if (kind < 0) return;
                    drawableSeen++;
                    if (patched[kind]) {
                        if (drawableSeen <= 12) mod.log("PNG resolver observed " + packageOf(kind));
                        return;
                    }
                    Resources res = (Resources) p.thisObject;
                    p.setResult(new BitmapDrawable(res, render(kind)));
                    canvasFallback++;
                    if (canvasFallback <= 12) mod.log("CANVAS FALLBACK served " + packageOf(kind));
                } catch (Throwable t) { mod.error("Canvas fallback", t); }
            }
        });
        mod.hook(app.classLoader, "launcher.menu_list.load", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                mod.log("RC7 menu event PNG patched=" + pngPatched
                    + " drawable observed=" + drawableSeen + " Canvas fallback=" + canvasFallback);
            }
        });
        mod.log("RC7 hooks installed; PNG patches=" + pngPatched);
    }

    private static Bitmap render(int kind) {
        final int n = 128;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.scale(n / 100f, n / 100f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(7f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setShader(new LinearGradient(12, 12, 88, 88,
                new int[]{Color.rgb(55,237,255), Color.rgb(123,110,255), Color.rgb(255,88,220)},
                null, Shader.TileMode.CLAMP));
        if (kind == 3) { // Comfort: heated seat and three heat waves
            Path seat = new Path();
            seat.moveTo(25,24); seat.lineTo(25,58);
            seat.quadTo(25,66,34,66);
            seat.lineTo(64,66);
            seat.quadTo(73,66,73,74);
            seat.lineTo(73,86);
            c.drawPath(seat,p);
            c.drawLine(30,85,76,85,p);
            for (int i = 0; i < 3; i++) {
                float x = 39 + i * 13;
                Path wave = new Path();
                wave.moveTo(x,14);
                wave.cubicTo(x-9,22,x+9,27,x,36);
                wave.cubicTo(x-9,44,x+9,49,x,55);
                c.drawPath(wave,p);
            }
        } else if (kind == 1) { // Tile Manager: three rounded boxes, checkmark; NO outer border
            box(c,p,13,13,43,43,6);
            box(c,p,57,13,87,43,6);
            box(c,p,13,57,43,87,6);
            Path check = new Path();
            check.moveTo(57,73); check.lineTo(68,83); check.lineTo(88,57);
            c.drawPath(check,p);
        } else if (kind == 2) { // Nemesis: settings cog
            Path cog = new Path();
            final int teeth = 8;
            for (int i=0; i<teeth*4; i++) {
                double angle = -Math.PI/2 + 2*Math.PI*i/(teeth*4);
                float radius = (i%4 == 0 || i%4 == 3) ? 43 : 34;
                float x = 50 + (float)Math.cos(angle)*radius;
                float y = 50 + (float)Math.sin(angle)*radius;
                if (i==0) cog.moveTo(x,y); else cog.lineTo(x,y);
            }
            cog.close();
            c.drawPath(cog,p);
            c.drawCircle(50,50,16,p);
        } else { // HMG-API: rounded dashboard with three lines and status dot
            box(c,p,13,21,87,84,12);
            c.drawLine(29,40,67,40,p);
            c.drawLine(29,53,67,53,p);
            c.drawLine(29,66,60,66,p);
            c.drawCircle(81,21,10,p);
        }
        p.setShader(null);
        return b;
    }

    private static void box(Canvas c, Paint p, float l, float t, float r, float b, float radius) {
        c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);
    }
}
