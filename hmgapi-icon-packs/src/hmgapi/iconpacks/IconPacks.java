package hmgapi.iconpacks;

import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import hmgapi.hook.ModEntry;

/**
 * Safe first milestone: observe launcher entries without modifying them.
 * The factory constructor is (menu, context, package, activity, icon, disabledIcon, title).
 */
public final class IconPacks extends ModEntry {
    @Override
    public void appLoaded(final ModContext mod, LoadedApp app) {
        if (!"com.android.launcher3".equals(app.packageName)) return;
        mod.log("Launcher detected; observing menu entry constructor");
        mod.hook(app.classLoader, "launcher.menu_entry.new", new MethodHook() {
            @Override
            protected void afterHookedMethod(Param p) {
                if (!mod.settingBoolean("verbose_logging", true)) return;
                try {
                    String pkg = String.valueOf(p.args[2]);
                    String activity = String.valueOf(p.args[3]);
                    int icon = ((Number) p.args[4]).intValue();
                    int disabled = ((Number) p.args[5]).intValue();
                    int title = ((Number) p.args[6]).intValue();
                    mod.log("entry package=" + pkg + " activity=" + activity
                            + " icon=0x" + Integer.toHexString(icon)
                            + " disabled=0x" + Integer.toHexString(disabled)
                            + " title=0x" + Integer.toHexString(title));
                } catch (Throwable e) {
                    mod.error("Unable to read launcher entry", e);
                }
            }
        });
    }
}
