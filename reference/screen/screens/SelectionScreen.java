package me.alpestrine.c.reward.screen.screens;

import me.alpestrine.c.reward.config.DailyConfigHandler;
import me.alpestrine.c.reward.config.PlayerDataHandler;
import me.alpestrine.c.reward.config.PlaytimeConfigHandler;
import me.alpestrine.c.reward.config.objects.JsonBaseReward;
import me.alpestrine.c.reward.config.objects.JsonDailyReward;
import me.alpestrine.c.reward.config.objects.JsonPlayerData;
import me.alpestrine.c.reward.config.objects.JsonPlaytimeReward;
import me.alpestrine.c.reward.config.objects.JsonStack;
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
import top.bearcabbage.mirrortree.screen.WarpTeleportScreen;

public class SelectionScreen extends AbstractACScreen {

    public SelectionScreen() {
    }

    @Override
    public void addButtons(ServerPlayerEntity viewer) {

        // Wiki Button
        setButton(10, ItemBuilder.start(Items.KNOWLEDGE_BOOK)
                .name("§l§9游戏Wiki")
                .tooltip("棱镜树的小屋")
                .button(event -> event.player.sendMessage(Texts.bracketed(Text.literal("点击链接在浏览器打开棱镜树Wiki：https://wiki.mirror.bearcabbage.top/").formatted(Formatting.ITALIC).styled((style) -> style.withColor(Formatting.BLUE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://wiki.mirror.bearcabbage.top/")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击在浏览器打开棱镜树的小屋"))))))));

        // Daily Rewards Button
        setButton(12, ItemBuilder.start(Items.BELL)
                .name("§l§6日常奖励领取")
                .tooltip("每日登录和在线奖励领取柜台")
                .button(event -> event.player.openHandledScreen(new RewardsScreen())));

        // Warp Teleport Button
        setButton(14, ItemBuilder.start(Items.ENDER_PEARL)
                .name("§l§9聚落传送")
                .tooltip("点击打开传送点选择界面（正在努力恢复中）")
                .button(event -> event.player.sendMessage(Text.literal("小熊白菜在尽快恢复聚落功能啦"))/*event.player.openHandledScreen(new WarpTeleportScreen())*/));

        // Mailbox Button
        setButton(16, ItemBuilder.start(Items.CHEST)
                .name("§l§a邮件")
                .button(event -> {
                    MailGui mailGui = new MailGui(event.player);
                    mailGui.buildMailboxGui();
                    mailGui.open();
                }));
    }

    @Override
    public String getName() {
        return "§l欢迎来到棱镜树！§r";
    }


    @Override
    public void refresh(ServerPlayerEntity viewer) {
        init(viewer);
        viewer.currentScreenHandler.updateToClient();
    }

    @Override
    public int getPage() {
        return 1;
    }
}
