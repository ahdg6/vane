package org.oddlama.vane.util;

import static org.oddlama.vane.util.Nms.creative_tab_id;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.player_handle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.destroystokyo.paper.profile.ProfileProperty;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import net.minecraft.server.v1_16_R3.Item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_16_R3.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemUtil {
	private static final UUID SKULL_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public static final UUID MODIFIER_UUID_GENERIC_ATTACK_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
	public static final UUID MODIFIER_UUID_GENERIC_ATTACK_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

	public static void damage_item(final Player player, ItemStack item_stack, int amount) {
		if (amount <= 0) {
			return;
		}

		final var handle = item_handle(item_stack);
		if (handle == null) {
			return;
		}

		handle.damage(amount, player_handle(player), x -> {});
	}

	public static String name_of(final ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return "";
		}
		final var meta = item.getItemMeta();
		if (!meta.hasDisplayName()) {
			return "";
		}
		return meta.getDisplayName();
	}

	public static ItemStack name_item(final ItemStack item, final BaseComponent name) {
		name.setItalic(false);
		return name_item(item, new BaseComponent[] { name }, (List<BaseComponent[]>)null);
	}

	public static ItemStack name_item(final ItemStack item, final BaseComponent name, final BaseComponent lore) {
		name.setItalic(false);
		lore.setItalic(false);
		return name_item(item, new BaseComponent[] { name }, Arrays.<BaseComponent[]>asList(new BaseComponent[] { lore }));
	}

	public static ItemStack name_item(final ItemStack item, final BaseComponent name, final List<BaseComponent> lore) {
		name.setItalic(false);
		final var list = lore.stream()
			.map(x -> {
				x.setItalic(false);
				return new BaseComponent[] { x };
			})
			.collect(Collectors.toList());
		return name_item(item, new BaseComponent[] { name }, list);
	}

	public static ItemStack name_item(final ItemStack item, final BaseComponent[] name, final List<BaseComponent[]> lore) {
		final var meta = item.getItemMeta();
		meta.setDisplayNameComponent(name);
		if (lore != null) {
			meta.setLoreComponents(lore);
		}
		item.setItemMeta(meta);
		return item;
	}

	public static int compare_enchantments(final ItemStack item_a, final ItemStack item_b) {
		var ae = item_a.getEnchantments();
		var be = item_b.getEnchantments();

		final var a_meta = item_a.getItemMeta();
		if (a_meta instanceof EnchantmentStorageMeta) {
			final var stored = ((EnchantmentStorageMeta)a_meta).getStoredEnchants();
			if (stored.size() > 0) {
				ae = stored;
			}
		}

		final var b_meta = item_b.getItemMeta();
		if (b_meta instanceof EnchantmentStorageMeta) {
			final var stored = ((EnchantmentStorageMeta)b_meta).getStoredEnchants();
			if (stored.size() > 0) {
				be = stored;
			}
		}

		// Unenchanted first
		final var a_count = ae.size();
		final var b_count = be.size();
		if (a_count == 0 && b_count == 0) {
			return 0;
		} else if (a_count == 0) {
			return -1;
		} else if (b_count == 0) {
			return 1;
		}

		// More enchantments before less enchantments
		if (a_count != b_count) {
			return b_count - a_count;
		}

		// Sort by combined rarity (rare = low value) first
		final var a_rarity = ae.keySet().stream().mapToInt(e -> CraftEnchantment.getRaw(e).d().a()).sum();
		final var b_rarity = be.keySet().stream().mapToInt(e -> CraftEnchantment.getRaw(e).d().a()).sum();
		if (a_rarity != b_rarity) {
			return b_rarity - a_rarity;
		}

		final var a_sorted = ae.entrySet().stream()
			.sorted(Map.Entry.<Enchantment, Integer>comparingByKey((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()))
				.thenComparing(Map.Entry.<Enchantment, Integer>comparingByValue()))
			.collect(Collectors.toList());
		final var b_sorted = be.entrySet().stream()
			.sorted(Map.Entry.<Enchantment, Integer>comparingByKey((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()))
				.thenComparing(Map.Entry.<Enchantment, Integer>comparingByValue()))
			.collect(Collectors.toList());

		// Lastly, compare names and levels
		final var ait = a_sorted.iterator();
		final var bit = b_sorted.iterator();

		while (ait.hasNext()) {
			final var a_el = ait.next();
			final var b_el = bit.next();

			// Lexicographic name comparison
			final var name_diff = a_el.getKey().getKey().toString().compareTo(b_el.getKey().getKey().toString());
			if (name_diff != 0) {
				return name_diff;
			}

			// Level
			int level_diff = b_el.getValue() - a_el.getValue();
			if (level_diff != 0) {
				return level_diff;
			}
		}

		return 0;
	}

	public static class ItemStackComparator implements Comparator<ItemStack> {
		@Override
		public int compare(final ItemStack a, final ItemStack b) {
			if (a == null && b == null) {
				return 0;
			} else if (a == null) {
				return 1;
			} else if (b == null) {
				return -1;
			}

			final var na = item_handle(a);
			final var nb = item_handle(b);
			if (na.isEmpty()) {
				return nb.isEmpty() ? 0 : 1;
			} else if (nb.isEmpty()) {
				return -1;
			}

			// By creative mode tab
			final var creative_mode_tab_diff = creative_tab_id(na.getItem()) - creative_tab_id(nb.getItem());
			if (creative_mode_tab_diff != 0) {
				return creative_mode_tab_diff;
			}

			// By id
			final var id_diff = Item.getId(na.getItem()) - Item.getId(nb.getItem());
			if (id_diff != 0) {
				return id_diff;
			}

			// By damage
			final var damage_diff = na.getDamage() - nb.getDamage();
			if (damage_diff != 0) {
				return damage_diff;
			}

			// By count
			final var count_diff = nb.getCount() - na.getCount();
			if (count_diff != 0) {
				return count_diff;
			}

			// By enchantments
			return compare_enchantments(a, b);
		}
	}

	public static ItemStack skull_for_player(final OfflinePlayer player) {
		final var item = new ItemStack(Material.PLAYER_HEAD);
		final var meta = (SkullMeta)item.getItemMeta();
		meta.setOwningPlayer(player);
		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack skull_with_texture(final String name, final String base64_texture) {
		final var profile = Bukkit.createProfile(SKULL_OWNER);
		profile.setProperty(new ProfileProperty("textures", base64_texture));

		final var item = new ItemStack(Material.PLAYER_HEAD);
		final var meta = (SkullMeta)item.getItemMeta();
		final var name_component = new TextComponent(name);
		name_component.setItalic(false);
		name_component.setColor(ChatColor.YELLOW);
		meta.setDisplayNameComponent(new BaseComponent[] { name_component });
		meta.setPlayerProfile(profile);
		item.setItemMeta(meta);
		return item;
	}
}
