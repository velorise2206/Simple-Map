package com.velorise.simplemap.item;

import com.velorise.simplemap.MapBookHooks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class MapBookItem extends Item {
    public MapBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;

        if (tag == null || !tag.contains("MapBookID")) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("gui.simplemap.book.invalid"), false);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            MapBookHooks.useWrittenBook(player, hand, stack, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        com.velorise.simplemap.PlatformBridge.onMapBookCrafted(stack, player.level(), player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        if (tag == null || !tag.contains("MapBookID")) {
            tooltipAdder.accept(Component.translatable("item.simplemap.book_map.no_data"));
            return;
        }

        String bookId = tag.getString("MapBookID").orElse("");
        tooltipAdder.accept(Component.translatable("item.simplemap.book_map.map_id",
                bookId.substring(0, Math.min(8, bookId.length())) + "..."));
        tag.getString("OwnerName").ifPresent(owner ->
                tooltipAdder.accept(Component.translatable("item.simplemap.book_map.owner", owner)));
        tooltipAdder.accept(Component.translatable("item.simplemap.book_map.right_click_hint"));
        tooltipAdder.accept(Component.translatable("item.simplemap.book_map.shift_right_click_hint"));
    }
}
