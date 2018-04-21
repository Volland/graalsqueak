package de.hpi.swa.graal.squeak.instrumentation;

import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.util.FrameMarker;

// refer to com.oracle.truffle.api.interop.Message for documentation

@MessageResolution(receiverType = BaseSqueakObject.class)
public class BaseSqueakObjectMessageResolution {

    @Resolve(message = "WRITE")
    public abstract static class BaseSqueakObjectWriteNode extends Node {
        @SuppressWarnings("unused")
        public Object access(final BaseSqueakObject receiver, final Object name, final Object value) {
            throw new RuntimeException("Not yet implemented");
        }
    }

    @Resolve(message = "READ")
    public abstract static class BaseSqueakObjectReadNode extends Node {
        public Object access(final BaseSqueakObject receiver, final int index) {
            return receiver.at0(index);
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class BaseSqueakObjectHasSizeNode extends Node {
        public Object access(@SuppressWarnings("unused") final BaseSqueakObject receiver) {
            return true;
        }

        public Object access(@SuppressWarnings("unused") final FrameMarker marker) {
            return false;
        }
    }

    @Resolve(message = "HAS_KEYS")
    public abstract static class BaseSqueakObjectHasKeysNode extends Node {
        public Object access(@SuppressWarnings("unused") final Object receiver) {
            return false;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class BaseSqueakObjectGetSizeNode extends Node {
        public Object access(final BaseSqueakObject receiver) {
            return receiver.size();
        }
    }

    @Resolve(message = "INVOKE")
    public abstract static class BaseSqueakObjectInvokeNode extends Node {
        @SuppressWarnings("unused")
        public Object access(final BaseSqueakObject receiver, final String name, final Object[] arguments) {
            return "BaseSqueakObjectInvokeNode";
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class BaseSqueakObjectPropertyInfoNode extends Node {
        @SuppressWarnings("unused")
        public int access(final BaseSqueakObject receiver, final Object name) {
            return 0;
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class BaseSqueakObjectPropertiesNode extends Node {
        public Object access(@SuppressWarnings("unused") final Object receiver) {
            return null; // FIXME
        }
    }
}