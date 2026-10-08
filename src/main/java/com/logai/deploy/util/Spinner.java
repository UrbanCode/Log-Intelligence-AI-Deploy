package com.logai.deploy.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class Spinner implements AutoCloseable {

    private static final String[] FRAMES = {
            "|",
            "/",
            "-",
            "\\"
    };

    private final AtomicBoolean running =
            new AtomicBoolean(false);

    private final String message;

    private Thread worker;

    public Spinner(String message) {
        this.message = message;
        start();
    }

    private void start() {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        worker =
                new Thread(
                        () -> {

                            int i = 0;

                            while (running.get()) {

                                System.out.print(
                                        "\r"
                                                + message
                                                + " "
                                                + FRAMES[
                                                i % FRAMES.length
                                                ]
                                );

                                i++;

                                try {
                                    Thread.sleep(100);

                                } catch (InterruptedException ex) {

                                    Thread.currentThread()
                                            .interrupt();

                                    break;
                                }
                            }
                        },
                        "log-intelligence-spinner"
                );

        worker.setDaemon(true);
        worker.start();
    }

    public void stop(String finalMessage) {

        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (worker != null) {

            worker.interrupt();

            try {
                worker.join(200);

            } catch (InterruptedException ex) {

                Thread.currentThread()
                        .interrupt();
            }
        }

        System.out.print(
                "\r"
                        + finalMessage
                        + "\n"
        );
    }

    @Override
    public void close() {
        stop("");
    }
}