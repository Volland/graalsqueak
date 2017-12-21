package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives.PrimAtNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives.PrimAtPutNode;

public class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {60, 210}, numArguments = 2)
    public static abstract class PrimBasicAtNode extends PrimAtNode {
        public PrimBasicAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected Object at(AbstractPointersObject receiver, int index) {
            return receiver.at0(index - 1 + receiver.instsize());
        }

        @Override
        @Specialization
        protected Object at(BaseSqueakObject receiver, int idx) {
            return super.at(receiver, idx);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {61, 211}, numArguments = 3)
    public static abstract class PrimBasicAtPutNode extends PrimAtPutNode {
        public PrimBasicAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected Object atput(AbstractPointersObject receiver, int idx, Object value) {
            receiver.atput0(idx - 1 + receiver.instsize(), value);
            return value;
        }

        @Override
        @Specialization
        protected Object atput(BaseSqueakObject receiver, int idx, Object value) {
            return super.atput(receiver, idx, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 62)
    public static abstract class PrimSizeNode extends AbstractPrimitiveNode {
        public PrimSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isNil(obj)")
        public int size(@SuppressWarnings("unused") char obj) {
            return 0;
        }

        @Specialization
        public int size(@SuppressWarnings("unused") boolean o) {
            return 0;
        }

        @Specialization
        public int size(@SuppressWarnings("unused") int o) {
            return 0;
        }

        @Specialization
        public int size(@SuppressWarnings("unused") long o) {
            return 0;
        }

        @Specialization
        public int size(String s) {
            return s.getBytes().length;
        }

        @Specialization
        public int size(BigInteger i) {
            return LargeInteger.byteSize(i);
        }

        @Specialization
        public int size(@SuppressWarnings("unused") double o) {
            return 2; // Float in words
        }

        @Specialization
        public int size(BaseSqueakObject obj) {
            return obj.size();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 63, numArguments = 2)
    public static abstract class PrimStringAtNode extends AbstractPrimitiveNode {
        public PrimStringAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        char stringAt(NativeObject obj, int idx) {
            byte nativeAt0 = ((Long) obj.getNativeAt0(idx - 1)).byteValue();
            return (char) nativeAt0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 64, numArguments = 3)
    public static abstract class PrimStringAtPutNode extends AbstractPrimitiveNode {
        public PrimStringAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        char atput(NativeObject obj, int idx, char value) {
            obj.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        char atput(NativeObject obj, int idx, int value) {
            char charValue = (char) ((Integer) value).byteValue();
            obj.setNativeAt0(idx - 1, charValue);
            return charValue;
        }
    }
}
