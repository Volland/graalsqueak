/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakFFIPrimsFactory.ConvertFromFFINodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakFFIPrimsFactory.ConvertToFFINodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.ffi.FFIConstants.FFI_TYPES;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.PrimCalloutToFFINode;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class SqueakFFIPrims extends AbstractPrimitiveFactoryHolder {

    /** "primitiveCallout" implemented as {@link PrimCalloutToFFINode}. */

    @ImportStatic(FFI_TYPES.class)
    protected abstract static class ConvertFromFFINode extends Node {

        protected static ConvertFromFFINode create() {
            return ConvertFromFFINodeGen.create();
        }

        public abstract Object execute(int headerWord, Object value);

        @Specialization(guards = {"getAtomicType(headerWord) == 2 || getAtomicType(headerWord) == 3", "lib.fitsInInt(value)"}, limit = "1")
        protected static final long doByte(@SuppressWarnings("unused") final int headerWord, final Object value,
                        @CachedLibrary("value") final InteropLibrary lib) {
            try {
                return lib.asByte(value) & 0xff;
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 4 || getAtomicType(headerWord) == 5", "lib.fitsInInt(value)"}, limit = "1")
        protected static final long doShort(@SuppressWarnings("unused") final int headerWord, final Object value,
                        @CachedLibrary("value") final InteropLibrary lib) {
            try {
                return lib.asShort(value);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 6 || getAtomicType(headerWord) == 7", "lib.fitsInInt(value)"}, limit = "1")
        protected static final long doInt(@SuppressWarnings("unused") final int headerWord, final Object value,
                        @CachedLibrary("value") final InteropLibrary lib) {
            try {
                return lib.asInt(value);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final boolean value) {
            return (char) (value ? 0 : 1);
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final char value) {
            return value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final long value) {
            return (char) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final double value) {
            return (char) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "isPointerType(headerWord)"}, limit = "1")
        protected static final NativeObject doString(@SuppressWarnings("unused") final int headerWord, final Object value,
                        @CachedLibrary("value") final InteropLibrary lib,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return image.asByteString(lib.asString(value));
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        @Specialization(guards = "getAtomicType(headerWord) == 12")
        protected static final double doFloat(@SuppressWarnings("unused") final int headerWord, final float value) {
            return value;
        }

        @Fallback
        protected static final Object doFallback(@SuppressWarnings("unused") final int headerWord, final Object value) {
            return value;
        }
    }

    @ImportStatic(FFI_TYPES.class)
    protected abstract static class ConvertToFFINode extends Node {

        protected static ConvertToFFINode create() {
            return ConvertToFFINodeGen.create();
        }

        public abstract Object execute(int headerWord, Object value);

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final boolean value) {
            return (char) (value ? 0 : 1);
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final char value) {
            return value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final long value) {
            return (char) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final double value) {
            return (char) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "isPointerType(headerWord)"}, limit = "1")
        protected static final String doString(@SuppressWarnings("unused") final int headerWord, final Object value,
                        @CachedLibrary("value") final InteropLibrary lib) {
            try {
                return lib.asString(value);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        @Specialization(guards = "getAtomicType(headerWord) == 12")
        protected static final float doFloat(@SuppressWarnings("unused") final int headerWord, final double value) {
            return (float) value;
        }

        @Fallback
        protected static final Object doFallback(@SuppressWarnings("unused") final int headerWord, final Object value) {
            return value;
        }
    }

    public abstract static class AbstractFFIPrimitiveNode extends AbstractPrimitiveNode {
        protected static final String NFI_LANGUAGE_ID = "nfi";

        protected final boolean nfiAvailable;

        @Child private ConvertFromFFINode convertFromFFINode = ConvertFromFFINode.create();
        @Child private ConvertToFFINode convertToFFINode = ConvertToFFINode.create();
        @Child private AbstractPointersObjectReadNode readArgTypesNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectReadNode readCompiledSpecNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectReadNode readNameNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectReadNode readModuleNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectReadNode readClassNameNode = AbstractPointersObjectReadNode.create();

        public AbstractFFIPrimitiveNode(final CompiledMethodObject method) {
            super(method);
            nfiAvailable = method.image.env.getInternalLanguages().containsKey(NFI_LANGUAGE_ID);
        }

        protected final Object doCallout(final PointersObject externalLibraryFunction, final AbstractSqueakObject receiver, final Object... arguments) {
            if (!externalLibraryFunction.getSqueakClass().includesExternalFunctionBehavior()) {
                throw PrimitiveFailed.FFI_NOT_FUNCTION;
            }

            final List<Integer> headerWordList = new ArrayList<>();

            final ArrayObject argTypes = readArgTypesNode.executeArray(externalLibraryFunction, ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.ARG_TYPES);

            if (argTypes != null && argTypes.getObjectStorage().length == arguments.length + 1) {
                final Object[] argTypesValues = argTypes.getObjectStorage();

                for (final Object argumentType : argTypesValues) {
                    if (argumentType instanceof PointersObject) {
                        final NativeObject compiledSpec = readCompiledSpecNode.executeNative((PointersObject) argumentType, ObjectLayouts.EXTERNAL_TYPE.COMPILED_SPEC);
                        final int headerWord = compiledSpec.getIntStorage()[0];
                        headerWordList.add(headerWord);
                    }
                }
            }

            final Object[] argumentsConverted = getConvertedArgumentsFromHeaderWords(headerWordList, arguments);
            final List<String> nfiArgTypeList = getArgTypeListFromHeaderWords(headerWordList);

            final String name = readNameNode.executeNative(externalLibraryFunction, ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.NAME).asStringUnsafe();
            final String moduleName = getModuleName(receiver, externalLibraryFunction);
            final String nfiCodeParams = generateNfiCodeParamsString(nfiArgTypeList);
            try {
                final String nfiCode = generateNfiCode(name, moduleName, nfiCodeParams);
                final Object value = calloutToLib(name, argumentsConverted, nfiCode);
                assert value != null;
                return convertFromFFINode.execute(headerWordList.get(0), value);
            } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
                // TODO: return correct error code.
                throw PrimitiveFailed.GENERIC_ERROR;
            } catch (final Exception e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
                // TODO: handle exception
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        private Object[] getConvertedArgumentsFromHeaderWords(final List<Integer> headerWordList, final Object[] arguments) {
            final Object[] argumentsConverted = new Object[arguments.length];

            for (int j = 1; j < headerWordList.size(); j++) {
                argumentsConverted[j - 1] = convertToFFINode.execute(headerWordList.get(j), arguments[j - 1]);
            }
            return argumentsConverted;
        }

        private static List<String> getArgTypeListFromHeaderWords(final List<Integer> headerWordList) {
            final List<String> nfiArgTypeList = new ArrayList<>();

            for (final int headerWord : headerWordList) {
                final String atomicName = FFI_TYPES.getTruffleTypeFromInt(headerWord);
                nfiArgTypeList.add(atomicName);
            }
            return nfiArgTypeList;
        }

        private Object calloutToLib(final String name, final Object[] argumentsConverted, final String nfiCode)
                        throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
            final Source source = newNFISource(nfiCode);
            final Object ffiTest = method.image.env.parseInternal(source).call();
            final InteropLibrary interopLib = InteropLibrary.getFactory().getUncached(ffiTest);
            return interopLib.invokeMember(ffiTest, name, argumentsConverted);
        }

        @TruffleBoundary
        protected static final Source newNFISource(final String nfiCode) {
            return Source.newBuilder(NFI_LANGUAGE_ID, nfiCode, "native").build();
        }

        private String getModuleName(final AbstractSqueakObject receiver, final PointersObject externalLibraryFunction) {
            final Object moduleObject = readModuleNode.execute(externalLibraryFunction, ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.MODULE);
            if (moduleObject != NilObject.SINGLETON) {
                return ((NativeObject) moduleObject).asStringUnsafe();
            } else {
                return readClassNameNode.executeNative((PointersObject) receiver, ObjectLayouts.CLASS.NAME).asStringUnsafe();
            }
        }

        private String generateNfiCode(final String name, final String module, final String nfiCodeParams) {
            return MiscUtils.format("load \"%s\" {%s%s}", getLibraryPath(module), name, nfiCodeParams);
        }

        protected final String getLibraryPath(final String module) {
            final String libDirectory = System.getProperty("user.dir") + File.separatorChar + "lib";
            assert method.image.env.getTruffleFile(libDirectory).isDirectory();
            return libDirectory + File.separatorChar + module + method.image.os.getFFIExtension();
        }

        private static String generateNfiCodeParamsString(final List<String> argumentList) {
            String nfiCodeParams = "";
            if (!argumentList.isEmpty()) {
                final String returnType = argumentList.get(0);
                argumentList.remove(0);
                if (!argumentList.isEmpty()) {
                    nfiCodeParams = "(" + String.join(",", argumentList) + ")";
                } else {
                    nfiCodeParams = "()";
                }
                nfiCodeParams += ":" + returnType + ";";
            }
            return nfiCodeParams;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCalloutWithArgs")
    protected abstract static class PrimCalloutWithArgsNode extends AbstractFFIPrimitiveNode implements BinaryPrimitive {

        @Child private ArrayObjectToObjectArrayCopyNode getObjectArrayNode = ArrayObjectToObjectArrayCopyNode.create();

        protected PrimCalloutWithArgsNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "nfiAvailable")
        protected final Object doCalloutWithArgs(final PointersObject receiver, final ArrayObject argArray) {
            return doCallout(receiver, receiver, getObjectArrayNode.execute(argArray));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLoadSymbolFromModule")
    protected abstract static class PrimLoadSymbolFromModuleNode extends AbstractFFIPrimitiveNode implements TernaryPrimitive {

        protected PrimLoadSymbolFromModuleNode(final CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary
        @Specialization(guards = {"nfiAvailable", "moduleSymbol.isByteType()", "module.isByteType()"})
        protected final Object doLoadSymbol(final ClassObject receiver, final NativeObject moduleSymbol, final NativeObject module) {
            final String moduleSymbolName = moduleSymbol.asStringUnsafe();
            final String moduleName = module.asStringUnsafe();
            final String nfiCode = MiscUtils.format("load \"%s\"", getLibraryPath(moduleName));
            final CallTarget target = method.image.env.parseInternal(newNFISource(nfiCode));
            final Object library;
            try {
                library = target.call();
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final InteropLibrary lib = InteropLibrary.getFactory().getUncached();
            final Object symbol;
            try {
                symbol = lib.readMember(library, moduleSymbolName);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final long pointer;
            try {
                pointer = lib.asPointer(symbol);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
            return newExternalAddress(receiver, pointer);
        }

        private static NativeObject newExternalAddress(final ClassObject externalAddressClass, final long pointer) {
            final byte[] bytes = new byte[8];
            bytes[0] = (byte) pointer;
            bytes[1] = (byte) (pointer >> 8);
            bytes[2] = (byte) (pointer >> 16);
            bytes[3] = (byte) (pointer >> 24);
            bytes[4] = (byte) (pointer >> 32);
            bytes[5] = (byte) (pointer >> 40);
            bytes[6] = (byte) (pointer >> 48);
            bytes[7] = (byte) (pointer >> 56);
            return NativeObject.newNativeBytes(externalAddressClass.image, externalAddressClass, bytes);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIIntegerAt")
    protected abstract static class PrimFFIIntegerAtNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimFFIIntegerAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "isSigned"})
        protected static final long doAt2Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return (int) doAt2Unsigned(byteArray, byteOffsetLong, byteSize, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "!isSigned"})
        protected static final long doAt2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return Short.toUnsignedLong(UnsafeUtils.getShortFromBytes(byteArray.getByteStorage(), byteOffsetLong - 1));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned"})
        protected static final long doAt4Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return (int) doAt4Unsigned(byteArray, byteOffsetLong, byteSize, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned"})
        protected static final long doAt4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return Integer.toUnsignedLong(UnsafeUtils.getIntFromBytes(byteArray.getByteStorage(), byteOffsetLong - 1));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned"})
        protected static final Object doAt8Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = Arrays.copyOfRange(byteArray.getByteStorage(), byteOffset, byteOffset + 8);
            return new LargeIntegerObject(byteArray.image, byteArray.image.largePositiveIntegerClass, bytes).toSigned().reduceIfPossible();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned"})
        protected static final Object doAt8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = Arrays.copyOfRange(byteArray.getByteStorage(), byteOffset, byteOffset + 8);
            return new LargeIntegerObject(byteArray.image, byteArray.image.largePositiveIntegerClass, bytes).reduceIfPossible();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIIntegerAtPut")
    protected abstract static class PrimFFIIntegerAtPutNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        protected static final long MAX_VALUE_SIGNED_1 = 1L << 8 * 1 - 1;
        protected static final long MAX_VALUE_SIGNED_2 = 1L << 8 * 2 - 1;
        protected static final long MAX_VALUE_SIGNED_4 = 1L << 8 * 4 - 1;
        protected static final BigInteger MAX_VALUE_SIGNED_8 = BigInteger.ONE.shiftLeft(8 * 8 - 1);
        protected static final long MAX_VALUE_UNSIGNED_1 = 1L << 8 * 1;
        protected static final long MAX_VALUE_UNSIGNED_2 = 1L << 8 * 2;
        protected static final long MAX_VALUE_UNSIGNED_4 = 1L << 8 * 4;

        protected PrimFFIIntegerAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_1)"})
        protected static final Object doAtPut1Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut1Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_1)"})
        protected static final Object doAtPut1Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            byteArray.getByteStorage()[(int) byteOffsetLong - 1] = (byte) value;
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_2)"})
        protected static final Object doAtPut2Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut2Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_2)"})
        protected static final Object doAtPut2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            UnsafeUtils.putShortIntoBytes(byteArray.getByteStorage(), byteOffsetLong - 1, (short) value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_4)"})
        protected static final Object doAtPut4Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut4Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_4)"})
        protected static final Object doAtPut4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            UnsafeUtils.putIntIntoBytes(byteArray.getByteStorage(), byteOffsetLong - 1, (int) value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned", "value.fitsIntoLong()", "inSignedBounds(value.longValueExact(), MAX_VALUE_SIGNED_4)"})
        protected static final Object doAtPut4SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            return doAtPut4UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned", "value.fitsIntoLong()",
                        "inUnsignedBounds(value.longValueExact(), MAX_VALUE_UNSIGNED_4)"})
        @ExplodeLoop
        protected static final Object doAtPut4UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] targetBytes = byteArray.getByteStorage();
            final byte[] sourceBytes = value.getBytes();
            final int numSourceBytes = sourceBytes.length;
            for (int i = 0; i < 4; i++) {
                targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
            }
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned"})
        protected static final Object doAtPut8Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut8Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned", "inUnsignedBounds(asLargeInteger(value))"})
        protected static final Object doAtPut8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            UnsafeUtils.putLongIntoBytes(byteArray.getByteStorage(), byteOffsetLong - 1, value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_8)"})
        protected static final Object doAtPut8SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            return doAtPut8UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned", "inUnsignedBounds(value)"})
        @ExplodeLoop
        protected static final Object doAtPut8UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] targetBytes = byteArray.getByteStorage();
            final byte[] sourceBytes = value.getBytes();
            final int numSourceBytes = sourceBytes.length;
            for (int i = 0; i < 8; i++) {
                targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
            }
            return value;
        }

        protected static final boolean inSignedBounds(final long value, final long max) {
            return value >= 0 - max && value < max;
        }

        protected static final boolean inUnsignedBounds(final long value, final long max) {
            return 0 <= value && value < max;
        }

        @TruffleBoundary
        protected static final boolean inSignedBounds(final LargeIntegerObject value, final BigInteger max) {
            return value.getBigInteger().compareTo(BigInteger.ZERO.subtract(max)) >= 0 && value.getBigInteger().compareTo(max) < 0;
        }

        @TruffleBoundary
        protected static final boolean inUnsignedBounds(final LargeIntegerObject value) {
            return value.isZeroOrPositive() && value.lessThanOneShiftedBy64();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakFFIPrimsFactory.getFactories();
    }
}
