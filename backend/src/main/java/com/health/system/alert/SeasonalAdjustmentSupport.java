package com.health.system.alert;

import java.time.LocalDate;
import java.time.Month;

public final class SeasonalAdjustmentSupport {

    // Guangzhou subtropical monsoon: hot-humid season May through October.
    // During this period, vasodilation lowers BP by approximately 5 mmHg.
    private static final int GUANGZHOU_HOT_START_MONTH = 5;
    private static final int GUANGZHOU_HOT_END_MONTH = 10;
    private static final int BP_ADJUSTMENT_MMHG = 5;

    // Spring Festival / cooler dry season: November through April, no adjustment.

    private SeasonalAdjustmentSupport() {
    }

    public static boolean isGuangzhouHotSeason() {
        Month month = LocalDate.now().getMonth();
        int m = month.getValue();
        return m >= GUANGZHOU_HOT_START_MONTH && m <= GUANGZHOU_HOT_END_MONTH;
    }

    public static int adjustedSystolicThreshold(int baseSystolic) {
        if (isGuangzhouHotSeason()) {
            return baseSystolic + BP_ADJUSTMENT_MMHG;
        }
        return baseSystolic;
    }

    public static int adjustedDiastolicThreshold(int baseDiastolic) {
        if (isGuangzhouHotSeason()) {
            return baseDiastolic + BP_ADJUSTMENT_MMHG;
        }
        return baseDiastolic;
    }
}
