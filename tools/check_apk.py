#!/usr/bin/env python3
"""Fails if R8 removed a constructor that a library calls by name at runtime (#7).

R8 can't see constructors that are only called through reflection; it relies on keep rules, and some libraries'
rules stopped keeping them under AGP 9. Such a build compiles and passes unit tests, then crashes on the phone.
This reads the APK's dex files and checks the classes that libraries create by name:

- Room's generated databases (`*Database_Impl`): a no-argument constructor (Room calls `newInstance()`).
- WorkManager's input mergers: a no-argument constructor.
- WorkManager workers: a (Context, WorkerParameters) constructor.
- androidx.startup initializers and Glance action callbacks: a no-argument constructor.

Usage: check_apk.py app.apk
"""
import struct
import sys
import zipfile

ABSTRACT = 0x400
INTERFACE = 0x200
CONTEXT = "Landroid/content/Context;"


def uleb(buf, off):
    value = shift = 0
    while True:
        byte = buf[off]
        off += 1
        value |= (byte & 0x7F) << shift
        if byte < 0x80:
            return value, off
        shift += 7


def read_dex(dex, classes):
    string_ids_size, string_ids_off, type_ids_size, type_ids_off = struct.unpack_from("<4I", dex, 0x38)
    proto_ids_size, proto_ids_off = struct.unpack_from("<2I", dex, 0x48)
    method_ids_size, method_ids_off, class_defs_size, class_defs_off = struct.unpack_from("<4I", dex, 0x58)
    strings = {}

    def string(i):
        if i not in strings:
            off = struct.unpack_from("<I", dex, string_ids_off + 4 * i)[0]
            _, off = uleb(dex, off)
            strings[i] = dex[off:dex.index(0, off)].decode("utf-8", "replace")
        return strings[i]

    def type_name(i):
        return None if i == 0xFFFFFFFF else string(struct.unpack_from("<I", dex, type_ids_off + 4 * i)[0])

    def type_list(off):
        if off == 0:
            return []
        size = struct.unpack_from("<I", dex, off)[0]
        return [type_name(struct.unpack_from("<H", dex, off + 4 + 2 * k)[0]) for k in range(size)]

    def method(i):
        _, proto, name = struct.unpack_from("<HHI", dex, method_ids_off + 8 * i)
        params_off = struct.unpack_from("<III", dex, proto_ids_off + 12 * proto)[2]
        return string(name), tuple(type_list(params_off))

    for c in range(class_defs_size):
        cls, flags, sup, ifaces_off, _, _, data_off, _ = struct.unpack_from("<8I", dex, class_defs_off + 32 * c)
        constructors = []
        if data_off:
            off = data_off
            static_fields, off = uleb(dex, off)
            instance_fields, off = uleb(dex, off)
            direct_methods, off = uleb(dex, off)
            _, off = uleb(dex, off)
            for _ in range(static_fields + instance_fields):
                _, off = uleb(dex, off)
                _, off = uleb(dex, off)
            index = 0
            for _ in range(direct_methods):
                delta, off = uleb(dex, off)
                _, off = uleb(dex, off)
                _, off = uleb(dex, off)
                index += delta
                name, params = method(index)
                if name == "<init>":
                    constructors.append(params)
        classes[type_name(cls)] = (flags, type_name(sup), type_list(ifaces_off), constructors)


def supertypes(classes, name):
    seen = []
    stack = [name]
    while stack:
        current = stack.pop()
        if current not in classes:
            continue
        _, sup, ifaces, _ = classes[current]
        for parent in [sup] + ifaces:
            if parent and parent not in seen:
                seen.append(parent)
                stack.append(parent)
    return seen


def readable(descriptor):
    return descriptor[1:-1].replace("/", ".")


def main(path):
    classes = {}
    with zipfile.ZipFile(path) as apk:
        for entry in sorted(apk.namelist()):
            if entry.endswith(".dex"):
                read_dex(apk.read(entry), classes)

    rules = [
        ("Room database", lambda n, s: n.endswith("Database_Impl;"), [()]),
        ("WorkManager input merger", lambda n, s: "Landroidx/work/InputMerger;" in s, [()]),
        ("WorkManager worker", lambda n, s: "Landroidx/work/ListenableWorker;" in s, [(CONTEXT, "Landroidx/work/WorkerParameters;")]),
        ("start-up initializer", lambda n, s: "Landroidx/startup/Initializer;" in s, [()]),
        ("Glance action callback", lambda n, s: "Landroidx/glance/appwidget/action/ActionCallback;" in s, [()]),
    ]
    checked = 0
    problems = []
    for name, (flags, _, _, constructors) in sorted(classes.items()):
        if flags & (ABSTRACT | INTERFACE):
            continue
        parents = None
        for label, matches, needed in rules:
            if parents is None:
                parents = supertypes(classes, name)
            if not matches(name, parents):
                continue
            checked += 1
            if not any(params in constructors for params in needed):
                want = ", ".join(readable(p) for p in needed[0]) or "no arguments"
                problems.append(f"{label} {readable(name)} has no constructor ({want}); R8 removed it")
            break

    print(f"Checked {checked} classes that libraries create by name, in {len(classes)} classes.")
    for problem in problems:
        print(f"::error::{problem}")
    return 1 if problems or checked == 0 else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
