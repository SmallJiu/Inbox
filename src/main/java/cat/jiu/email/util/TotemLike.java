package cat.jiu.email.util;

import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

public class TotemLike {
    static final HashMap<String, Boolean> TOTEM_LIKE = new HashMap<>();
    static {
        addTotemLike("voidtotem:totem_of_void_undying", true);
        addTotemLike("moretotems:explosive_totem_of_undying", true);
        addTotemLike("moretotems:teleportinge_totem_of_undying", true);
        addTotemLike("moretotems:stinging_totem_of_undying", true);
        addTotemLike("moretotems:ghastly_totem_of_undying", true);
        addTotemLike("totemicoverhaul:totem", true);
        addTotemLike("toimmortality:totem_of_immortality", false);
        addTotemLike("substitute_totem_for_death:substitute_totem", true);
        addTotemLike("realtotem:advanced_totem", true);
        addTotemLike("friendsandfoes:totem_of_illusion", true);
        addTotemLike("friendsandfoes:totem_of_freezing", true);
        addTotemLike("born_in_chaos_v1:death_totem", true);
        addTotemLike("enigmaticaddons:totem_of_malice", true);
        addTotemLike("biomemakeover:enchanted_totem", true);
        addTotemLike("avaritia:infinity_totem", false);
        addTotemLike("endless:infinity_totem", false);
        addTotemLike("artifacts:chorus_totem", true);
        addTotemLike("l2complements:totem_of_dream", true);
        addTotemLike("losttrinkets:broken_totem", true);
        addTotemLike("l2complements:totem_of_the_sea", true);
        addTotemLike("unusualend:void_totem", true);
        addTotemLike("endgoblintraders:durability_totem", true);
        addTotemLike("a_lot_of_respawn:bu_si_tu_teng_chu_ji_jia_qiang", true);
        addTotemLike("a_lot_of_respawn:bu_si_tu_teng_2_ji", true);
        addTotemLike("electrona:advanced_totem_of_undying", true);
        addTotemLike("chaos_world:totem_of_immortality", true);
        for (int i = 1; i < 17; i++) {
            addTotemLike("qiq2i_clga1:item_sp/totem_of_undying/type_"+i, true);
        }
        addTotemLike("game_of_reborn:totem_of_reborn", true);
    }

    public static void addTotemLike(String totemID, boolean canShrink) {
        if (!StringUtil.isNullOrEmpty(totemID)) {
            TOTEM_LIKE.put(totemID, canShrink);
        }
    }

    public static boolean isTotemLike(ItemStack stack) {
        return ModList.get().isLoaded("everything_totem") || stack.is(Items.TOTEM_OF_UNDYING) || TOTEM_LIKE.containsKey(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));
    }

    public static boolean canShrinkTotemLike(ItemStack stack) {
        if (ModList.get().isLoaded("everything_totem")) {
            return false;
        }
        if (stack.is(Items.TOTEM_OF_UNDYING)) {
            return true;
        }
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        return TOTEM_LIKE.containsKey(id) && TOTEM_LIKE.get(id);
    }

    public static void shrinkTotemLike(ItemStack stack){
        if (stack.isDamageableItem()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }else {
            stack.shrink(1);
        }
    }

    public static boolean checkAndShrinkTotemLike(ItemStack stack){
        if (isTotemLike(stack)) {
            if (canShrinkTotemLike(stack)) {
                shrinkTotemLike(stack);
            }
            return true;
        }
        return false;
    }
}
