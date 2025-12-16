package top.bearcabbage.twodimensional_bedwars.screen.button;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    private ItemStack stack = ItemStack.EMPTY;

    private ItemBuilder() {
    }

    public static ItemBuilder start() {
        return new ItemBuilder();
    }

    public static ItemBuilder start(Item item) {
        return new ItemBuilder().item(item);
    }

    public static ItemBuilder start(ItemStack stack) {
        ItemBuilder builder = new ItemBuilder();
        builder.stack = stack;
        return builder;
    }

    public ItemBuilder item(Item item) {
        if (stack.isEmpty()) {
            stack = new ItemStack(item);
        } else {
            stack = stack.withItem(item);
        }
        return this;
    }

    public ItemBuilder name(String name) {
        return name(Text.of("§r".concat(name)));
    }

    public ItemBuilder name(Text name) {
        stack.set(DataComponentTypes.ITEM_NAME, name);
        return this;
    }

    public ItemBuilder tooltip(String... lines) {
        Text[] texts = new Text[lines.length];
        for (int i = 0; i < lines.length; i++) {
            MutableText mt = MutableText.of(Text.of("§r".concat(lines[i])).getContent());
            mt.setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE));
            texts[i] = mt;
        }
        return tooltip(texts);
    }

    public ItemBuilder tooltip(Text... lines) {
        List<Text> lineList = Arrays.asList(lines);
        return tooltip(new LoreComponent(lineList, lineList));
    }

    public ItemBuilder tooltip(LoreComponent lines) {
        stack.set(DataComponentTypes.LORE, lines);
        return this;
    }

    public ItemStack build() {
        return stack;
    }

    public ACButton button() {
        return new ACButton(stack);
    }

    public ACButton button(ACListener listener) {
        return new ACButton(stack).withListener(listener);
    }
}
