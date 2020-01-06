/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BooleanObject;

@ImportStatic({SqueakGuards.class, BooleanObject.class})
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNode extends Node {

    protected final SqueakImageContext getImageUncached() {
        return lookupContextReference(SqueakLanguage.class).get();
    }
}
