package com.unicornshared;

import software.amazon.awscdk.services.logs.RetentionDays;

public interface UnicornStage {
    public enum Stage{
        local("local",RetentionDays.ONE_DAY),
        dev("dev",RetentionDays.ONE_WEEK),
        prod("prod",RetentionDays.TWO_WEEKS);

        public final String name;
        public final RetentionDays logRetentionDays;
        Stage(String name, RetentionDays logRetentionDays) {
            this.name=name;
            this.logRetentionDays=logRetentionDays;
        }
    }
}
