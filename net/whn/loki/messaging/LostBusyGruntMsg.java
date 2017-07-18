/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.whn.loki.messaging;

import net.whn.loki.common.Task;

/**
 *
 * @author danieru
 */
public class LostBusyGruntMsg extends Msg{
    
    public LostBusyGruntMsg (long gID, Task t) {
        super(MsgType.LOST_BUSY_GRUNT);
        gruntID = gID;
        task = t;
    }
    
    public Task getGruntTask() {
        return task;
    }
    
    /*BEGIN PRIVATE*/
    final private long gruntID;
    final private Task task;
}
