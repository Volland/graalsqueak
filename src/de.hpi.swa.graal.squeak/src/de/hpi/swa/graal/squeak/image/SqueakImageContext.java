package de.hpi.swa.graal.squeak.image;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.io.SqueakDisplay;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.POINT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectGraph;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.OSDetector;
import de.hpi.swa.graal.squeak.util.SqueakConfig;

public final class SqueakImageContext {
    // Special objects
    @CompilationFinal public final NilObject nil = new NilObject(this);
    @CompilationFinal public final boolean sqFalse = false;
    @CompilationFinal public final boolean sqTrue = true;
    @CompilationFinal public final PointersObject specialObjectsArray = new PointersObject(this);
    @CompilationFinal public final PointersObject schedulerAssociation = new PointersObject(this);
    @CompilationFinal public final ClassObject characterClass = new ClassObject(this);
    @CompilationFinal public final ClassObject smallIntegerClass = new ClassObject(this);
    @CompilationFinal public final ClassObject arrayClass = new ClassObject(this);
    @CompilationFinal public final PointersObject smalltalk = new PointersObject(this);
    @CompilationFinal public final NativeObject doesNotUnderstand = NativeObject.newNativeBytes(this, null, 0);
    @CompilationFinal public final PointersObject specialSelectors = new PointersObject(this);
    @CompilationFinal public final NativeObject mustBeBoolean = NativeObject.newNativeBytes(this, null, 0);
    @CompilationFinal public final ClassObject metaclass = new ClassObject(this);
    @CompilationFinal public final ClassObject methodContextClass = new ClassObject(this);
    @CompilationFinal public final ClassObject nilClass = new ClassObject(this);
    @CompilationFinal public final ClassObject trueClass = new ClassObject(this);
    @CompilationFinal public final ClassObject falseClass = new ClassObject(this);
    @CompilationFinal public final ClassObject stringClass = new ClassObject(this);
    @CompilationFinal public final ClassObject compiledMethodClass = new ClassObject(this);
    @CompilationFinal public final ClassObject blockClosureClass = new ClassObject(this);
    @CompilationFinal public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    @CompilationFinal public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    @CompilationFinal public final ClassObject floatClass = new ClassObject(this);

    @CompilationFinal private final SqueakLanguage language;
    @CompilationFinal private final PrintWriter output;
    @CompilationFinal private final PrintWriter error;
    @CompilationFinal public final SqueakLanguage.Env env;

    // Special selectors
    @CompilationFinal public final NativeObject plus = new NativeObject(this);
    @CompilationFinal public final NativeObject minus = new NativeObject(this);
    @CompilationFinal public final NativeObject lt = new NativeObject(this);
    @CompilationFinal public final NativeObject gt = new NativeObject(this);
    @CompilationFinal public final NativeObject le = new NativeObject(this);
    @CompilationFinal public final NativeObject ge = new NativeObject(this);
    @CompilationFinal public final NativeObject eq = new NativeObject(this);
    @CompilationFinal public final NativeObject ne = new NativeObject(this);
    @CompilationFinal public final NativeObject times = new NativeObject(this);
    @CompilationFinal public final NativeObject divide = new NativeObject(this);
    @CompilationFinal public final NativeObject modulo = new NativeObject(this);
    @CompilationFinal public final NativeObject pointAt = new NativeObject(this);
    @CompilationFinal public final NativeObject bitShift = new NativeObject(this);
    @CompilationFinal public final NativeObject floorDivide = new NativeObject(this);
    @CompilationFinal public final NativeObject bitAnd = new NativeObject(this);
    @CompilationFinal public final NativeObject bitOr = new NativeObject(this);
    @CompilationFinal public final NativeObject at = new NativeObject(this);
    @CompilationFinal public final NativeObject atput = new NativeObject(this);
    @CompilationFinal public final NativeObject sqSize = new NativeObject(this);
    @CompilationFinal public final NativeObject next = new NativeObject(this);
    @CompilationFinal public final NativeObject nextPut = new NativeObject(this);
    @CompilationFinal public final NativeObject atEnd = new NativeObject(this);
    @CompilationFinal public final NativeObject equivalent = new NativeObject(this);
    @CompilationFinal public final NativeObject klass = new NativeObject(this);
    @CompilationFinal public final NativeObject blockCopy = new NativeObject(this);
    @CompilationFinal public final NativeObject sqValue = new NativeObject(this);
    @CompilationFinal public final NativeObject valueWithArg = new NativeObject(this);
    @CompilationFinal public final NativeObject sqDo = new NativeObject(this);
    @CompilationFinal public final NativeObject sqNew = new NativeObject(this);
    @CompilationFinal public final NativeObject newWithArg = new NativeObject(this);
    @CompilationFinal public final NativeObject x = new NativeObject(this);
    @CompilationFinal public final NativeObject y = new NativeObject(this);

