package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class TemporaryWriteMarkContextsNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode writeNode;

    protected TemporaryWriteMarkContextsNode(final CompiledCodeObject code, final int tempIndex) {
        super(code);
        writeNode = FrameSlotWriteNode.create(code.getStackSlot(tempIndex));
    }

    public static TemporaryWriteMarkContextsNode create(final CompiledCodeObject code, final int tempIndex) {
        return TemporaryWriteMarkContextsNodeGen.create(code, tempIndex);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization
    protected final void doWriteContext(final VirtualFrame frame, final ContextObject value) {
        assert value != null;
        value.markEscaped();
        writeNode.executeWrite(frame, value);
    }

    @Specialization
    protected final void doWriteBlockClosure(final VirtualFrame frame, final BlockClosureObject value) {
        assert value != null;
        final ContextObject outerContext = value.getOuterContextOrNull();
        if (outerContext != null) {
            outerContext.markEscaped();
        }
        writeNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isContextObject(value)", "!isBlockClosureObject(value)"})
    protected final void doWriteOther(final VirtualFrame frame, final Object value) {
        assert value != null;
        writeNode.executeWrite(frame, value);
    }
}
