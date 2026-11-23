package com.velorise.simplemap.recipe;

import com.velorise.simplemap.SimpleMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, SimpleMap.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, CustomRecipe.Serializer<CopyMapBookRecipe>> COPY_RECIPE = SERIALIZERS.register("copy_book_map",
            () -> new CustomRecipe.Serializer<>(CopyMapBookRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, CustomRecipe.Serializer<MergeMapBooksRecipe>> MERGE_RECIPE = SERIALIZERS.register("merge_book_maps",
            () -> new CustomRecipe.Serializer<>(MergeMapBooksRecipe::new));
}
