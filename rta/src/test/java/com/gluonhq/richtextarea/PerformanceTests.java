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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author johan
 */
public class PerformanceTests {

    Stage stage;

    static final Logger LOG = Logger.getLogger(PerformanceTests.class.getName());
    
    public static void main(String[] args) throws InterruptedException {
        PerformanceTests pt = new PerformanceTests();
//        pt.insertOne();
        pt.insertMany();
    }

    AtomicInteger ait = new AtomicInteger(0);
    Runnable prelayout = () -> {
        int now = ait.incrementAndGet();
        LOG.info("start pulse "+now);
    };
     Runnable postlayout = () -> {
        LOG.info("done pulse "+ait.get());
    };
    public void insertMany() throws InterruptedException {

        final int WARMUP_CNT = 200; // how many chars to warmup
        final int TEST_CNT = 100; // how many chars to test
        final int SLEEP_MS = 2000; // sleep between warmup and test
        CountDownLatch cdl = new CountDownLatch(1);
        Platform.startup(() -> {
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
        System.err.println("Will test inserts of " + TEST_CNT + " after warmup of " + WARMUP_CNT + " chars, and sleep of " + SLEEP_MS + " ms.");

        long mem0 = getUsedMemory();
        long startTime = System.nanoTime();
        for (int round = 0; round < 1; round++) {
            for (int i = 0; i < WARMUP_CNT; i++) {
                char c = (char) ('a' + tlr.nextInt(26));
                if (tlr.nextInt(10) > 8) {
                    c = ' ';
                }
                String k = String.valueOf(c);
                KeyEvent evt = new KeyEvent(KeyEvent.KEY_TYPED, k, "", KeyCode.UNDEFINED, false, false, false, false);
                Platform.runLater(() -> skin.keyTypedListener(evt));
            }
            CountDownLatch cdl3 = new CountDownLatch(1);
            Platform.runLater(() -> cdl3.countDown());
            cdl3.await(1, TimeUnit.SECONDS);
            LOG.info("Waiting 2 seconds...");
            System.gc();
            Thread.sleep(1000);
            LOG.info("Waited 2 seconds");
        }
        long endTime = System.nanoTime();
        long dur = endTime - startTime;
        System.err.println("in Thread " + Thread.currentThread()+", warmup: total time = " + dur + ", average = " + (dur / (1e6 * WARMUP_CNT)) + ", now sleep for " + SLEEP_MS);
        Thread.sleep(SLEEP_MS);
        System.err.println("resume IN 2 seconds");
        Thread.sleep(2000);
        LOG.info("resume");
        Platform.runLater(() -> {
            rta.getScene().addPreLayoutPulseListener(prelayout);
            rta.getScene().addPostLayoutPulseListener(postlayout);
        });
        startTime = System.nanoTime();
        long[] extime = new long[TEST_CNT];
        final int cnt = TEST_CNT;
        for (int i = 0; i < cnt; i++) {
            char c = (char) ('a' + tlr.nextInt(26));
            if (tlr.nextInt(10) > 8) {
                c = ' ';
            }
            String k = String.valueOf(c);
            final KeyEvent evt = new KeyEvent(KeyEvent.KEY_TYPED, k, "", KeyCode.UNDEFINED, false, false, false, false);
            LOG.info("Schedule typing "+k+" with event "+evt);
            final int idx = i;
            CountDownLatch oneRun = new CountDownLatch(1);
            Platform.runLater(() -> {
                LOG.info("in PR, start " + evt+", idx = "+idx);
                long t0 = System.nanoTime();
                skin.keyTypedListener(evt);
                long t1 = System.nanoTime();
                extime[idx] =t1-t0;
                LOG.info("in PR, done " + evt);
                Platform.requestNextPulse();
                oneRun.countDown();
            });
            boolean await = oneRun.await(1, TimeUnit.SECONDS);
            if (!await) {
                System.err.println("STUCK! exit now");
                System.exit(1);
            }
//            Thread.sleep(1);
        }
        LOG.info("during test, ait = "+ait);
        CountDownLatch cdl4 = new CountDownLatch(1);
        Platform.runLater(() -> cdl4.countDown());
        cdl4.await(10, TimeUnit.SECONDS);
        LOG.info("after test, ait = "+ait);
        endTime = System.nanoTime();
        dur = endTime - startTime;
//        long mem1 = getUsedMemory();
long mem1 =0;
        System.err.println("total time = " + dur + ", average = " + (dur / (1e6 * cnt)) + " and used mem = " + (mem1 - mem0));
        long tottime = 0;
        for (int i = 0; i < TEST_CNT; i++) {
            tottime = tottime + extime[i];
        }
        System.err.println("And in keytrigger, average = "+ (tottime/(1e6*cnt))+" and ait = "+ait);
//        Thread.sleep(5 * SLEEP_MS);
        Platform.exit();
    }

    void prelayoutlistener() {
        
    }
    public void insertOne() throws InterruptedException {
        final int SLEEP_MS = 2000; // sleep between warmup and test
        CountDownLatch cdl = new CountDownLatch(1);
        Platform.startup(() -> {
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

        Thread.sleep(SLEEP_MS);

        long mem0 = getUsedMemory();
        long startTime = System.nanoTime();
        for (int i = 0; i < 1; i++) {
            char c = (char) ('a' + tlr.nextInt(26));
            if (tlr.nextInt(10) > 8) {
                c = ' ';
            }
            String k = String.valueOf(c);
            KeyEvent evt = new KeyEvent(KeyEvent.KEY_TYPED, k, "", KeyCode.UNDEFINED, false, false, false, false);
            Platform.runLater(() -> skin.keyTypedListener(evt));
        }
        Thread.sleep(SLEEP_MS/10);
        Platform.exit();
    }
public void insertTwo() throws InterruptedException {
        final int SLEEP_MS = 2000; // sleep between warmup and test
        CountDownLatch cdl = new CountDownLatch(1);
        Platform.startup(() -> {
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

        Thread.sleep(SLEEP_MS);

        long mem0 = getUsedMemory();
        long startTime = System.nanoTime();
        for (int i = 0; i < 1; i++) {
            char c = (char) ('a' + tlr.nextInt(26));
            if (tlr.nextInt(10) > 8) {
                c = ' ';
            }
            String k = String.valueOf(c);
            KeyEvent evt = new KeyEvent(KeyEvent.KEY_TYPED, k, "", KeyCode.UNDEFINED, false, false, false, false);
            Platform.runLater(() -> skin.keyTypedListener(evt));
        }
        Thread.sleep(SLEEP_MS/10);
        Platform.exit();
    }

    long getUsedMemory() {
        System.gc();
        try {
            Thread.sleep(100);
            System.gc();
            Thread.sleep(1400);
        } catch (InterruptedException ex) {
            System.getLogger(PerformanceTests.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        Runtime runtime = Runtime.getRuntime();
        long val = runtime.totalMemory() - runtime.freeMemory();
        return val;
    }

}
