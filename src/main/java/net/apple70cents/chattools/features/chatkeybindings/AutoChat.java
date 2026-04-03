package net.apple70cents.chattools.features.chatkeybindings;

import net.apple70cents.chattools.config.SpecialUnits;
import net.apple70cents.chattools.utils.ConfigUtils;
import net.apple70cents.chattools.utils.LoggerUtils;
import net.apple70cents.chattools.utils.MessageUtils;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-rule scheduling:
 * <ul>
 *   <li>Default: first send after {@link SpecialUnits.AutoChatUnit#initialDelaySeconds}, then every
 *       {@link SpecialUnits.AutoChatUnit#interval} seconds (wall-clock between sends, not tied to JVM start).</li>
 *   <li>Optional {@link SpecialUnits.AutoChatUnit#wallClockAligned}: fire when Unix time crosses into a new
 *       bucket of length {@code interval} seconds, shifted by {@code offsetSeconds % interval}.</li>
 * </ul>
 */
public class AutoChat {

    /** Next due time (ms since epoch) for non-wall-clock rules. */
    private static final Map<String, Long> nextDueMs = new HashMap<>();
    /** Last Unix bucket index for wall-clock rules. */
    private static final Map<String, Long> lastWallBucket = new HashMap<>();

    private static String scheduleKey(SpecialUnits.AutoChatUnit unit) {
        return unit.message + "\0" + unit.interval + "\0" + unit.initialDelaySeconds + "\0"
                + unit.wallClockAligned + "\0" + unit.offsetSeconds;
    }

    public static void tick() {
        boolean enabled = (boolean) ConfigUtils.get("chatkeybindings.AutoChat.Enabled");
        if (!enabled) {
            nextDueMs.clear();
            lastWallBucket.clear();
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        long now = System.currentTimeMillis();
        List<SpecialUnits.AutoChatUnit> units =
                SpecialUnits.AutoChatUnit.fromList((List) ConfigUtils.get("chatkeybindings.AutoChat.List"));

        for (SpecialUnits.AutoChatUnit unit : units) {
            if (!unit.abled) {
                continue;
            }
            if (unit.message == null || unit.message.trim().isEmpty()) {
                continue;
            }
            if (unit.interval <= 0) {
                continue;
            }

            String key = scheduleKey(unit);

            if (unit.wallClockAligned) {
                tickWallClock(unit, key, now);
            } else {
                tickPeriodic(unit, key, now);
            }
        }
    }

    private static void tickPeriodic(SpecialUnits.AutoChatUnit unit, String key, long nowMs) {
        Long nextDue = nextDueMs.get(key);
        if (nextDue == null) {
            nextDue = nowMs + Math.max(0L, unit.initialDelaySeconds) * 1000L;
            nextDueMs.put(key, nextDue);
        }
        if (nowMs >= nextDue) {
            LoggerUtils.info("[ChatTools] AutoChat: " + unit.message);
            MessageUtils.sendToPublicChat(unit.message);
            nextDueMs.put(key, nowMs + unit.interval * 1000L);
        }
    }

    private static void tickWallClock(SpecialUnits.AutoChatUnit unit, String key, long nowMs) {
        long epochSec = nowMs / 1000L;
        long interval = unit.interval;
        long phase = Math.floorMod(unit.offsetSeconds, interval);
        long bucket = Math.floorDiv(epochSec - phase, interval);
        Long last = lastWallBucket.get(key);
        if (last == null) {
            lastWallBucket.put(key, bucket);
            return;
        }
        if (bucket > last) {
            LoggerUtils.info("[ChatTools] AutoChat: " + unit.message);
            MessageUtils.sendToPublicChat(unit.message);
            lastWallBucket.put(key, bucket);
        }
    }
}
