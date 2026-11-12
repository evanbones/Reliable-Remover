package com.evandev.reliable_remover.config;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.data.Action;
import com.evandev.reliable_remover.data.RemovalRule;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
//?} else {
/*import net.minecraft.world.entity.decoration.PaintingVariant;
*///?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21 {
import net.minecraft.world.item.alchemy.PotionContents;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
*///?}
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
//? if >=1.21 {
import net.minecraft.world.item.enchantment.ItemEnchantments;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RuleManager {
    private static volatile Map<Action, List<RemovalRule>> RULES_BY_ACTION = new EnumMap<>(Action.class);
    private static volatile Map<Action, RuleIndex> RULE_INDEX = new EnumMap<>(Action.class);
    private static volatile Set<String> GLOBALLY_BANNED_ITEMS = ConcurrentHashMap.newKeySet();
    private static volatile List<String> BLACKLISTED_ITEMS = List.of();
    private static volatile Set<String> CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger RULES_GENERATION = new AtomicInteger();
    private static final Map<Item, Boolean> RECIPE_VIEWER_BLOCKED = new ConcurrentHashMap<>();
    private static volatile int recipeViewerGeneration = -1;
    private static volatile Runnable CNM_CASCADE_RECOMPUTE_HOOK = null;
    private static final List<Runnable> SYNCED_RULES_CHANGED_HOOKS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean CREATIVE_TABS_DIRTY = new AtomicBoolean(false);
    public static final Map<String, Set<String>> EXPANDED_TAGS_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> IN_CHEST_FILL = ThreadLocal.withInitial(() -> false);
    private static final Map<String, List<RemovalRule>> DYNAMIC_RULES = new ConcurrentHashMap<>();
    public static boolean MOD_INIT_PHASE = true;
    private static volatile HolderLookup.Provider REGISTRIES = null;

    public static void registerDynamicRules(String sourceId, List<RemovalRule> rules) {
        if (rules == null || rules.isEmpty()) {
            DYNAMIC_RULES.remove(sourceId);
        } else {
            DYNAMIC_RULES.put(sourceId, new ArrayList<>(rules));
        }
        load();
    }

    public static void unregisterDynamicRules(String sourceId) {
        if (DYNAMIC_RULES.remove(sourceId) != null) {
            load();
        }
    }

    public static boolean isInChestFill() {
        return IN_CHEST_FILL.get();
    }

    public static void setInChestFill(boolean value) {
        IN_CHEST_FILL.set(value);
    }

    public static void load() {
        RuleLoader.LoadedRules loaded = RuleLoader.load(DYNAMIC_RULES.values(), !MOD_INIT_PHASE);
        List<String> blacklist = loaded.blacklist();

        RULES_BY_ACTION = loaded.rulesByAction();
        rebuildIndex();
        GLOBALLY_BANNED_ITEMS = loaded.globallyBanned();
        if (blacklist != null) {
            GLOBALLY_BANNED_ITEMS.addAll(blacklist);
        }
        BLACKLISTED_ITEMS = blacklist != null ? List.copyOf(blacklist) : List.of();
        CNM_CASCADE_REMOVED = ConcurrentHashMap.newKeySet();

        int ruleCount = RULES_BY_ACTION.values().stream().mapToInt(List::size).sum() + GLOBALLY_BANNED_ITEMS.size();
        Constants.LOG.info("Loaded {} reliable remover rules.", ruleCount);

        RuleLoader.registerRecipeReplacements(RULES_BY_ACTION);

        if (CNM_CASCADE_RECOMPUTE_HOOK != null) CNM_CASCADE_RECOMPUTE_HOOK.run();
        RULES_GENERATION.incrementAndGet();

        if (Services.PLATFORM.isPhysicalClient()) {
            CREATIVE_TABS_DIRTY.set(true);
        }
    }

    public static void onSyncedRulesChanged() {
        reload();
        notifyRulesChanged();
    }

    public static void reload() {
        if (REGISTRIES != null) {
            expandTagRules(REGISTRIES);
        } else {
            load();
        }
    }

    public static void notifyRulesChanged() {
        for (Runnable hook : SYNCED_RULES_CHANGED_HOOKS) hook.run();
    }

    public static void registerSyncedRulesChanged(Runnable hook) {
        SYNCED_RULES_CHANGED_HOOKS.add(hook);
    }

    public static boolean consumeCreativeTabsDirty() {
        return CREATIVE_TABS_DIRTY.getAndSet(false);
    }

    private static void rebuildIndex() {
        Map<Action, RuleIndex> index = new EnumMap<>(Action.class);
        RULES_BY_ACTION.forEach((action, rules) -> index.put(action, new RuleIndex(rules)));
        RULE_INDEX = index;
    }

    public static Map<Action, List<RemovalRule>> getRulesByAction() {
        return RULES_BY_ACTION;
    }

    public static List<String> getBlacklistedItems() {
        return BLACKLISTED_ITEMS;
    }

    public static boolean isHidden(ItemStack stack) {
        return isHidden(stack, null, null, "item");
    }

    public static boolean isHidden(Item item) {
        return isItemRemoved(itemId(item), "item");
    }

    private static boolean isItemRemoved(String id, String context) {
        return isGloballyRemoved(id) || checkRules(null, id, Action.REMOVE, null, null, null, context);
    }

    public static boolean isHidden(ItemStack stack, String context) {
        return isHidden(stack, null, null, context);
    }

    public static boolean isHiddenFromRecipes(ItemStack stack, String context) {
        if (ReliableRecipesAPI.RECIPE_OUTPUT_CONTEXT.equals(context)) return isHidden(stack) || isRecipeBlocked(stack);
        return isHidden(stack, context);
    }

    public static boolean isHidden(ItemStack stack, Level level) {
        return isHidden(stack, level, null, "item");
    }

    public static boolean isHidden(ItemStack stack, Level level, Entity holder) {
        return isHidden(stack, level, holder, "item");
    }

    public static boolean isHidden(ItemStack stack, Level level, Entity holder, String context) {
        if (stack == null || stack.isEmpty()) return false;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.toString();

        if (isGloballyRemoved(id)) return true;

        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            if (checkRules(stack, id, Action.REMOVE, dimensionId(level), holder, null, context)) return true;
        }

        if (stack.is(Items.PAINTING) && isPaintingBlocked(stack)) return true;

        //? if >=1.21 {
        if (id.equals("minecraft:enchanted_book")) {
            ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                boolean allBlocked = true;
                for (var entry : enchantments.entrySet()) {
                    if (!isEnchantmentBlocked(stack, entry.getKey())) {
                        allBlocked = false;
                        break;
                    }
                }
                if (allBlocked) return true;
            }
        }

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            String dim = dimensionId(level);
            String potionId = contents.potion().flatMap(Holder::unwrapKey).map(key -> key.identifier().toString()).orElse(null);
            if (potionId != null && checkRules(null, potionId, Action.REMOVE_POTION, dim, holder, null, context))
                return true;
            for (MobEffectInstance effectInst : contents.getAllEffects()) {
                if (isPotionEffectBlocked(effectInst.getEffect(), level, holder, context)) return true;
            }
        }
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (tag == null) return false;

        if (id.equals("minecraft:enchanted_book")) {
            ListTag enchantments = tag.getList("StoredEnchantments", Tag.TAG_COMPOUND);
            if (!enchantments.isEmpty()) {
                boolean allBlocked = true;
                for (int i = 0; i < enchantments.size(); i++) {
                    if (!isEnchantmentBlocked(stack, EnchantmentRules.getEnchantmentHolder(enchantments.getCompound(i)))) {
                        allBlocked = false;
                        break;
                    }
                }
                if (allBlocked) return true;
            }
        }

        if (tag.contains("Potion") || tag.contains("CustomPotionEffects")) {
            String dim = dimensionId(level);
            Potion potion = PotionUtils.getPotion(stack);
            String potionId = potion == Potions.EMPTY ? null : BuiltInRegistries.POTION.getKey(potion).toString();
            if (potionId != null && checkRules(null, potionId, Action.REMOVE_POTION, dim, holder, null, context))
                return true;
            for (MobEffectInstance effectInst : PotionUtils.getMobEffects(stack)) {
                if (isPotionEffectBlocked(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effectInst.getEffect()), level, holder, context)) return true;
            }
        }
        *///?}
        return false;
    }

    private static boolean isPotionEffectBlocked(Holder<MobEffect> effectHolder, Level level, Entity holder, String context) {
        Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effectHolder.value());
        if (effectId != null && checkRules(null, effectId.toString(), Action.REMOVE_POTION, dimensionId(level), holder, effectHolder, context)) {
            return true;
        }
        return EffectRules.isEffectBlocked(effectHolder, level, holder);
    }

    //? if <1.21 {
    /*public static boolean isEnchantmentBlocked(ItemStack stack, Enchantment enchantment) {
        return isEnchantmentBlocked(stack, BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment));
    }
    *///?}

    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        return checkRules(stack, itemId(stack), Action.REMOVE_ATTACKS, dimensionId(level), target, null, "attack");
    }

    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        if (stack == null || stack.isEmpty()) return false;
        return checkRules(stack, itemId(stack), Action.REMOVE_INTERACTIONS, dimensionId(level), target, null, "interaction");
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state == null || state.isAir()) return false;
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return checkRules(null, id, Action.REMOVE_INTERACTIONS, dimensionId(level), entity, state.getBlock().builtInRegistryHolder(), "block_interaction");
    }

    public static boolean isBlockInteractionBlocked(BlockState state, Level level) {
        return isBlockInteractionBlocked(state, level, null, null);
    }

    public static boolean isPlacementBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty()) return false;
        return checkRules(stack, itemId(stack), Action.REMOVE_PLACEMENT, dimensionId(level), entity, null, "placement");
    }

    public static boolean isPlacementBlocked(ItemStack stack, Level level) {
        return isPlacementBlocked(stack, level, null);
    }

    public static boolean isTradeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromTrades) return false;
        return isBlockedUnlessReplaced(stack, Action.REMOVE_TRADE, null, null, "trade");
    }

    public static boolean isLootBlocked(ItemStack stack) {
        return isLootBlocked(stack, null);
    }

    public static boolean isLootBlocked(ItemStack stack, LootParams context) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromLootChests) return false;
        Level level = context != null ? context.getLevel() : null;
        String dim = dimensionId(level);
        Entity entity = getLootEntity(context);
        if (isHidden(stack, level, entity, "loot")) return true;
        if (getLootReplacement(stack, context) != null) return false;
        String id = itemId(stack);

        if (isInChestFill()) {
            if (checkRules(stack, id, Action.REMOVE_CHEST_LOOT, dim, entity, null, "chest_loot")) return true;
        }
        return checkRules(stack, id, Action.REMOVE_LOOT, dim, entity, null, "loot");
    }

    public static boolean isLootBlocked(Item item) {
        if (item == Items.AIR || !ModConfig.get().removeItemsFromLootChests) return false;
        String id = itemId(item);
        if (isItemRemoved(id, "loot")) return true;
        RemovalRule replacing = getLootRule(null, id, null, null);
        if (replacing != null && replacing.hasReplacement()) return false;

        if (isInChestFill()) {
            if (checkRules(null, id, Action.REMOVE_CHEST_LOOT, null, null, null, "chest_loot")) return true;
        }
        return checkRules(null, id, Action.REMOVE_LOOT, null, null, null, "loot");
    }

    private static Entity getLootEntity(LootParams context) {
        if (context == null) return null;
        //? if >=26.3 {
        /*return context.contextMap().get(LootContextParams.THIS_ENTITY);
         *///?} else if >=26.1 {
        return context.contextMap().getOptional(LootContextParams.THIS_ENTITY);
        //?} else {
        /*return context.getParamOrNull(LootContextParams.THIS_ENTITY);
         *///?}
    }

    public static boolean isInventoryBlocked(ItemStack stack) {
        return isInventoryBlocked(stack, null, null);
    }

    public static boolean isInventoryBlocked(ItemStack stack, Level level, Entity holder) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromInventories) return false;
        return isBlockedUnlessReplaced(stack, Action.REMOVE_INVENTORY, level, holder, "inventory");
    }

    public static boolean isCreativeBlocked(ItemStack stack) {
        return isCreativeBlocked(stack, null);
    }

    public static boolean isCreativeBlocked(ItemStack stack, Entity player) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromCreativeTabs) return false;
        return isBlockedUnlessReplaced(stack, Action.REMOVE_CREATIVE, null, player, "creative");
    }

    public static boolean isRecipeViewerBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (hasStackData(stack)) return computeRecipeViewerBlocked(stack);
        int generation = RULES_GENERATION.get();
        if (recipeViewerGeneration != generation) {
            RECIPE_VIEWER_BLOCKED.clear();
            recipeViewerGeneration = generation;
        }
        Boolean cached = RECIPE_VIEWER_BLOCKED.get(stack.getItem());
        if (cached != null) return cached;
        boolean blocked = computeRecipeViewerBlocked(stack);
        if (RULES_GENERATION.get() == generation) RECIPE_VIEWER_BLOCKED.put(stack.getItem(), blocked);
        return blocked;
    }

    private static boolean hasStackData(ItemStack stack) {
        //? if >=1.21 {
        return !stack.getComponentsPatch().isEmpty();
        //?} else {
        /*return stack.hasTag();
        *///?}
    }

    private static boolean computeRecipeViewerBlocked(ItemStack stack) {
        if (isHidden(stack)) return true;
        RemovalRule rule = getMatchingRule(stack, itemId(stack), Action.REMOVE_CREATIVE, null, null, null, "creative");
        return rule != null && rule.createReplacement(stack) == null;
    }

    public static boolean isDropsBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeDroppedItems) return false;
        return isBlockedUnlessReplaced(stack, Action.REMOVE_DROPS, level, entity, "drops");
    }

    public static boolean isEquipmentBlocked(ItemStack stack, Level level, Entity entity) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeMobEquipment) return false;
        return isBlockedUnlessReplaced(stack, Action.REMOVE_EQUIPMENT, level, entity, "equipment");
    }

    public static boolean isStorageBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromStorage) return false;
        if (getReplacement(stack, Action.REMOVE_STORAGE, level, null, "storage") != null) return false;
        if (isHidden(stack, level)) return true;
        return checkRules(stack, itemId(stack), Action.REMOVE_STORAGE, dimensionId(level), null, null, "storage");
    }

    public static boolean isHandSwingBlocked(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return false;
        return checkRules(stack, itemId(stack), Action.REMOVE_HAND_SWING, dimensionId(level), null, null, "swing");
    }

    public static boolean isInfoBlocked(ItemStack stack) {
        return isInfoBlocked(stack, null);
    }

    public static boolean isInfoBlocked(ItemStack stack, Entity player) {
        if (stack == null || stack.isEmpty()) return false;
        return checkRules(stack, itemId(stack), Action.REMOVE_INFO, null, player, null, "info");
    }

    public static boolean isRecipeBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeRecipes) return false;
        if (getReplacement(stack, Action.REMOVE_RECIPE, null, null, "recipe") != null) return false;
        if (isHidden(stack)) return true;
        return checkRules(stack, itemId(stack), Action.REMOVE_RECIPE, null, null, null, "recipe");
    }

    //? if >=26.1 {
    public static boolean isRecipeBlocked(Recipe<?> recipe) {
        if (!ModConfig.get().removeRecipes) return false;
        List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(recipe);
        if (outputs.isEmpty()) return false;
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !isRecipeBlocked(stack)) return false;
        }
        return true;
    }
    //?}

    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        return isEnchantmentBlocked(null, enchantment);
    }

    public static boolean isEnchantmentBlocked(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment == null) return false;
        String id = enchantment.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        return checkRules(stack, id, Action.REMOVE_ENCHANTMENT, null, null, enchantment, "enchantment");
    }

    public static boolean isPaintingBlocked(Holder<PaintingVariant> variant) {
        if (variant == null) return false;
        return variant.unwrapKey().map(key -> isPaintingBlocked(key.identifier(), variant)).orElse(false);
    }

    private static boolean isPaintingBlocked(Identifier id, Holder<PaintingVariant> variant) {
        return checkRules(null, id.toString(), Action.REMOVE_PAINTING, null, null, variant, "painting");
    }

    private static boolean isPaintingBlocked(ItemStack stack) {
        //? if >=26.1 {
        return isPaintingBlocked(stack.get(DataComponents.PAINTING_VARIANT));
        //?} else if >=1.21 {
        /*net.minecraft.world.item.component.CustomData entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData == null) return false;
        net.minecraft.nbt.CompoundTag tag = entityData.copyTag();
        Identifier id = tag.contains("variant") ? Identifier.tryParse(tag.getString("variant")) : null;
        if (id == null) return false;
        Holder<PaintingVariant> variant = REGISTRIES == null ? null : REGISTRIES.lookup(Registries.PAINTING_VARIANT)
                .flatMap(lookup -> lookup.get(net.minecraft.resources.ResourceKey.create(Registries.PAINTING_VARIANT, id)))
                .orElse(null);
        return isPaintingBlocked(id, variant);
        *///?} else {
        /*CompoundTag tag = stack.getTagElement("EntityTag");
        Identifier id = tag != null && tag.contains("variant") ? Identifier.tryParse(tag.getString("variant")) : null;
        if (id == null) return false;
        return isPaintingBlocked(id, RemovalRule.getRegistryHolder(BuiltInRegistries.PAINTING_VARIANT, id).orElse(null));
        *///?}
    }

    public static ItemStack getReplacement(ItemStack stack, Action action, Level level, Entity holder, String context) {
        if (stack == null || stack.isEmpty() || !isRemovalEnabled(action)) return null;
        String id = itemId(stack);
        String dim = dimensionId(level);
        RemovalRule rule = getMatchingRule(stack, id, action, dim, holder, null, context);
        if (rule == null && action != Action.REMOVE)
            rule = getMatchingRule(stack, id, Action.REMOVE, dim, holder, null, context);

        return rule != null ? rule.createReplacement(stack) : null;
    }

    private static boolean isRemovalEnabled(Action action) {
        ModConfig config = ModConfig.get();
        return switch (action) {
            case REMOVE_INVENTORY -> config.removeItemsFromInventories;
            case REMOVE_DROPS -> config.removeDroppedItems;
            case REMOVE_STORAGE -> config.removeItemsFromStorage;
            case REMOVE_TRADE -> config.removeItemsFromTrades;
            case REMOVE_CREATIVE -> config.removeItemsFromCreativeTabs;
            case REMOVE_RECIPE -> config.removeRecipes;
            case REMOVE_EQUIPMENT -> config.removeMobEquipment;
            default -> true;
        };
    }

    public static ItemStack getLootReplacement(ItemStack stack, LootParams context) {
        if (stack == null || stack.isEmpty() || !ModConfig.get().removeItemsFromLootChests) return null;
        String id = itemId(stack);
        String dim = dimensionId(context != null ? context.getLevel() : null);
        RemovalRule rule = getLootRule(stack, id, dim, getLootEntity(context));
        return rule != null ? rule.createReplacement(stack) : null;
    }

    private static RemovalRule getLootRule(ItemStack stack, String id, String dim, Entity entity) {
        RemovalRule rule = null;
        if (isInChestFill())
            rule = getMatchingRule(stack, id, Action.REMOVE_CHEST_LOOT, dim, entity, null, "chest_loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE_LOOT, dim, entity, null, "loot");
        if (rule == null) rule = getMatchingRule(stack, id, Action.REMOVE, dim, entity, null, "item");
        return rule;
    }

    private static boolean isBlockedUnlessReplaced(ItemStack stack, Action action, Level level, Entity holder, String context) {
        if (isHidden(stack, level, holder)) return true;
        if (getReplacement(stack, action, level, holder, context) != null) return false;
        return checkRules(stack, itemId(stack), action, dimensionId(level), holder, null, context);
    }

    static boolean checkRules(ItemStack stack, String itemId, Action action, String dimension, Entity target, Holder<?> registryHolder, String context) {
        return getMatchingRule(stack, itemId, action, dimension, target, registryHolder, context) != null;
    }

    static RemovalRule getMatchingRule(ItemStack stack, String itemId, Action action, String dimension, Entity target, Holder<?> registryHolder, String context) {
        RuleIndex index = RULE_INDEX.get(action);
        if (index == null) return null;
        List<RemovalRule> rules = index.candidates(itemId);
        if (rules.isEmpty()) return null;
        String entityId = target != null ? BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString() : null;
        for (RemovalRule rule : rules) {
            if (rule.action == action && rule.matches(stack, itemId, dimension, entityId, target, registryHolder, context))
                return rule;
        }
        return null;
    }

    static boolean isGloballyRemoved(String id) {
        return GLOBALLY_BANNED_ITEMS.contains(id) || CNM_CASCADE_REMOVED.contains(id);
    }

    static String dimensionId(Level level) {
        return level != null ? level.dimension().identifier().toString() : null;
    }

    private static String itemId(ItemStack stack) {
        return itemId(stack.getItem());
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static HolderLookup.Provider getRegistries() {
        return REGISTRIES;
    }

    public static void releaseRegistries() {
        REGISTRIES = null;
    }

    //? if <1.19.3 {
    /*public static void expandTagRules(net.minecraft.core.RegistryAccess registryAccess) {
        expandTagRules(HolderLookup.Provider.of(registryAccess));
    }

    *///?}
    public static void expandTagRules(HolderLookup.Provider registries) {
        REGISTRIES = registries;
        load();
        for (List<RemovalRule> rules : RULES_BY_ACTION.values()) {
            for (RemovalRule rule : rules) {
                rule.expandTags(registries);
            }
        }
        rebuildIndex();
        int totalExpandedItems = RULES_BY_ACTION.values().stream()
                .flatMap(List::stream)
                .mapToInt(rule -> rule.items != null ? rule.items.size() : 0)
                .sum();
        Constants.LOG.info("Total items removed: {}", totalExpandedItems + GLOBALLY_BANNED_ITEMS.size());

        if (CNM_CASCADE_RECOMPUTE_HOOK != null) CNM_CASCADE_RECOMPUTE_HOOK.run();
        RULES_GENERATION.incrementAndGet();
    }

    public static void registerCnmCascadeRecompute(Runnable hook) {
        CNM_CASCADE_RECOMPUTE_HOOK = hook;
    }

    public static void setCnmCascadeRemoved(Set<String> items) {
        CNM_CASCADE_REMOVED = items;
        RULES_GENERATION.incrementAndGet();
    }

    public static int getRulesGeneration() {
        return RULES_GENERATION.get();
    }

    public static boolean isFluidHidden(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return false;
        if (isGloballyRemoved(fluidId)) return true;
        return checkRules(null, fluidId, Action.REMOVE, null, null, null, "item");
    }
}
