/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import java.io.File;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.graalvm.home.HomeFinder;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakInterrupt;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class MiscUtils {
    private static final CompilationMXBean COMPILATION_BEAN = ManagementFactory.getCompilationMXBean();
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    private static final RuntimeMXBean RUNTIME_BEAN = ManagementFactory.getRuntimeMXBean();
    private static final List<GarbageCollectorMXBean> GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans();

    // The delta between Squeak Epoch (January 1st 1901) and POSIX Epoch (January 1st 1970)
    public static final long EPOCH_DELTA_SECONDS = (69L * 365 + 17) * 24 * 3600;
    public static final long EPOCH_DELTA_MICROSECONDS = EPOCH_DELTA_SECONDS * 1000 * 1000;
    public static final long TIME_ZONE_OFFSET_MICROSECONDS = (Calendar.getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(Calendar.DST_OFFSET)) * 1000L;
    public static final long TIME_ZONE_OFFSET_SECONDS = TIME_ZONE_OFFSET_MICROSECONDS / 1000 / 1000;

    private MiscUtils() {
    }

    public static int bitSplit(final long value, final int offset, final int length) {
        return (int) (value >> offset & (1 << length) - 1);
    }

    /** Ceil version of {@link Math#floorDiv(int, int)}. */
    public static int ceilDiv(final int x, final int y) {
        int r = x / y;
        // if the signs are the same and modulo not zero, round up
        if ((x ^ y) >= 0 && r * y != x) {
            r++;
        }
        return r;
    }

    @TruffleBoundary
    public static String format(final String format, final Object... args) {
        return String.format(format, args);
    }

    @TruffleBoundary
    public static long getCollectionCount() {
        long totalCollectionCount = 0;
        for (final GarbageCollectorMXBean gcBean : GC_BEANS) {
            totalCollectionCount += Math.min(gcBean.getCollectionCount(), 0);
        }
        return totalCollectionCount;
    }

    @TruffleBoundary
    public static long getCollectionTime() {
        long totalCollectionTime = 0;
        for (final GarbageCollectorMXBean gcBean : GC_BEANS) {
            totalCollectionTime += Math.min(gcBean.getCollectionTime(), 0);
        }
        return totalCollectionTime;
    }

    @TruffleBoundary
    public static String getGraalVMInformation() {
        final HomeFinder homeFinder = HomeFinder.getInstance();
        final StringBuilder sb = new StringBuilder();
        sb.append("GraalVM Version: ");
        sb.append(homeFinder.getVersion());
        sb.append("\nGraalVM Home Folder: ");
        sb.append(homeFinder.getHomeFolder().toString());
        sb.append("\nGraalVM Language Homes:\n");
        appendEntries(homeFinder.getLanguageHomes().entrySet(), sb);
        sb.append("GraalVM Tool Homes:\n");
        appendEntries(homeFinder.getToolHomes().entrySet(), sb);
        return sb.toString();
    }

    private static void appendEntries(final Set<Entry<String, Path>> set, final StringBuilder sb) {
        for (final Entry<String, Path> entry : set) {
            sb.append("  ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue().toString());
            sb.append("\n");
        }
    }

    @TruffleBoundary
    public static long getHeapMemoryMax() {
        return MEMORY_BEAN.getHeapMemoryUsage().getMax();
    }

    @TruffleBoundary
    public static long getHeapMemoryUsed() {
        return MEMORY_BEAN.getHeapMemoryUsage().getUsed();
    }

    @TruffleBoundary
    public static String getJavaClassPath() {
        return System.getProperty("java.class.path");
    }

    @TruffleBoundary
    public static long getObjectPendingFinalizationCount() {
        return MEMORY_BEAN.getObjectPendingFinalizationCount();
    }

    @TruffleBoundary
    public static long getStartTime() {
        return RUNTIME_BEAN.getStartTime();
    }

    @TruffleBoundary
    public static String getSystemProperties() {
        final Properties properties = System.getProperties();
        final StringBuilder sb = new StringBuilder();
        sb.append("\n\n== System Properties =================================>\n");
        final Object[] keys = properties.keySet().toArray();
        Arrays.sort(keys);
        for (final Object systemKey : keys) {
            final String key = (String) systemKey;
            sb.append(String.format("%s = %s\n", key, System.getProperty(key, "n/a")));
        }
        sb.append("<= System Properties ===================================\n\n");
        return sb.toString();
    }

    @TruffleBoundary
    public static long getTotalCompilationTime() {
        if (COMPILATION_BEAN.isCompilationTimeMonitoringSupported()) {
            return COMPILATION_BEAN.getTotalCompilationTime();
        } else {
            return -1L;
        }
    }

    @TruffleBoundary
    public static long getUptime() {
        return RUNTIME_BEAN.getUptime();
    }

    @TruffleBoundary
    public static String getVMInformation() {
        return String.format("\n%s (%s; %s)\n", System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.vm.info"));
    }

    @TruffleBoundary
    public static String getVMBinaryPath(final SqueakImageContext image) {
        return image.getLanguage().getHome() + File.separatorChar + "bin" + File.separatorChar + "graalsqueak";
    }

    @TruffleBoundary
    public static long runtimeFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    @TruffleBoundary
    public static long runtimeMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    @TruffleBoundary
    public static long runtimeTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    @TruffleBoundary
    public static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SqueakInterrupt();
        }
    }

    @TruffleBoundary
    public static String stringValueOf(final char value) {
        return String.valueOf(value);
    }

    @TruffleBoundary
    public static void systemGC() {
        System.gc();
    }

    @TruffleBoundary
    public static byte[] toBytes(final String value) {
        return value.getBytes();
    }

    public static long toJavaMicrosecondsUTC(final long microseconds) {
        return microseconds - EPOCH_DELTA_MICROSECONDS;
    }

    public static long toSqueakMicrosecondsLocal(final long microseconds) {
        return toSqueakMicrosecondsUTC(microseconds) + TIME_ZONE_OFFSET_MICROSECONDS;
    }

    public static long toSqueakMicrosecondsUTC(final long microseconds) {
        return microseconds + EPOCH_DELTA_MICROSECONDS;
    }

    public static long toSqueakSecondsLocal(final long seconds) {
        return seconds + EPOCH_DELTA_SECONDS + TIME_ZONE_OFFSET_SECONDS;
    }

    @TruffleBoundary
    public static String toString(final Object value) {
        return value.toString();
    }
}
