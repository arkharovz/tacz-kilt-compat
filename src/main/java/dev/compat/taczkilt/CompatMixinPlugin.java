package dev.compat.taczkilt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.Config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1) Sets "injectors.defaultRequire" to 0 for the mixin configs of Forge mods that were written against
 *    Forge-patched Minecraft (targets that do not exist on Fabric), so an unresolvable injector is skipped
 *    instead of crashing the game ("Critical injection failure ... Scanned 0 target(s)").
 * 2) Applies our replacement mixin only if TACZ is actually present.
 *
 * Extra configs can be added without rebuilding: -Dtaczkilt.relax=other.mixins.json,another.mixins.json
 * Uses Mixin internals via reflection (MixinConfig.injectorOptions.defaultRequire, Mixin 0.8.x).
 */
public class CompatMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LogManager.getLogger("TACZ-Kilt-Compat");
    private static final String TACZ_CONFIG = "tacz.mixins.json";

    private final Set<String> relaxedConfigs = new HashSet<>();

    private static boolean shouldRelax(String name) {
        if (name == null) return false;
        if (name.equals("simplebedrockmodel.mixins.json")) return true;
        if (name.startsWith("tacz.") && name.endsWith(".mixins.json")) return true; // tacz.mixins.json, tacz.oculus..., tacz.compat...
        String extra = System.getProperty("taczkilt.relax", "");
        for (String s : extra.split(",")) {
            if (!s.isBlank() && s.trim().equals(name)) return true;
        }
        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
        relax();
    }

    /** @return true if tacz.mixins.json is registered. Idempotent. */
    private boolean relax() {
        boolean taczFound = false;
        try {
            for (Config cfg : Mixins.getConfigs()) {
                String name = cfg.getName();
                if (TACZ_CONFIG.equals(name)) taczFound = true;
                if (!shouldRelax(name) || relaxedConfigs.contains(name)) continue;
                try {
                    Object mixinConfig = cfg.getConfig();
                    Field optsField = mixinConfig.getClass().getDeclaredField("injectorOptions");
                    optsField.setAccessible(true);
                    Object opts = optsField.get(mixinConfig);
                    boolean done = false;
                    for (Field f : opts.getClass().getDeclaredFields()) {
                        if (f.getType() != int.class) continue;
                        boolean isDefaultRequire = f.getName().toLowerCase().contains("defaultrequire");
                        if (!isDefaultRequire) {
                            for (var a : f.getAnnotations()) {
                                if (String.valueOf(a).contains("defaultRequire")) { isDefaultRequire = true; break; }
                            }
                        }
                        if (isDefaultRequire) {
                            f.setAccessible(true);
                            int old = f.getInt(opts);
                            f.setInt(opts, 0);
                            done = true;
                            LOG.info("Relaxed {} injectors.defaultRequire {} -> 0 (field '{}')", name, old, f.getName());
                        }
                    }
                    if (done) relaxedConfigs.add(name);
                    else LOG.warn("Found {} but no defaultRequire field; Mixin internals differ from 0.8.x", name);
                } catch (Throwable t) {
                    LOG.error("Could not relax {} - the game may still crash at startup", name, t);
                }
            }
        } catch (Throwable t) {
            LOG.error("Could not enumerate mixin configs", t);
        }
        return taczFound;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Called before any class is transformed with this config; re-try in case a config
        // was registered by Kilt after our onLoad.
        return relax();
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
