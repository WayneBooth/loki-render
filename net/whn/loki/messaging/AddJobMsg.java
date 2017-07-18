/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 12, 2009
 */

/**
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.whn.loki.messaging;

import net.whn.loki.common.LokiForm;
import net.whn.loki.master.*;

/**
 *
 * @author daniel
 */
public class AddJobMsg extends Msg {
    
    public AddJobMsg(JobFormInput j, LokiForm ajForm) {
        super(MsgType.ADD_JOB);
        jobInput = j;
        addingJobForm = ajForm;
    }

    public JobFormInput getJobInput() {
        return jobInput;
    }

    public void disposeAddingJobForm() {
        addingJobForm.dispose();
    }

    /*BEGIN PRIVATE*/
    private JobFormInput jobInput;
    private LokiForm addingJobForm;
}
