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

package net.whn.loki.common;

/**
 *provides a common set of enums
 * @author daniel
 */
public interface ICommon {

    enum FileCacheType {BLEND, BLEND_CACHE};

    enum LokiRole { GRUNT, GRUNTCL, MASTER, MASTER_GRUNT, ASK };

    /**
     *A - remaining, stopped
     *B - remaining, tasks RUNNING
     *C - all assigned, RUNNING
     *D - all tasks finished or ABORTED
     */
    enum JobStatus { A, B, C, D };

    /**
     * //TODO - is 'requested' really necessary? investigate during
     * next task algorithm...
     * 
     */
    enum TaskStatus { READY, RUNNING, DONE, FAILED,
        LOCAL_ABORT, MASTER_ABORT, LOST_GRUNT };

    enum ReturnStatus { DONE, FAILED };

    /**
     * expand this as needed
     */
    enum JobType { BLENDER };

    /**
     * expand as needed
     */
    enum MsgType {

        SHUTDOWN,
        FATAL_ERROR,
        START_QUEUE,
        STOP_QUEUE,
        ADD_JOB,
        VIEW_JOB,
        REMOVE_JOBS,
        ABORT_ALL,
        ADD_GRUNT,
        UPDATE_GRUNT,
        REMOVE_GRUNT,
        IDLE_GRUNT,
        VIEW_GRUNT,
        QUIT_GRUNT,
        LOST_BUSY_GRUNT,
        QUIT_ALL_GRUNTS,
        TASK_ASSIGN,
        TASK_REPORT,
        RESET_FAILURES,
        FILE_REQUEST
    };

    /**
     * grunt status as displayed in gruntList
     */
    enum GruntStatus { IDLE, BUSY };

    /**
     * grunt update text status
     */
    enum GruntTxtStatus { 
        IDLE, BUSY, FETCH, PREP_CACHE, SEND,
        PENDING_SEND, ABORT, ERROR
    };

    /**
     * header type for network communication. this object is the lead object
     * sent on the stream and defines to the receiver what to do: carry out task
     * requesting a file, sending a file, etc. expand as needed
     */
    enum HdrType {
        MACHINE_INFO,
        MACHINE_UPDATE,
        NO_DELIVERY,
        TASK_ASSIGN,
        TASK_REPORT,
        TASK_ABORT,
        QUIT_AFTER_TASK,
        FILE_REQUEST,
        FILE_REPLY,
        MASTER_SHUTDOWN,
        IDLE
        };
}
