package me.bishops_exe.rhmr.utils;

import java.util.concurrent.TimeUnit;

public record TimeAmount(long amount, TimeUnit unit) {
    public static TimeAmount fromSeconds(double seconds) {
        long nanos = Math.round(seconds * 1_000_000_000.0);
        TimeUnit[] units = {
                TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS
        };
        for (TimeUnit u : units) {
            long perUnit = u.toNanos(1);
            if (nanos % perUnit == 0) {
                return new TimeAmount(nanos / perUnit, u);
            }
        }
        return new TimeAmount(nanos, TimeUnit.NANOSECONDS);
    }
}

