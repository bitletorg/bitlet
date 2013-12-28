/*
 *              bitlet - Simple bittorrent library
 *  Copyright (C) 2008 Alessandro Bahgat Shehata, Daniele Castagna
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.bitlet.wetorrent.util.thread;

import java.util.LinkedList;
import java.util.Queue;

public class InterruptableTasksThread extends Thread {

    Queue<ThreadTask> tasks = new LinkedList<ThreadTask>();
    ThreadTask currentTask = null;
    private boolean closing = false;

    /**
     * Creates a new instance of InterruptableTasksThread
     */
    public InterruptableTasksThread() {
    }

    public InterruptableTasksThread(String name) {
        super(name);
    }

    public synchronized void addTask(ThreadTask task) {
        tasks.add(task);
        if (tasks.size() == 1) {
            notify();
        }
    }

    public void run() {
        ThreadTask currentTask = nextTask();
        while (!isClosing() && currentTask != null) {
            try {
                if (!currentTask.execute()) {
                    currentTask = nextTask();
                }
            } catch (Exception e) {
                closing = true;
                currentTask.exceptionCought(e);
            }
        }
    }

    public synchronized ThreadTask nextTask() {
        currentTask = null;
        while (!isClosing() && (currentTask = tasks.poll()) == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                closing = true;
            }
        }
        return currentTask;
    }

    public synchronized ThreadTask getCurrentTask() {
        return currentTask;
    }

    public synchronized void interrupt() {
        if (currentTask != null) {
            currentTask.interrupt();
        }
        closing = true;
        notify();
    }

    public synchronized boolean isClosing() {
        return closing;
    }
}
