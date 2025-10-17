package com.setycz.chickens;

/**
 * Enumerates the biomes in which a chicken can spawn. Direct port from
 * the classic mod, retained for compatibility with the registry layout.
 */
public enum SpawnType {
    NORMAL,
    SNOW,
    NONE,
    HELL;

    public static String[] names() {
        SpawnType[] states = values();
        String[] names = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            names[i] = states[i].name();
        }
        return names;
    }
}
