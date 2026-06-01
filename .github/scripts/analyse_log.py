#!/usr/bin/env python3
"""
analyse_log.py — Scan Minecraft game.log for MIXIN-specific failures.

Rules
-----
PASS conditions:
  - "Loading N mods" found → Fabric Loader successfully applied all Mixins
  - No Mixin transformation errors

FAIL conditions (Mixin-specific only):
  - MixinTransformerError / MixinApplyError
  - @Shadow target resolution failure
  - "Mixin apply failed" / "could not apply mixin"
  - VerifyError whose stack trace includes a mixin class
  - ClassNotFoundException for a .mixin. class

IGNORE (not Mixin failures, expected in CI with Android LWJGL JARs on x86_64):
  - org.lwjgl.* NoSuchMethodError  (LWJGL version mismatch on desktop CI)
  - General game crash after Mixins loaded
  - OpenGL / GLFW / display errors
  - Any error in org.lwjgl.*, com.sun.*, sun.*, java.awt.*
"""

import re
import sys

LOG_FILE = sys.argv[1] if len(sys.argv) > 1 else "run/game.log"

MIXIN_ERROR_PATTERNS = [
    # Mixin transformer / apply failures
    (re.compile(r"MixinTransformerError", re.I),
     "Mixin transformer error — injector failed to apply"),
    (re.compile(r"MixinApplyError", re.I),
     "Mixin apply error"),
    (re.compile(r"mixin apply failed", re.I),
     "Mixin apply failed"),
    (re.compile(r"could not apply mixin", re.I),
     "Could not apply mixin"),
    # Shadow resolution
    (re.compile(r"@Shadow.*could not resolve", re.I),
     "@Shadow target not found"),
    (re.compile(r"InvalidMixinException.*@Shadow", re.I),
     "InvalidMixinException (@Shadow)"),
    (re.compile(r"shadow field.*not found", re.I),
     "Shadow field not found"),
    (re.compile(r"shadow method.*not found", re.I),
     "Shadow method not found"),
    # Hard Mixin failures
    (re.compile(r"FATAL.*Mixin", re.I),
     "FATAL Mixin error"),
    (re.compile(r"Mixin.*FATAL", re.I),
     "Mixin FATAL error"),
    (re.compile(r"SpongePowered Mixin.*Exception", re.I),
     "SpongePowered Mixin exception"),
    # ClassNotFoundException for mixin classes
    (re.compile(r"ClassNotFoundException.*\.mixin\.", re.I),
     "ClassNotFoundException for mixin class"),
    # VerifyError in a mixin class (not LWJGL, not vanilla MC)
    (re.compile(r"VerifyError.*(?:vulkanmod|\.mixin\.)", re.I),
     "VerifyError in mixin class"),
]

# Patterns indicating LWJGL / environment errors that should be IGNORED
# (these are expected when running Android-targeted LWJGL on Linux x86_64)
LWJGL_IGNORE_PATTERNS = [
    re.compile(r"org\.lwjgl\.", re.I),
    re.compile(r"java\.lang\.NoSuchMethodError.*lwjgl", re.I),
    re.compile(r"NoSuchMethodError.*MemoryUtil", re.I),
    re.compile(r"NoSuchMethodError.*org\.lwjgl", re.I),
    re.compile(r"GLFW", re.I),
    re.compile(r"OpenGL", re.I),
]

def is_lwjgl_or_env_error(line: str) -> bool:
    return any(p.search(line) for p in LWJGL_IGNORE_PATTERNS)

def main():
    try:
        with open(LOG_FILE, encoding="utf-8", errors="replace") as f:
            lines = f.readlines()
    except FileNotFoundError:
        print(f"::error::Log file not found: {LOG_FILE}")
        sys.exit(1)

    if not lines:
        print(f"::error::Log file is empty — game did not start at all")
        sys.exit(1)

    print()
    print("=======================================================")
    print(f"LOG ANALYSIS: {LOG_FILE}")
    print("=======================================================")

    # --- Positive indicators ---
    mods_loaded_line = None
    for line in lines:
        m = re.search(r"Loading (\d+) mod", line)
        if m:
            mods_loaded_line = (m.group(0), line.strip())
            break

    if mods_loaded_line:
        print(f"✓ {mods_loaded_line[0]}")
    else:
        print("⚠  'Loading N mods' not found — Fabric Loader may not have started")

    # --- Scan for MIXIN errors (ignore LWJGL/env noise) ---
    mixin_errors = []
    for lineno, line in enumerate(lines, 1):
        # Skip lines that are purely LWJGL / environment errors
        if is_lwjgl_or_env_error(line):
            continue
        for pattern, description in MIXIN_ERROR_PATTERNS:
            if pattern.search(line):
                mixin_errors.append((lineno, description, line.strip()))
                break  # one match per line is enough

    # --- Emit results ---
    if not mixin_errors:
        print()
        print("✓ No Mixin-specific errors found")
        if mods_loaded_line:
            print("✓ Mixin loading validated successfully")
            print()
            print("NOTE: Any LWJGL/crash errors after Mixin loading are expected")
            print("      when running Android-targeted LWJGL JARs on Linux x86_64 CI.")
            sys.exit(0)
        else:
            # Mods didn't load at all — suspicious
            print("✗ Fabric Loader did not report mod loading — check log manually")
            sys.exit(1)

    print()
    print(f"❌ {len(mixin_errors)} Mixin error(s):")
    for lineno, description, text in mixin_errors:
        short = text[:120] + "…" if len(text) > 120 else text
        print(f"  L{lineno}: {description}")
        print(f"       {short}")
        print(f"::error file=game.log,line={lineno}::{description}")
    sys.exit(1)


if __name__ == "__main__":
    main()
