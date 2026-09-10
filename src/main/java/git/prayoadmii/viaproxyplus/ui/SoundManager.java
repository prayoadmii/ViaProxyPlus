/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package git.prayoadmii.viaproxyplus.ui;

import javazoom.jl.player.Player;

import java.io.InputStream;

public final class SoundManager {

    private static final String SOUND_PATH = "assets/viaproxy/sound/";

    private SoundManager() {
    }

    public static void playClick() {
        play("click.mp3");
    }

    public static void playDone() {
        play("done.mp3");
    }

    private static void play(final String sound) {
        final InputStream stream = SoundManager.class.getClassLoader().getResourceAsStream(SOUND_PATH + sound);
        if (stream == null) return;

        Thread playback = new Thread(() -> {
            try (InputStream input = stream) {
                new Player(input).play();
            } catch (Throwable ignored) {
            }
        }, "ViaProxyPlus-Sound");
        playback.setDaemon(true);
        playback.start();
    }

}