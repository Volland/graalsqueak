/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.FloatObjectNodesFactory.AsFloatObjectIfNessaryNodeGen;

public final class FloatObjectNodes {
    @ImportStatic(Double.class)
    public abstract static class AsFloatObjectIfNessaryNode extends AbstractNode {

        public static AsFloatObjectIfNessaryNode create() {
            return AsFloatObjectIfNessaryNodeGen.create();
        }

        public abstract Object execute(double value);

        @Specialization(guards = "isFinite(value)")
        protected static final double doFinite(final double value) {
            return value;
        }

        @Specialization(guards = "!isFinite(value)")
        protected static final FloatObject doNaNOrInfinite(final double value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return new FloatObject(image, value);
        }
    }
}