    @CompilationFinal(dimensions = 1) public final NativeObject[] specialSelectorsArray = new NativeObject[]{
                    plus, minus, lt, gt, le, ge, eq, ne, times, divide, modulo, pointAt, bitShift,
                    floorDivide, bitAnd, bitOr, at, atput, sqSize, next, nextPut, atEnd, equivalent,
                    klass, blockCopy, sqValue, valueWithArg, sqDo, sqNew, newWithArg, x, y
    };

    @CompilationFinal(dimensions = 1) public final int[] specialSelectorsNumArgs = new int[]{
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0
    };

    @CompilationFinal public final SqueakConfig config;
    @CompilationFinal public final SqueakDisplay display;
    @CompilationFinal public final SqueakImageFlags flags = new SqueakImageFlags();
    @CompilationFinal public final ObjectGraph objects = new ObjectGraph(this);
    @CompilationFinal public final OSDetector os = new OSDetector();
    @CompilationFinal public final InterruptHandlerNode interrupt;
    @CompilationFinal public final long startUpMillis = System.currentTimeMillis();

    @CompilationFinal public AbstractSqueakObject asSymbol = nil; // for testing
    @CompilationFinal public AbstractSqueakObject simulatePrimitiveArgs = nil;

    public SqueakImageContext(final SqueakLanguage squeakLanguage, final SqueakLanguage.Env environ,
                    final PrintWriter out, final PrintWriter err) {
        language = squeakLanguage;
        env = environ;
        output = out;
        error = err;
        final String[] applicationArguments = env.getApplicationArguments();
        config = new SqueakConfig(applicationArguments);
        display = SqueakDisplay.create(this, config.isCustomContext());
        interrupt = InterruptHandlerNode.create(this, config);
    }

    // for testing
    public SqueakImageContext(final String imagePath) {
        language = null;
        env = null;
        output = new PrintWriter(System.out, true);
        error = new PrintWriter(System.err, true);
        config = new SqueakConfig(new String[]{imagePath, "--testing"});
        display = SqueakDisplay.create(this, true);
        interrupt = InterruptHandlerNode.create(this, config);
    }

