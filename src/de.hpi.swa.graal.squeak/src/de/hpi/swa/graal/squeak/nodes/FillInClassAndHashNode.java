package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;

public abstract class FillInClassAndHashNode extends Node {

    public static FillInClassAndHashNode create() {
        return FillInClassAndHashNodeGen.create();
    }

    public abstract void execute(Object obj, SqueakImageChunk chunk);

    // For special objects (known selectors, classes, ...) which do not have a hash yet.
    @Specialization(guards = {"!obj.hasSqueakClass()", "!obj.hasSqueakHash()"})
    protected static final void doClassObjectClassAndHash(final AbstractSqueakObject obj, final SqueakImageChunk chunk) {
        obj.setSqueakClass(chunk.getSqClass());
        obj.setSqueakHash(chunk.getHash());
    }

    @Specialization(guards = {"!obj.hasSqueakClass()", "obj.hasSqueakHash()"})
    protected static final void doClassObjectClass(final AbstractSqueakObject obj, final SqueakImageChunk chunk) {
        obj.setSqueakClass(chunk.getSqClass());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doNothing(final Object obj, final SqueakImageChunk chunk) {
        // Nothing to do.
    }
}