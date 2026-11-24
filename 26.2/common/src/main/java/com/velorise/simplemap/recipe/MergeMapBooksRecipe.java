package com.velorise.simplemap.recipe;

import com.mojang.serialization.MapCodec;
import com.velorise.simplemap.ModItems;
import com.velorise.simplemap.PlatformBridge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class MergeMapBooksRecipe extends CustomRecipe {
    public static final MergeMapBooksRecipe INSTANCE = new MergeMapBooksRecipe();
    public static final MapCodec<MergeMapBooksRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, MergeMapBooksRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public MergeMapBooksRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!PlatformBridge.areMapBooksReady()) return false;
        int bookWithDataCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.MAP_BOOK.get())) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                CompoundTag tag = customData != null ? customData.copyTag() : null;
                if (tag != null && tag.contains("MapBookID")) {
                    bookWithDataCount++;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        return bookWithDataCount == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (!PlatformBridge.areMapBooksReady()) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(ModItems.MAP_BOOK.get(), 1);

        CompoundTag tag = new CompoundTag();
        tag.putBoolean("PendingMerge", true);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        result.set(DataComponents.CUSTOM_NAME, Component.translatable("item.simplemap.merged_book_map"));
        return result;
    }

    @Override
    public RecipeSerializer<MergeMapBooksRecipe> getSerializer() {
        return ModRecipes.MERGE_RECIPE.get();
    }
}
