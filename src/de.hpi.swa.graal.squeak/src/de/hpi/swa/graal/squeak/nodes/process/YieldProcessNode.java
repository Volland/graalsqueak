package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public final class YieldProcessNode extends AbstractNodeWithCode {
    @Child private AbstractPointersObjectReadNode schedulerReadNode = AbstractPointersObjectReadNode.create();
    @Child private AbstractPointersObjectReadNode activeProcessReadNode = AbstractPointersObjectReadNode.create();
    @Child private AbstractPointersObjectReadNode processListReadNode = AbstractPointersObjectReadNode.create();
    @Child private LinkProcessToListNode linkProcessToListNode;
    @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
    @Child private ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.create();

    private YieldProcessNode(final CompiledCodeObject code) {
        super(code);
    }

    public static YieldProcessNode create(final CompiledCodeObject image) {
        return new YieldProcessNode(image);
    }

    public void executeYield(final VirtualFrame frame, final PointersObject scheduler) {
        final PointersObject activeProcess = (PointersObject) schedulerReadNode.executeRead(scheduler, PROCESS_SCHEDULER.ACTIVE_PROCESS);
        final long priority = (long) activeProcessReadNode.executeRead(activeProcess, PROCESS.PRIORITY);
        final ArrayObject processLists = (ArrayObject) schedulerReadNode.executeRead(scheduler, PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) arrayReadNode.execute(processLists, priority - 1);
        if (!processList.isEmptyList(processListReadNode)) {
            getLinkProcessToListNode().executeLink(activeProcess, processList);
            getWakeHighestPriorityNode().executeWake(frame);
        }
    }

    private LinkProcessToListNode getLinkProcessToListNode() {
        if (linkProcessToListNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            linkProcessToListNode = insert(LinkProcessToListNode.create());
        }
        return linkProcessToListNode;
    }

    private WakeHighestPriorityNode getWakeHighestPriorityNode() {
        if (wakeHighestPriorityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            wakeHighestPriorityNode = insert(WakeHighestPriorityNode.create(code));
        }
        return wakeHighestPriorityNode;
    }
}
