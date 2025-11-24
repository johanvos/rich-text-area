/*
 * Copyright (C) 2025 Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gluonhq.richtextarea;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

/**
 *
 * @author johan
 */
public class PerformanceTests {

    static Logger LOG = Logger.getLogger(PerformanceTests.class.getName());
    Stage stage;
    @Test
    public void insertMany() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        Platform.startup(() ->{
            this.stage = new Stage();
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
        RichTextArea rta = new RichTextArea();
        CountDownLatch cdl2 = new CountDownLatch(1);
        Platform.runLater(() -> {
                Scene scene = new Scene(new StackPane(rta));
                stage.setScene(scene);
                stage.show();
                cdl2.countDown();
        });
        cdl2.await(1, TimeUnit.SECONDS);
        RichTextAreaSkin skin = (RichTextAreaSkin) rta.getSkin();
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        long startTime = System.nanoTime();
        final int WARMUP_CNT = 100;
        for (int i = 0; i < WARMUP_CNT; i++) {
            char c = (char) ('a' + tlr.nextInt(26));
            if (tlr.nextInt(10) > 8) c = ' ';
            String k = String.valueOf(c);
            KeyEvent evt = new KeyEvent(KeyEvent.KEY_TYPED, k, "", KeyCode.UNDEFINED, false, false, false, false);
            Platform.runLater(() -> skin.keyTypedListener(evt));
        }
        CountDownLatch cdl3 = new CountDownLatch(1);
        Platform.runLater(() -> cdl3.countDown());
        cdl3.await(1, TimeUnit.SECONDS);
                long endTime = System.nanoTime();
        long dur = endTime - startTime;
        System.err.println("warmup: total time = " + dur + ", average = " + (dur / WARMUP_CNT));
Thread.sleep(3000);
        System.err.println("resume");
        startTime = System.nanoTime();
        final int cnt = 2000;
        for (int i = 0; i < cnt; i++) {
            char c = (char) ('a' + tlr.nextInt(26));
            if (tlr.nextInt(10) > 8) c = ' ';
            String k = String.valueOf(c);
            KeyEvent evt = new KeyEvent(KeyEvent.KEY_TYPED, k, "", KeyCode.UNDEFINED, false, false, false, false);
            Platform.runLater(() -> skin.keyTypedListener(evt));
        }
        CountDownLatch cdl4 = new CountDownLatch(1);
        Platform.runLater(() -> cdl4.countDown());
        cdl4.await(10, TimeUnit.SECONDS);
        endTime = System.nanoTime();
        dur = endTime - startTime;
        System.err.println("total time = "+dur+", average = "+ (dur/(1e6*cnt)));
    }
}
