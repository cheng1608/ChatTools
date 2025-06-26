package net.apple70cents.chattools.features.chatkeybindings;

import com.fasterxml.jackson.databind.JsonSerializer;
import net.apple70cents.chattools.utils.ConfigUtils;
import net.apple70cents.chattools.utils.LoggerUtils;
import net.apple70cents.chattools.utils.MessageUtils;
import net.apple70cents.chattools.config.SpecialUnits;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  "chatkeybindings.AutoChat.Enabled": false,
  "chatkeybindings.AutoChat.List": [],

{
          "type": "boolean",
          "key": "chatkeybindings.AutoChat.Enabled"
        },
        {
          "type": "AutoChatList",
          "key": "chatkeybindings.AutoChat.List"
        },

 */

public class AutoChat {
    private static final long startTime = System.currentTimeMillis();
    private static final Map<String, Long> lastPeriodMap = new HashMap<>();

    public static void tick() {
        boolean enabled = (boolean) ConfigUtils.get("chatkeybindings.AutoChat.Enabled");
        if (!enabled) return;
        if (Minecraft.getInstance().screen != null) return;

        long now = System.currentTimeMillis();
        long elapsed = (now - startTime) / 1000; //秒

        for (SpecialUnits.AutoChatUnit unit : SpecialUnits.AutoChatUnit.fromList((List) ConfigUtils.get("chatkeybindings.AutoChat.List"))) {
            if (!unit.abled) continue;
            if (unit.message == null || unit.message.trim().isEmpty()) continue;
            if (unit.interval <= 0) continue;

            String key = unit.message + "#" + unit.interval;

            long period = elapsed / unit.interval;
            Long lastPeriod = lastPeriodMap.get(key);

            if (lastPeriod == null || period > lastPeriod) {//刚好位于两个 period交界处时
                LoggerUtils.info("[ChatTools] AutoChat: " + unit.message);
                MessageUtils.sendToPublicChat(unit.message);
                lastPeriodMap.put(key, period);
            }
        }
    }
}



/*
public class AutoChat {
    public static void tick() {
        //boolean enabled = (boolean) ConfigUtils.get("chatkeybindings.AutoChat.Enabled");
    }
}
*/
