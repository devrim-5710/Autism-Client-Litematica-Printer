package com.litematicaprinteraddon.addon;

import net.minecraft.world.level.BlockGetter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class LitematicaBridge {
    private static boolean probed;
    private static Method getter;
    private static Object handler;

    private LitematicaBridge() {}

    public static BlockGetter schematicWorld() {
        if (!probed) probe();
        try {
            Object world = Modifier.isStatic(getter.getModifiers())
                ? getter.invoke(null)
                : getter.invoke(handler);
            return world instanceof BlockGetter bg ? bg : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void probe() {
        probed = true;
        String[] candidates = {
            "fi.dy.masa.litematica.world.SchematicWorldHandler",
            "fi.dy.masa.litematica.SchematicWorldHandler",
            "fi.dy.masa.litematica.schematic.SchematicWorldHandler"
        };
        for (String name : candidates) {
            Class<?> cls;
            try {
                cls = Class.forName(name);
            } catch (ClassNotFoundException ignored) {
                continue;
            }
            for (Method m : cls.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 0
                    && m.getReturnType().getSimpleName().equals("WorldSchematic")) {
                    getter = m;
                    handler = null;
                    return;
                }
            }
            Object inst = null;
            try {
                inst = cls.getDeclaredField("INSTANCE").get(null);
            } catch (ReflectiveOperationException ignored) {
            }
            if (inst != null) {
                for (Method m : cls.getMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0) continue;
                    if (m.getName().equals("getWorld") || m.getName().equalsIgnoreCase("getSchematicWorld")) {
                        getter = m;
                        handler = inst;
                        return;
                    }
                }
            }
        }
    }
}
