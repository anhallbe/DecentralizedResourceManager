/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

/**
 * A wrapper of a task, which essentially describes the needed resources for a task.
 * Contains same info as RequestResources.Request?
 * @author Andreas
 */
public class Task {
    private int cpus;
    private int mem;
    private int milliseconds;

    public Task(int cpus, int mem, int milliseconds) {
        this.cpus = cpus;
        this.mem = mem;
        this.milliseconds = milliseconds;
    }

    public int getCpus() {
        return cpus;
    }

    public int getMem() {
        return mem;
    }

    public int getMilliseconds() {
        return milliseconds;
    }
}
