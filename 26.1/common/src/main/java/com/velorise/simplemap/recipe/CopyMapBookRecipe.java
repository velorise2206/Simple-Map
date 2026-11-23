package com.velorise.simplemap.recipe;

import com.mojang.serialization.MapCodec;
import com.velorise.simplemap.ModItems;
import com.velorise.simplemap.PlatformBridge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CopyMapBookRecipe extends CustomRecipe {
    public static final CopyMapBookRecipe INSTANCE = new CopyMapBookRecipe();
    public static final MapCodec<CopyMapBookRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CopyMapBookRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public CopyMapBookRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!PlatformBridge.areMapBooksReady()) return false;
        int bookWithDataCount = 0;
        int emptyBookCount = 0;

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
            } else if (stack.is(ModItems.EMPTY_MAP_BOOK.get())) {
                emptyBookCount++;
            } else {
                return false;
            }
        }

        return bookWithDataCount == 1 && emptyBookCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (!PlatformBridge.areMapBooksReady()) return ItemStack.EMPTY;
        ItemStack sourceBook = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.MAP_BOOK.get())) {
                sourceBook = stack;
                break;
            }
        }

        if (sourceBook.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.MAP_BOOK.get(), 2);

        // Copy custom NBT data components
        CustomData customData = sourceBook.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            result.set(DataComponents.CUSTOM_DATA, customData);
        }

        // Copy custom name component
        if (sourceBook.has(DataComponents.CUSTOM_NAME)) {
            result.set(DataComponents.CUSTOM_NAME, sourceBook.get(DataComponents.CUSTOM_NAME));
        }

        return result;
    }

    @Override
    public RecipeSerializer<CopyMapBookRecipe> getSerializer() {
        return ModRecipes.COPY_RECIPE.get();
    }
}
