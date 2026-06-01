#!/usr/bin/env python3
"""
analyse_log.py — Scan Minecraft/Mixin log and fail CI on critical errors.
Usage: python3 .github/scripts/analyse_log.py <log_file>
Exit: 0 = clean, 1 = fatal errors found
"""
import re, sys
from pathlib import Path

FATAL = [
    (re.compile(r"@Shadow field .+? in .+? could not find"),        "Shadow field not found"),
    (re.compile(r"@Shadow method .+? in .+? could not find"),       "Shadow method not found"),
    (re.compile(r"Critical mixin application failure", re.I),       "Critical mixin failure"),
    (re.compile(r"Mixin apply failed"),                             "Mixin inject failed"),
    (re.compile(r"Unable to read mixin refmap"),                    "Refmap missing from JAR"),
    (re.compile(r"refmap .+? not found"),                           "Refmap file missing"),
    (re.compile(r"\[FATAL\].*[Mm]ixin"),                            "FATAL in Mixin subsystem"),
    (re.compile(r"MixinTransformerError", re.I),                    "Mixin transformer error"),
    (re.compile(r"InvalidMixinException", re.I),                    "Invalid mixin"),
    (re.compile(r"java\.lang\.VerifyError"),                        "Bytecode VerifyError"),
    (re.compile(r"NoSuchFieldError"),                               "NoSuchFieldError at runtime"),
    (re.compile(r"NoSuchMethodError"),                              "NoSuchMethodError at runtime"),
    (re.compile(r"Game crashed!"),                                  "Game crashed"),
]
IGNORE = [
    re.compile(r"vulkanmod\.test\.skip_vulkan"),
    re.compile(r"\[TEST\].*Vulkan init skipped"),
    re.compile(r"Could not get a display"),
    re.compile(r"LWJGL.*failed to load"),
    re.compile(r"Opening GL display"),
]
OK_SIGNS = [
    (re.compile(r"Loading \d+ mods"),               lambda m: f"✓ {m.group(0)}"),
    (re.compile(r"Loading Vulkanmod"),               lambda _: "✓ VulkanMod loaded"),
    (re.compile(r"vulkanmod\.test\.skip_vulkan"),    lambda _: "✓ skip_vulkan active"),
    (re.compile(r"Vulkan init skipped"),             lambda _: "✓ headless mode active"),
]

def analyse(path):
    if not path.exists():
        print(f"[analyse_log] WARNING: {path} not found")
        return []
    failures = []
    for no, line in enumerate(path.read_text(errors="replace").splitlines(), 1):
        if any(p.search(line) for p in IGNORE):
            continue
        for pat, desc in FATAL:
            if pat.search(line):
                failures.append((no, line.rstrip(), desc))
                break
    return failures

def main():
    if len(sys.argv) < 2:
        print("Usage: analyse_log.py <log>"); return 1
    path = Path(sys.argv[1])
    lines = path.read_text(errors="replace") if path.exists() else ""
    print(f"\n{'='*55}\nLOG ANALYSIS: {path.name}\n{'='*55}")
    for pat, fmt in OK_SIGNS:
        m = pat.search(lines)
        if m:
            print(fmt(m))
    failures = analyse(path)
    if not failures:
        print("\n✅ No Mixin or crash errors detected"); return 0
    print(f"\n❌ {len(failures)} critical error(s):\n")
    for no, line, desc in failures:
        print(f"  L{no}: {desc}")
        print(f"       {line[:180]}")
        print(f"::error file={path.name},line={no}::{desc}")
    return 1

if __name__ == "__main__":
    sys.exit(main())
