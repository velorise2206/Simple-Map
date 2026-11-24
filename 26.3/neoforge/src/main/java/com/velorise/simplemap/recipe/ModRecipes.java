package com.velorise.simplemap.recipe;

import com.velorise.simplemap.SimpleMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, SimpleMap.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CopyMapBookRecipe>> COPY_RECIPE = SERIALIZERS.register("copy_book_map",
            () -> new RecipeSerializer<>(CopyMapBookRecipe.CODEC, CopyMapBookRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MergeMapBooksRecipe>> MERGE_RECIPE = SERIALIZERS.register("merge_book_maps",
            () -> new RecipeSerializer<>(MergeMapBooksRecipe.CODEC, MergeMapBooksRecipe.STREAM_CODEC));
}
