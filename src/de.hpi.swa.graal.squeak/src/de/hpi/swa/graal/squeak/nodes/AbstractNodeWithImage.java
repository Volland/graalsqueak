/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public abstract class AbstractNodeWithImage extends AbstractNode {
    @CompilationFinal private ContextReference<SqueakImageContext> reference;

    protected final SqueakImageContext getImage() {
        if (reference == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            reference = lookupContextReference(SqueakLanguage.class);
        }
        return reference.get();
    }
}
