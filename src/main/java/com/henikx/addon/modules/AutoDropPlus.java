package com.henikx.addon.modules;

import com.henikx.addon.Meteor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AutoDropPlus extends Module{
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<List<Item>> autoDropItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("auto-drop-items")
        .description("Items to drop.")
        .build()
    );

    private final Setting<Boolean> sellInstead = sgGeneral.add(new BoolSetting.Builder()
        .name("sell-instead")
        .description("!!! Requires EssentialsX plugin !!! Sell the items instead of dropping them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDropExcludeHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("exclude-hotbar")
        .description("Whether or not to drop items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> countHotbarItems = sgGeneral.add(new BoolSetting.Builder()
        .name("count-hotbar")
        .description("Whether or not to count items in the hotbar although they arent dropped")
        .visible(autoDropExcludeHotbar::get)
        .defaultValue(false)
        .build());

    private final Setting<Boolean> autoDropOnlyFullStacks = sgGeneral.add(new BoolSetting.Builder()
        .name("only-full-stacks")
        .description("Only drops the items if the stack is full.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> stacksToKeep = sgGeneral.add(new IntSetting.Builder()
        .name("stacks-to-keep")
        .description("How many stacks of items to keep before throwing anny out")
        .defaultValue(4)
        .min(0)
        .max(36)
        .sliderRange(0,36)
        .build());

    private final Setting<Boolean> countIndiviual = sgGeneral.add(new BoolSetting.Builder()
        .name("count-individual")
        .description("Whether or not to count multiple items individually or to group all items together.")
        .defaultValue(true)
        .visible(() -> autoDropItems.get().size() >= 2)
        .build()
    );


    public AutoDropPlus() {
        super(Meteor.CATEGORY, "Auto Drop Plus", "A more advanced implementation of Auto Drop");
    }

    //Float because it counts in stacks or slots not items to account for multiple items with different max stack counts
    private final Map<String, Float> currentStackCount = new HashMap<>();

    @EventHandler
    private void onTickPost(TickEvent.Post event){

        //copied from the official auto drop
        // no idea what the first thing does, second stops dropping while in screens, third stops dropping when no items selected
        if (!Utils.canUpdate() || mc.currentScreen instanceof HandledScreen<?> || autoDropItems.get().isEmpty()) return;



        // When the items are counted indiviually create an entry for every selected item, else create one with the key default
        // Initialize the value either way as 0
        if(countIndiviual.get()){
            for (Item item: autoDropItems.get()) {
                currentStackCount.put(item.getName().toString(),0f);
            }
        }
        else {
            currentStackCount.put("default", 0f);
        }



        //Count
        if (mc.player == null) return;

        for (int i = (autoDropExcludeHotbar.get() && !countHotbarItems.get()) ? 9 : 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);


            if (autoDropItems.get().contains(itemStack.getItem())){

                // divide through the max count so when countIndividual is false and items with varying max stack counts are selected it still works
                if(countIndiviual.get()){
                    currentStackCount.put(itemStack.getName().toString(), currentStackCount.get(itemStack.getName().toString()) + itemStack.getCount() / itemStack.getMaxCount());
                }
                else {

                    currentStackCount.put("default", currentStackCount.get("default") + itemStack.getCount() / itemStack.getMaxCount());
                }
            }
        }


        //Drop
        //iterate from last to first to keep items in hotbar instead of
        for (int i = mc.player.getInventory().size(); i >= ((autoDropExcludeHotbar.get() && !countHotbarItems.get()) ? 9 : 0 ); i--) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (autoDropItems.get().contains(itemStack.getItem())){

                if(countIndiviual.get()){

                    executeDrop(itemStack, i, itemStack.getName().toString());

                }
                else {

                    executeDrop(itemStack, i, "default");

                }
            }

        }



    }

    private void executeDrop(ItemStack itemStack, int i, String key) {


        if ((!autoDropOnlyFullStacks.get() || itemStack.getCount() == itemStack.getMaxCount()) &&
            !SlotUtils.isArmor(i) && currentStackCount.get(key) - (float) itemStack.getCount()/itemStack.getMaxCount() >= stacksToKeep.get())
        {
            currentStackCount.put(key, currentStackCount.get(key) - itemStack.getCount()/itemStack.getMaxCount());

            if(sellInstead.get())
                ChatUtils.sendPlayerMsg("/sell " + itemStack.getItem().toString().replace("minecraft:","") + " " + itemStack.getMaxCount());

            else InvUtils.drop().slot(i);
        }
    }


}
