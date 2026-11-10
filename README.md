# TACZ x Kilt compat (Fabric 1.20.1)

Fixes startup crash of `tacz-forge-1.20.1-1.2.0.jar` under Kilt:

    InjectionError: Callback method tacz$drawShaderScopeLenses(...) in tacz.mixins.json:client.GameRendererMixin
    failed injection check, (0/1) succeeded. Scanned 0 target(s).

## What it does
1. `CompatMixinPlugin` sets `injectors.defaultRequire` to 0 (reflection) for `tacz*.mixins.json` and
   `simplebedrockmodel.mixins.json`, so injectors that cannot be resolved on Fabric are skipped instead of crashing.
   More configs: `-Dtaczkilt.relax=name.mixins.json,...`.
2. `GameRendererScopeMixin` re-adds the two skipped TACZ injectors into `GameRenderer#renderLevel`
   (HEAD -> `ScopeLensRenderer.beginFrame()`, `popPush("hand")` -> `ScopeLensRenderer.render()`),
   resolved through this mod's own refmap.

##
Made with AI assistance.

License: GPL-3.0.
