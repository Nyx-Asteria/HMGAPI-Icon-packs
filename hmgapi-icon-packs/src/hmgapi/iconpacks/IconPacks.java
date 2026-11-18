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
import java.util.concurrent.ConcurrentHashMap;

/**
 * RC3: runtime icon replacement with focused drawable diagnostics.
 * Art is drawn as native gradients (no image file I/O from the launcher UID).
 */
public final class IconPacks extends ModEntry {
    private final ConcurrentHashMap<Integer, String> entries =
            new ConcurrentHashMap<Integer, String>();
    private boolean hooked;
    private int beforeCount;
    private int afterCount;

    @Override public void appLoaded(final ModContext mod, LoadedApp app) {
        if (!"com.android.launcher3".equals(app.packageName) || hooked) return;
        hooked = true;
        mod.log("RC3 launcher detected");
        mod.hook(app.classLoader, "launcher.menu_entry.new", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                try {
                    String pkg = String.valueOf(p.args[2]);
                    int icon = ((Number) p.args[4]).intValue();
                    int disabled = ((Number) p.args[5]).intValue();
                    entries.put(icon, pkg);
                    entries.put(disabled, pkg);
                    // Keep the ID-to-package mapping for replacement; no repeated discovery logs.
                } catch (Throwable t) { mod.error("entry mapping", t); }
            }
        });
        // After the core's own getDrawable hook: core resolves synthetic IDs first.
        // This replace-mode after callback substitutes the drawable without touching APKs.
        mod.hook(null, "framework.resources.get_drawable", new MethodHook() {
            @Override protected void beforeHookedMethod(Param p) {
                try {
                    int id = ((Number) p.args[0]).intValue();
                    String pkg = entries.get(id);
                    if (beforeCount++ < 12 || (pkg != null && beforeCount <= 40))
                        mod.log("DIAG BEFORE id=0x" + Integer.toHexString(id)
                            + " package=" + pkg);
                } catch (Throwable t) { mod.error("DIAG before", t); }
            }
            @Override protected void afterHookedMethod(Param p) {
                try {
                    int id = ((Number) p.args[0]).intValue();
                    String pkg = entries.get(id);
                    if (afterCount++ < 12 || (pkg != null && afterCount <= 40))
                        mod.log("DIAG AFTER id=0x" + Integer.toHexString(id)
                            + " package=" + pkg + " result="
                            + (p.getResult() == null ? "null" : p.getResult().getClass().getName())
                            + " throwable=" + p.hasThrowable());
                } catch (Throwable t) { mod.error("DIAG after", t); }
                if (!mod.settingBoolean("enable_overrides", true) || p.hasThrowable()) return;
                try {
                    int id = ((Number) p.args[0]).intValue();
                    String pkg = entries.get(id);
                    if (pkg == null) return;
                    int kind = "hmgapi.app".equals(pkg) ? 0 :
                            "dev.lemoyne.tiles".equals(pkg) ? 1 :
                            "com.nemesis.manager".equals(pkg) ? 2 :
                            "dev.lemoyne.comfort".equals(pkg) ? 3 : -1;
                    if (kind < 0) return;
                    Bitmap image = render(kind);
                    Resources res = (Resources) p.thisObject;
                    p.setResult(new BitmapDrawable(res, image));
                    if (mod.settingBoolean("verbose_logging", true))
                        mod.log("REPLACED " + pkg + " id=0x" + Integer.toHexString(id));
                } catch (Throwable t) { mod.error("icon replacement", t); }
            }
        });
        mod.log("RC3 drawable diagnostic hook registered");
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
