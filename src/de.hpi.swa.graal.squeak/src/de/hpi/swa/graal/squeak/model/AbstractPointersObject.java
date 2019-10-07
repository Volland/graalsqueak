/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.layout.Location;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayout;
import de.hpi.swa.graal.squeak.nodes.ObjectGraphNode.ObjectTracer;
import de.hpi.swa.graal.squeak.nodes.accessing.UpdateSqueakObjectHashNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractPointersObject extends AbstractSqueakObjectWithClassAndHash {
    private ObjectLayout layout;

    public int booleanUsedMap;
    public boolean boolean0;
    public boolean boolean1;
    public boolean boolean2;

    public int primitiveUsedMap;
    public long primitive0;
    public long primitive1;
    public long primitive2;

    public Object object0 = NilObject.SINGLETON;
    public Object object1 = NilObject.SINGLETON;
    public Object object2 = NilObject.SINGLETON;

    public boolean[] booleanExtension;
    public long[] primitiveExtension;
    public Object[] objectExtension;

    protected AbstractPointersObject(final SqueakImageContext image) {
        super(image);
    }

    protected AbstractPointersObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
        initializeLayoutAndExtensionsUnsafe();
    }

    protected AbstractPointersObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
    }

    protected AbstractPointersObject(final AbstractPointersObject original) {
        super(original);
        layout = original.layout;

        booleanUsedMap = original.booleanUsedMap;
        boolean0 = original.boolean0;
        boolean1 = original.boolean1;
        boolean2 = original.boolean2;

        primitiveUsedMap = original.primitiveUsedMap;
        primitive0 = original.primitive0;
        primitive1 = original.primitive1;
        primitive2 = original.primitive2;

        object0 = original.object0;
        object1 = original.object1;
        object2 = original.object2;

        if (original.booleanExtension != null) {
            booleanExtension = original.booleanExtension.clone();
        }
        if (original.primitiveExtension != null) {
            primitiveExtension = original.primitiveExtension.clone();
        }
        if (original.objectExtension != null) {
            objectExtension = original.objectExtension.clone();
        }
    }

    public final void copyLayoutValuesFrom(final AbstractPointersObject anotherObject) {
        assert layout == anotherObject.layout;
        booleanUsedMap = anotherObject.booleanUsedMap;
        boolean0 = anotherObject.boolean0;
        boolean1 = anotherObject.boolean1;
        boolean2 = anotherObject.boolean2;
        primitiveUsedMap = anotherObject.primitiveUsedMap;
        primitive0 = anotherObject.primitive0;
        primitive1 = anotherObject.primitive1;
        primitive2 = anotherObject.primitive2;
        object0 = anotherObject.object0;
        object1 = anotherObject.object1;
        object2 = anotherObject.object2;
        if (anotherObject.booleanExtension != null) {
            System.arraycopy(anotherObject.booleanExtension, 0, booleanExtension, 0, anotherObject.booleanExtension.length);
        }
        if (anotherObject.primitiveExtension != null) {
            System.arraycopy(anotherObject.primitiveExtension, 0, primitiveExtension, 0, anotherObject.primitiveExtension.length);
        }
        if (anotherObject.objectExtension != null) {
            System.arraycopy(anotherObject.objectExtension, 0, objectExtension, 0, anotherObject.objectExtension.length);
        }
    }

    protected final void initializeLayoutAndExtensionsUnsafe() {
        layout = getSqueakClass().getLayout();
        booleanExtension = layout.getFreshBooleanExtension();
        primitiveExtension = layout.getFreshPrimitiveExtension();
        objectExtension = layout.getFreshObjectExtension();
    }

    public final ObjectLayout getLayout() {
        if (layout == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            layout = getSqueakClass().getLayout();
        }
        return layout;
    }

    @TruffleBoundary
    public final void updateLayout() {
        final ObjectLayout latestLayout = getSqueakClass().getLayout();
        assert !layout.isValid() && layout != latestLayout : "Layout upgrade requested, but layout is latest";
        migrateToLayout(latestLayout);
    }

    @TruffleBoundary
    public final ObjectLayout updateLayout(final int index, final Object value) {
        assert !layout.getLocation(index).canStore(value);
        ObjectLayout latestLayout = getSqueakClass().getLayout();
        if (!latestLayout.getLocation(index).canStore(value)) {
            latestLayout = latestLayout.evolveLocation(getSqueakClass(), index, value);
        } else {
            assert !layout.isValid() && layout != latestLayout : "Layout must have changed";
        }
        migrateToLayout(latestLayout);
        return latestLayout;
    }

    private void migrateToLayout(final ObjectLayout newLayout) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert newLayout.isValid() : "Should not migrate to outdated layout";
        ObjectLayout theLayout = newLayout;
        final ObjectLayout oldLayout = layout;
        assert oldLayout.getInstSize() == newLayout.getInstSize();
        final int instSize = oldLayout.getInstSize();
        final Object[] changes = new Object[instSize];
        for (int i = 0; i < instSize; i++) {
            final Location oldLocation = oldLayout.getLocation(i);
            final Location newLocation = newLayout.getLocation(i);
            if (oldLocation != newLocation) {
                if (oldLocation.isSet(this)) {
                    changes[i] = oldLocation.read(this);
                }
            }
        }
        if (oldLayout.getNumBooleanExtension() != newLayout.getNumBooleanExtension()) {
            // primExt has grown ...
            if (booleanExtension == null) {
                assert oldLayout.getNumBooleanExtension() == 0;
                // ... primExt now needed
                booleanExtension = newLayout.getFreshBooleanExtension();
            } else {
                // ... resize booleanExt
                booleanExtension = Arrays.copyOf(booleanExtension, newLayout.getNumBooleanExtension());
            }
        }
        if (oldLayout.getNumPrimitiveExtension() != newLayout.getNumPrimitiveExtension()) {
            // primExt has grown ...
            if (primitiveExtension == null) {
                assert oldLayout.getNumPrimitiveExtension() == 0;
                // ... primExt now needed
                primitiveExtension = newLayout.getFreshPrimitiveExtension();
            } else {
                // ... resize primExt
                primitiveExtension = Arrays.copyOf(primitiveExtension, newLayout.getNumPrimitiveExtension());
            }
        }
        if (oldLayout.getNumObjectExtension() != newLayout.getNumObjectExtension()) {
            // objExt has grown ...
            if (objectExtension == null) {
                assert oldLayout.getNumObjectExtension() == 0;
                // ... objExt now needed
                objectExtension = newLayout.getFreshObjectExtension();
            } else {
                // ... resize objExt
                objectExtension = Arrays.copyOf(objectExtension, newLayout.getNumObjectExtension());
                for (int i = oldLayout.getNumObjectExtension(); i < newLayout.getNumObjectExtension(); i++) {
                    objectExtension[i] = NilObject.SINGLETON;
                }
            }
        }
        assert newLayout.getNumBooleanExtension() == 0 || newLayout.getNumBooleanExtension() == booleanExtension.length;
        assert newLayout.getNumPrimitiveExtension() == 0 || newLayout.getNumPrimitiveExtension() == primitiveExtension.length;
        assert newLayout.getNumObjectExtension() == 0 || newLayout.getNumObjectExtension() == objectExtension.length;

        for (int i = 0; i < instSize; i++) {
            final Location oldLocation = oldLayout.getLocation(i);
            final Location newLocation = newLayout.getLocation(i);
            if (oldLocation != newLocation) {
                if (changes[i] != null) {
                    final Object change = changes[i];
                    if (newLocation.canStore(change)) {
                        newLocation.writeMustSucceed(this, change);
                    } else {
                        theLayout = newLayout.evolveLocation(getSqueakClass(), i, change);
                        // TODO: is it possible that extensions need to be resized again?
                        theLayout.getLocation(i).writeMustSucceed(this, change);
                    }
                }
            }
        }
        layout = theLayout;
        assert layout.getNumBooleanExtension() == 0 || layout.getNumBooleanExtension() == booleanExtension.length;
        assert layout.getNumPrimitiveExtension() == 0 || layout.getNumPrimitiveExtension() == primitiveExtension.length;
        assert layout.getNumObjectExtension() == 0 || layout.getNumObjectExtension() == objectExtension.length;
    }

    public final void becomeLayout(final AbstractPointersObject other) {
        assert getClass() == other.getClass();
        becomeOtherClass(other);

        // Copy all values.
        final ObjectLayout otherLayout = other.layout;

        final int otherBooleanUsedMap = other.booleanUsedMap;
        final boolean otherBoolean0 = other.boolean0;
        final boolean otherBoolean1 = other.boolean1;
        final boolean otherBoolean2 = other.boolean2;

        final int otherPrimitiveUsedMap = other.primitiveUsedMap;
        final long otherPrimitive0 = other.primitive0;
        final long otherPrimitive1 = other.primitive1;
        final long otherPrimitive2 = other.primitive2;

        final Object otherObject0 = other.object0;
        final Object otherObject1 = other.object1;
        final Object otherObject2 = other.object2;

        final long[] otherPrimitiveExtension = other.primitiveExtension;
        final Object[] otherObjectExtension = other.objectExtension;

        // Move content from this object to the other.
        other.layout = layout;

        other.booleanUsedMap = booleanUsedMap;
        other.boolean0 = boolean0;
        other.boolean1 = boolean1;
        other.boolean2 = boolean2;

        other.primitiveUsedMap = primitiveUsedMap;
        other.primitive0 = primitive0;
        other.primitive1 = primitive1;
        other.primitive2 = primitive2;

        other.object0 = object0;
        other.object1 = object1;
        other.object2 = object2;

        other.primitiveExtension = primitiveExtension;
        other.objectExtension = objectExtension;

        // Move copied content to this object.
        layout = otherLayout;

        booleanUsedMap = otherBooleanUsedMap;
        boolean0 = otherBoolean0;
        boolean1 = otherBoolean1;
        boolean2 = otherBoolean2;

        primitiveUsedMap = otherPrimitiveUsedMap;
        primitive0 = otherPrimitive0;
        primitive1 = otherPrimitive1;
        primitive2 = otherPrimitive2;

        object0 = otherObject0;
        object1 = otherObject1;
        object2 = otherObject2;

        primitiveExtension = otherPrimitiveExtension;
        objectExtension = otherObjectExtension;
    }

    @Override
    public final int instsize() {
        assert getSqueakClass().getBasicInstanceSize() == getLayout().getInstSize();
        return getLayout().getInstSize();
    }

    public final boolean layoutValuesPointTo(final Object thang) {
        // FIXME: primitive values?
        return object0 == thang || object1 == thang || object2 == thang || objectExtension != null && ArrayUtils.contains(objectExtension, thang);
    }

    public final void layoutValuesBecomeOneWay(final UpdateSqueakObjectHashNode updateHashNode, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (object0 == fromPointer) {
                object0 = to[i];
                updateHashNode.executeUpdate(fromPointer, to[i], copyHash);
            }
            if (object1 == fromPointer) {
                object1 = to[i];
                updateHashNode.executeUpdate(fromPointer, to[i], copyHash);
            }
            if (object2 == fromPointer) {
                object2 = to[i];
                updateHashNode.executeUpdate(fromPointer, to[i], copyHash);
            }
            if (objectExtension != null) {
                for (int j = 0; j < objectExtension.length; j++) {
                    final Object object = objectExtension[j];
                    if (object == fromPointer) {
                        objectExtension[j] = to[i];
                        updateHashNode.executeUpdate(fromPointer, to[i], copyHash);
                    }
                }
            }
        }
    }

    protected final void traceLayoutObjects(final ObjectTracer tracer) {
        tracer.addIfUnmarked(object0);
        tracer.addIfUnmarked(object1);
        tracer.addIfUnmarked(object2);
        if (objectExtension != null) {
            for (final Object object : objectExtension) {
                tracer.addIfUnmarked(object);
            }
        }
    }
}
