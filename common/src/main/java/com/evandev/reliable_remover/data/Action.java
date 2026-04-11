package com.evandev.reliable_remover.data;

import com.google.gson.annotations.SerializedName;

public enum Action {
    @SerializedName(value = "REMOVE", alternate = {"remove", "delete", "Remove"})
    REMOVE,
    @SerializedName(value = "REMOVE_ATTACKS", alternate = {"remove_attacks", "remove_attack", "Remove_Attacks"})
    REMOVE_ATTACKS,
    @SerializedName(value = "REMOVE_INTERACTIONS", alternate = {"remove_interactions", "remove_interaction", "Remove_Interactions"})
    REMOVE_INTERACTIONS,
    @SerializedName(value = "REMOVE_ENCHANTMENT", alternate = {"remove_enchantment", "remove_enchantments", "Remove_Enchantment"})
    REMOVE_ENCHANTMENT,
    @SerializedName(value = "REMOVE_POTION", alternate = {"remove_potion", "remove_potions", "Remove_Potion"})
    REMOVE_POTION,
    @SerializedName(value = "REMOVE_TRADE", alternate = {"remove_trade", "remove_trades", "Remove_Trade"})
    REMOVE_TRADE,
    @SerializedName(value = "REMOVE_LOOT", alternate = {"remove_loot", "remove_loot_tables", "remove_loot_table", "remove_loots", "Remove_Loot"})
    REMOVE_LOOT,
    @SerializedName(value = "REMOVE_HAND_SWING", alternate = {"remove_swing", "remove_swings", "remove_hand_swings", "remove_hand_swing", "Remove_Hand_Swing"})
    REMOVE_HAND_SWING,
    @SerializedName(value = "REMOVE_INFO", alternate = {"remove_info", "remove_information", "Remove_Info"})
    REMOVE_INFO,
    @SerializedName(value = "REMOVE_DROPS", alternate = {"remove_drops", "remove_drop", "Remove_Drops"})
    REMOVE_DROPS,
    @SerializedName(value = "REMOVE_INVENTORY", alternate = {"remove_inventory", "remove_inventories", "Remove_Inventory"})
    REMOVE_INVENTORY,
    @SerializedName(value = "REMOVE_CREATIVE", alternate = {"remove_creative", "remove_creatives", "Remove_Creative"})
    REMOVE_CREATIVE,
    @SerializedName(value = "REMOVE_EQUIPMENT", alternate = {"remove_equipment", "remove_mob_equipment", "Remove_Equipment"})
    REMOVE_EQUIPMENT,
    @SerializedName(value = "REMOVE_RECIPE", alternate = {"remove_recipe", "remove_recipes", "Remove_Recipe"})
    REMOVE_RECIPE,
    @SerializedName(value = "REMOVE_STORAGE", alternate = {"remove_storage", "remove_storages", "Remove_Storage"})
    REMOVE_STORAGE,
    @SerializedName(value = "REMOVE_CHEST_LOOT", alternate = {"remove_chest_loot", "remove_container_loot", "Remove_Chest_Loot"})
    REMOVE_CHEST_LOOT
}
