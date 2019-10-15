/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameMarker {
    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "FrameMarker@" + Integer.toHexString(System.identityHashCode(this));
    }

    public ContextObject getMaterializedContext() {
        final Object[] values = FrameAccess.findFrameForMarker(this);
        final MaterializedFrame targetFrame = (MaterializedFrame) values[0];
        final ContextObject context = FrameAccess.getContext(targetFrame);
        if (context != null) {
            assert context.getFrameMarker() == this;
            return context;
        } else {
            assert this == FrameAccess.getMarker(targetFrame) : "Frame does not match";
            return ContextObject.create(targetFrame, (CompiledCodeObject) values[1]);
        }
    }
}
