/*
 *              bitlet - Simple bittorrent library
 *
 * Copyright (C) 2008 Alessandro Bahgat Shehata, Daniele Castagna
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Alessandro Bahgat Shehata - ale dot bahgat at gmail dot com
 * Daniele Castagna - daniele dot castagna at gmail dot com
 *
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
