/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;

/**
 * A wrapper of a task, which essentially describes the needed resources for a task.
 * Contains same info as RequestResources.Request?
 * @author Andreas
 */
public class Task {
    final private int cpus;
    final private int mem;
    final private int milliseconds;
    final private long id;
    final Address address;

    public Task(int cpus, int mem, int milliseconds, long id, Address address) {
        this.cpus = cpus;
        this.mem = mem;
        this.milliseconds = milliseconds;
        this.id = id;
        this.address = address;
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

    public long getId() {
        return id;
    }
    
    public Address getAddress() {
        return address;
    }
}
