package me.alpestrine.c.reward.screen.screens;

import me.alpestrine.c.reward.config.DailyConfigHandler;
import me.alpestrine.c.reward.config.PlayerDataHandler;
import me.alpestrine.c.reward.config.PlaytimeConfigHandler;
import me.alpestrine.c.reward.config.objects.*;
import me.alpestrine.c.reward.screen.button.ItemBuilder;
import me.alpestrine.c.reward.server.MainServer;
import me.alpestrine.c.reward.util.IMath;
import me.alpestrine.c.reward.util.TimeFormatter;
import net.borisshoes.fabricmail.MailGui;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RewardsScreen extends AbstractACScreen {
    // Reward items
    public static final Item canClaimDailyItem = Items.BELL;
    public static final Item cannotClaimedDailyItem = Items.CLOCK;
    public static final Item canClaimPlaytime = Items.WHEAT;
    public static final Item cannotClaimPlaytime = Items.WHEAT_SEEDS;
    public static final Item backItem = Items.RED_CONCRETE;
    public static final Item prevItem = Items.BLUE_CONCRETE;
    public static final Item nextItem = Items.PURPLE_CONCRETE;
    public static final Item invalidItem = Items.GRAY_CONCRETE;

    // Config handlers
    public static final PlaytimeConfigHandler playtimeHandler = new PlaytimeConfigHandler();
    public static final DailyConfigHandler dailyHandler = new DailyConfigHandler();
    public static final PlayerDataHandler dataHandler = new PlayerDataHandler();


    public RewardsScreen() {
    }

    @Override
    public void addButtons(ServerPlayerEntity viewer) {
        JsonPlayerData data = dataHandler.getForUUID(viewer.getUuid());
        int currentPlaytime = data.playtimeSeconds;
        boolean hasJointToday = data.hasJointToday;

        // Daily Rewards Button
        ArrayList<Map.Entry<Integer, JsonDailyReward>> rewards_d = new ArrayList<>(getDailyRewards().entrySet());
        rewards_d.sort(Map.Entry.comparingByKey());
        if (!rewards_d.isEmpty()) {
            Map.Entry<Integer, JsonDailyReward> entry = rewards_d.get(0);
            JsonDailyReward jpr = entry.getValue();

            setButton(11, ItemBuilder.start(!hasJointToday ? canClaimDailyItem : cannotClaimedDailyItem)
                    .name(!hasJointToday
                            ? String.format("每日签到：" + jpr.getRewardItems().get(0).toItemStack().getCount() + jpr.getRewardItems().get(0).toItemStack().getName().getLiteralString())
                            : "今天已经签到过了，请明天再来吧")
                    .button(event -> {
                        onClick(false, !hasJointToday, event.player, data, jpr, Type.Daily);
                        data.hasJointToday = true;
                        refresh(event.player);
                    }));
        }

        // Playtime Rewards Button
        ArrayList<Map.Entry<Double, JsonPlaytimeReward>> rewards_p = new ArrayList<>(getPlaytimeRewards().entrySet());
        rewards_p.sort(Map.Entry.comparingByKey());
        if (!rewards_p.isEmpty()) {
            Map.Entry<Double, JsonPlaytimeReward> entry = rewards_p.get(0);
            int seconds = (int) Math.round(entry.getKey() * 60 * 60);
            JsonPlaytimeReward jpr = entry.getValue();
            boolean isClaimable = currentPlaytime >= seconds;

            setButton(15, ItemBuilder.start(isClaimable ? canClaimPlaytime : cannotClaimPlaytime)
                    .name(isClaimable
                            ? String.format("在线时长奖励: " + currentPlaytime / seconds * jpr.getRewardItems().get(0).toItemStack().getCount() + jpr.getRewardItems().get(0).toItemStack().getName().getLiteralString())
                            : String.format("距离下次可以领取在线奖励还有：%s", TimeFormatter.format(IMath.round((seconds - currentPlaytime) / 60, 3))))
                    .button(event -> {
                        while (data.playtimeSeconds >= seconds) {
                            onClick(false, isClaimable, event.player, data, jpr, Type.Playtime);
                            data.playtimeSeconds -= seconds;
                            refresh(event.player);
                        }
                    }));
        }
    }

    @Override
    public String getName() {
        return "日常奖励领取柜台";
    }

    protected Map<Double, JsonPlaytimeReward> getPlaytimeRewards() {
        return playtimeHandler.getItems();
    }

    protected Map<Integer, JsonDailyReward> getDailyRewards() {
        return dailyHandler.getItems();
    }



    @Override
    public void refresh(ServerPlayerEntity viewer) {
        init(viewer);
        viewer.currentScreenHandler.updateToClient();
    }

    protected String[] getToolTip(List<JsonStack> stacks) {
        ArrayList<String> tooltips = new ArrayList<>();
        for (JsonStack stack : stacks) {
            tooltips.add("");
            tooltips.addAll(stack.toToolTip());
        }
        return tooltips.toArray(new String[0]);
    }

    protected void onClick(boolean Claimed, boolean isClaimable, ServerPlayerEntity player, JsonPlayerData data, JsonBaseReward<?> jbr, Type type) {
        if (!Claimed && isClaimable) {
            List<ItemStack> stacks = jbr.getRewardItems().stream().map(JsonStack::toItemStack).toList();
            int empty = 0;
            for (ItemStack iStack : player.getInventory().main) {
                if (!iStack.isEmpty()) continue;
                empty++;
            }
            if (empty < stacks.size()) {
                player.sendMessage(Text.empty().append(String.format("你的背包装不下%s组东西了! 你只要%s个空位了.", stacks.size(), empty)).formatted(Formatting.RED));
                return;
            }
            for (ItemStack is : stacks) {
                player.getInventory().insertStack(is);
            }
            switch (type) {
                case Playtime -> data.claimedPlaytime.add(jbr.getId());
                case Daily -> data.claimedDaily.add(jbr.getId());
            }
            MainServer.configHandlerThread.execute(dataHandler::writeCurrentValue);
            refresh(player);
        }
    }

    @Override
    public int getPage() {
        return 1;
    }

    public enum Type { Playtime, Daily }
}
