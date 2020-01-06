package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class GetActiveProcessNode extends AbstractNode {

    public static GetActiveProcessNode create() {
        return GetActiveProcessNodeGen.create();
    }

    public static PointersObject getSlow(final SqueakImageContext image) {
        return doGet(AbstractPointersObjectReadNode.getUncached(), image);
    }

    public abstract PointersObject execute();

    @Specialization
    protected static final PointersObject doGet(
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return readNode.executePointers(image.getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);

    }
}