    public CallTarget getActiveContext() {
        // TODO: maybe there is a better way to do the below
        final PointersObject activeProcess = GetActiveProcessNode.create(this).executeGet();
        final ContextObject activeContext = (ContextObject) activeProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, nil);
        output.println("Resuming active context for " + activeContext.getMethod() + "...");
        return Truffle.getRuntime().createCallTarget(ExecuteTopLevelContextNode.create(language, activeContext));
    }

    public CallTarget getCustomContext() {
        final Object receiver = config.getReceiver();
        final String selector = config.getSelector();
        final ClassObject receiverClass = receiver instanceof Long ? smallIntegerClass : nilClass;
        final CompiledMethodObject lookupResult = (CompiledMethodObject) receiverClass.lookup(selector);
        if (lookupResult.getCompiledInSelector() == doesNotUnderstand) {
            throw new SqueakException(receiver + " >> " + selector + " could not be found!");
        }
        final ContextObject customContext = ContextObject.create(this, lookupResult.frameSize());
        customContext.atput0(CONTEXT.METHOD, lookupResult);
        customContext.atput0(CONTEXT.INSTRUCTION_POINTER, (long) lookupResult.getInitialPC());
        customContext.atput0(CONTEXT.RECEIVER, receiver);
        customContext.atput0(CONTEXT.STACKPOINTER, 1L);
        customContext.atput0(CONTEXT.CLOSURE_OR_NIL, nil);
        customContext.setSender(nil);
        // if there were arguments, they would need to be pushed before the temps
        final long numTemps = lookupResult.getNumTemps() - lookupResult.getNumArgs();
        for (int i = 0; i < numTemps; i++) {
            customContext.push(nil);
        }

        output.println("Starting to evaluate " + receiver + " >> " + selector + "...");
        return Truffle.getRuntime().createCallTarget(ExecuteTopLevelContextNode.create(getLanguage(), customContext));
    }

    public void fillInFrom(final FileInputStream inputStream, final VirtualFrame frame) throws IOException {
// output.println("Going to sleep...");
// try {
// Thread.sleep(10000);
// } catch (InterruptedException e) {
// output.println(e.getMessage());
// }
// output.println("Waking and now Loading!");
        final long start = System.nanoTime();
        SqueakImageReader.readImage(this, inputStream, frame);
        final long stop = System.nanoTime();
        final long delta = stop - start;
        final double deltaf = (delta / 1000_000) / 1000.0;
        output.println("LoadImage:\t" + deltaf + "s");
        System.exit(0);
        if (!display.isHeadless() && simulatePrimitiveArgs.isNil()) {
            throw new SqueakException("Unable to find BitBlt simulation in image, cannot run with display.");
        }
    }

    public PrintWriter getOutput() {
        return output;
    }

    public PrintWriter getError() {
        return error;
    }

    public SqueakLanguage getLanguage() {
        return language;
    }

    public Object wrap(final Object obj) {
        if (obj == null) {
            return nil;
        } else if (obj instanceof Boolean) {
            return wrap((boolean) obj);
        } else if (obj instanceof Integer) {
            return wrap((long) Long.valueOf((Integer) obj));
        } else if (obj instanceof Long) {
            return wrap((long) obj);
        } else if (obj instanceof Double) {
            return wrap((double) obj);
        } else if (obj instanceof BigInteger) {
            return wrap((BigInteger) obj);
        } else if (obj instanceof String) {
            return wrap((String) obj);
        } else if (obj instanceof Character) {
            return wrap((char) obj);
        } else if (obj instanceof Object[]) {
            return wrap((Object[]) obj);
        } else if (obj instanceof Point) {
            return wrap((Point) obj);
        } else if (obj instanceof Dimension) {
            return wrap((Dimension) obj);
        }
        throw new SqueakException("Don't know how to wrap " + obj);
    }

    public Object wrap(final boolean value) {
        return value ? sqTrue : sqFalse;
    }

    @SuppressWarnings("static-method")
    public long wrap(final long l) {
        return l;
    }

    public AbstractSqueakObject wrap(final BigInteger i) {
        return new LargeIntegerObject(this, i);
    }

    public FloatObject wrap(final double value) {
        return new FloatObject(this, value);
    }

    public NativeObject wrap(final String s) {
        return NativeObject.newNativeBytes(this, stringClass, s.getBytes());
    }

    public NativeObject wrap(final byte[] bytes) {
        return NativeObject.newNativeBytes(this, (ClassObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassByteArray), bytes);
    }

    public static char wrap(final char character) {
        return character;
    }

    @TruffleBoundary
    public PointersObject wrap(final Object... elements) {
        final Object[] wrappedElements = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            wrappedElements[i] = wrap(elements[i]);
        }
        return newList(wrappedElements);
    }

    public PointersObject wrap(final Point point) {
        return newPoint((long) point.getX(), (long) point.getY());
    }

    public PointersObject wrap(final Dimension dimension) {
        return newPoint((long) dimension.getWidth(), (long) dimension.getHeight());
    }

    public PointersObject newList(final Object[] elements) {
        return new PointersObject(this, arrayClass, elements);
    }

    public PointersObject newListWith(final Object... elements) {
        return newList(elements);
    }

    public PointersObject newPoint(final Object xPos, final Object yPos) {
        final ClassObject pointClass = (ClassObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassPoint);
        final PointersObject newPoint = (PointersObject) pointClass.newInstance();
        newPoint.atput0(POINT.X, xPos);
        newPoint.atput0(POINT.Y, yPos);
        return newPoint;
    }

    public NativeObject newSymbol(final String value) {
        return NativeObject.newNativeBytes(this, doesNotUnderstand.getSqClass(), value.getBytes());
    }

    public void registerSemaphore(final AbstractSqueakObject semaphore, final long index) {
        specialObjectsArray.atput0(index, semaphore.isSemaphore() ? semaphore : nil);
    }

    public String imageRelativeFilePathFor(final String fileName) {
        return config.getImageDirectory() + File.separator + fileName;
    }

    public void trace(final Object... arguments) {
        if (config.isTracing()) {
            printToStdout(arguments);
        }
    }

    public void traceVerbose(final Object... arguments) {
        if (config.isTracing() && config.isVerbose()) {
            printToStdout(arguments);
        }
    }

    @TruffleBoundary
    private void printToStdout(final Object[] arguments) {
        final List<String> strings = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            strings.add(arguments[i].toString());
        }
        getOutput().println(String.join(" ", strings));
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        final boolean isTravisBuild = System.getenv().containsKey("TRAVIS");
        final int[] depth = new int[1];
        final Object[] lastSender = new Object[]{null};
        getOutput().println("== Squeak stack trace ===========================================================");
        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {

            @Override
            public Object visitFrame(final FrameInstance frameInstance) {
                if (depth[0]++ > 50 && isTravisBuild) {
                    return null;
                }
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (current.getArguments().length < FrameAccess.RECEIVER) {
                    return null;
                }
                final Object method = FrameAccess.getMethod(current);
                lastSender[0] = FrameAccess.getSender(current);
                final Object contextOrMarker = FrameAccess.getContextOrMarker(current);
                final Object[] arguments = FrameAccess.getReceiverAndArguments(current);
                final String[] argumentStrings = new String[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    argumentStrings[i] = arguments[i].toString();
                }
                final String prefix = FrameAccess.getClosure(current) == null ? "" : "[] in ";
                getOutput().println(String.format("%s%s #(%s) [this: %s, sender: %s]", prefix, method, String.join(", ", argumentStrings), contextOrMarker, lastSender[0]));
                return null;
            }
        });
        getOutput().println("== " + depth[0] + " Truffle frames ================================================================");
        if (lastSender[0] instanceof ContextObject) {
            ((ContextObject) lastSender[0]).printSqStackTrace();
        }
    }
}
