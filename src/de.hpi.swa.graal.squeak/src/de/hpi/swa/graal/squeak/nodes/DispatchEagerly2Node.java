package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.FrameAccess.FrameArgumentsNode;

@ReportPolymorphism
@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchEagerly2Node extends AbstractNodeWithCode {
    protected static final int INLINE_CACHE_SIZE = 6;

    protected final int argumentCount;

    protected DispatchEagerly2Node(final CompiledCodeObject code, final int argumentCount) {
        super(code);
        this.argumentCount = argumentCount;
    }

    public static DispatchEagerly2Node create(final CompiledCodeObject code, final int argumentCount) {
        return DispatchEagerly2NodeGen.create(code, argumentCount);
    }

    public abstract Object executeDispatch(VirtualFrame frame, CompiledMethodObject method, Object receiver);

    @SuppressWarnings("unused")
    @Specialization(guards = {"method.hasPrimitive()", "method == cachedMethod", "primitiveNode != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected final Object doPrimitiveEagerly(final VirtualFrame frame, final CompiledMethodObject method, final Object receiver,
                    @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("cachedMethod.image.primitiveNodeFactory.forIndex(cachedMethod, cachedMethod.primitiveIndex(), argumentCount)") final AbstractPrimitiveNode primitiveNode) {
        final Object result = primitiveNode.executePrimitive(frame);
        // TODO: clear values
        FrameAccess.setStackPointer(frame, code, FrameAccess.getStackPointer(frame, code) - 1 - argumentCount);
        return result;
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()", "cachedMethod.getDoesNotNeedSenderAssumption()"}, replaces = "doPrimitiveEagerly")
    protected final Object doDirect(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method, final Object receiver,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode,
                    @Cached("create(code, argumentCount)") final FrameArgumentsNode argumentsNode) {
        return callNode.call(argumentsNode.execute(frame, cachedMethod, getContextOrMarker(frame), null, receiver));
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, replaces = {"doPrimitiveEagerly"})
    protected static final Object doDirectWithSender(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method, final Object receiver,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode,
                    @Cached("create(code, argumentCount)") final FrameArgumentsNode argumentsNode) {
        return callNode.call(argumentsNode.execute(frame, cachedMethod, getOrCreateContextNode.executeGet(frame), null, receiver));
    }

    @Specialization(guards = "method.getDoesNotNeedSenderAssumption().isValid()", replaces = {"doDirect", "doDirectWithSender"})
    protected final Object doIndirect(final VirtualFrame frame, final CompiledMethodObject method, final Object receiver,
                    @Cached("create(code, argumentCount)") final FrameArgumentsNode argumentsNode,
                    @Cached final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, method, getContextOrMarker(frame), null, receiver));
    }

    @Specialization(guards = "!method.getDoesNotNeedSenderAssumption().isValid()", replaces = {"doDirect", "doDirectWithSender"})
    protected static final Object doIndirectWithSender(final VirtualFrame frame, final CompiledMethodObject method, final Object receiver,
                    @Cached("create(code, argumentCount)") final FrameArgumentsNode argumentsNode,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, method, getOrCreateContextNode.executeGet(frame), null, receiver));
    }
}
