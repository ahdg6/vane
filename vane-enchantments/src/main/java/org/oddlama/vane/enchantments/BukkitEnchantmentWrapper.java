package org.oddlama.vane.enchantments;

import net.minecraft.server.v1_16_R3.Enchantment;

import org.bukkit.craftbukkit.v1_16_R3.enchantments.CraftEnchantment;

import org.jetbrains.annotations.NotNull;

public class BukkitEnchantmentWrapper extends CraftEnchantment {
	private CustomEnchantment<?> custom_enchantment;

	public BukkitEnchantmentWrapper(CustomEnchantment<?> custom_enchantment, Enchantment native_enchantment) {
		super(native_enchantment);
		this.custom_enchantment = custom_enchantment;
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@NotNull
	@Override
	public String getName() {
		return custom_enchantment.get_name();
	}

	public CustomEnchantment<?> custom_enchantment() {
		return custom_enchantment;
	}
}
