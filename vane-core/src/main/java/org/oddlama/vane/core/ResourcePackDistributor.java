package org.oddlama.vane.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;

public class ResourcePackDistributor extends Listener<Core> {
	@ConfigString(def = "https://your-server.tld/path/to/pack.zip", desc = "URL to an resource pack. Will request players to use the specified resource pack. [as of 1.16.2] Beware that the minecraft client currently has issues with webservers that serve resource packs via https and don't allow ssl3. This protocol is considered insecure and therefore should NOT be used. To workaround this issue, you should host the file in a http context. Using http is not a security issue, as the file will be verified via its sha1 sum by the client.")
	public String config_url;
	@ConfigString(def = "", desc = "Resource pack SHA-1 sum. Required to verify resource pack integrity.")
	public String config_sha1;
	@ConfigBoolean(def = true, desc = "Kick players if they deny to use the specified resource pack (if set). Individual players can be exempt from this rule by giving them the permission 'vane.core.resource_pack.bypass'.")
	public boolean config_force;

	@LangMessage public TranslatedMessage lang_declined;
	@LangMessage public TranslatedMessage lang_download_failed;

	// The permission to bypass the resource pack
	public final Permission bypass_permission;
	public PlayerMessageDelayer player_message_delayer;

	public ResourcePackDistributor(Context<Core> context) {
		super(context.group("resource_pack", "Enable resource pack distribution."));

		// Delay messages if this the distributor is active.
		player_message_delayer = new PlayerMessageDelayer(get_context());

		// Register bypass permission
		bypass_permission = new Permission("vane." + get_module().get_name() + ".resource_pack.bypass", "Allows bypassing an enforced resource pack", PermissionDefault.FALSE);
		get_module().register_permission(bypass_permission);
	}

	@Override
	public void on_enable() {
		// Check sha1 sum validity
		if (config_sha1.length() != 40) {
			get_module().log.warning("Invalid resource pack SHA-1 sum '" + config_sha1 + "', should be 40 characters long but has " + config_sha1.length());
			get_module().log.warning("Disabling resource pack serving and message delaying");

			// Disable resource pack
			config_url = "";
			// Prevent subcontexts from being enabling
			// FIXME this can be coded more cleanly. We need a way
			// to process config changes _before_ the module is enabled.
			// like on_config_change_pre_enable(), where we can override
			// the context group enable state.
			((ModuleGroup<Core>)player_message_delayer.get_context()).config_enabled = false;
		}

		// Propagate enable after setting our config_url, so the
		// message delayer can observe the result
		super.on_enable();

		config_sha1 = config_sha1.toLowerCase();
		if (!config_url.isEmpty()) {
			get_module().log.info("Distributing resource pack from '" + config_url + "' with sha1 " + config_sha1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		if (config_url.isEmpty()) {
			return;
		}

		event.getPlayer().setResourcePack(config_url, config_sha1);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void on_player_status(final PlayerResourcePackStatusEvent event) {
		if (!config_force || event.getPlayer().hasPermission(bypass_permission)) {
			return;
		}

		switch (event.getStatus()) {
			case DECLINED:
				event.getPlayer().kickPlayer(lang_declined.str());
				break;

			case FAILED_DOWNLOAD:
				event.getPlayer().kickPlayer(lang_download_failed.str());
				break;
		}
	}
}
