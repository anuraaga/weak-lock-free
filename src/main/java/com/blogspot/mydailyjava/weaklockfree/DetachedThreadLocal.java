package com.blogspot.mydailyjava.weaklockfree;

/**
 * <p>
 * A detached local that allows for explicit control of setting and removing values from a thread-local
 * context.
 * </p>
 * Instances of this class are non-blocking and fully thread safe.
 */
public class DetachedThreadLocal<T> implements Runnable {

    final WeakConcurrentMap<Thread, T> map;

    public DetachedThreadLocal(Cleaner cleaner) {
        switch (cleaner) {
            case THREAD:
            case MANUAL:
                map = new WeakConcurrentMap<Thread, T>(cleaner == Cleaner.THREAD) {
                    @Override
                    protected T defaultValue(Thread key) {
                        return DetachedThreadLocal.this.initialValue(key);
                    }
                };
                break;
            case INLINE:
                map = new WeakConcurrentMap.WithInlinedExpunction<Thread, T>() {
                    @Override
                    protected T defaultValue(Thread key) {
                        return DetachedThreadLocal.this.initialValue(key);
                    }
                };
                break;
            default:
                throw new AssertionError();
        }
    }

    public T get() {
        return map.get(Thread.currentThread());
    }

    public void set(T value) {
        map.put(Thread.currentThread(), value);
    }

    public void clear() {
        map.remove(Thread.currentThread());
    }

    /**
     * Clears all thread local references for all threads.
     */
    public void clearAll() {
        map.clear();
    }

    /**
     * @param thread The thread to which this thread's thread local value should be pushed.
     * @return The value being set.
     */
    public T pushTo(Thread thread) {
        T value = get();
        if (value != null) {
            map.put(thread, value);
        }
        return value;
    }

    /**
     * @param thread The thread from which the thread thread local value should be polled.
     * @return The value being set.
     */
    public T pullFrom(Thread thread) {
        T value = map.get(thread);
        if (value != null) {
            set(value);
        }
        return value;
    }

    /**
     * @param thread The thread for which an initial value is created.
     * @return The initial value for any thread local. If no default is set, the default value is {@code null}.
     */
    protected T initialValue(Thread thread) {
        return null;
    }

    /**
     * @return The cleaner thread or {@code null} if no such thread was set.
     */
    public Thread getCleanerThread() {
        return map.getCleanerThread();
    }

    @Override
    public void run() {
        map.run();
    }

    /**
     * Determines the cleaning format. A reference is removed either by an explicitly started cleaner thread
     * associated with this instance ({@link Cleaner#THREAD}), as a result of interacting with this thread local
     * from any thread ({@link Cleaner#INLINE} or manually by submitting the detached thread local to a thread
     * ({@link Cleaner#MANUAL}).
     */
    public enum Cleaner {
        THREAD, INLINE, MANUAL
    }
}
