package com.aleosiss.endlessrenewal.access;

import java.util.concurrent.Callable;

public interface ServerTimerAccess {
    void endlessrenewal$setTimer(long ticksUntil, Callable<Boolean> runnable);
}
