package org.oddlama.vane.portals;

import java.util.HashMap;
import java.util.UUID;

import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class PortalTeleporter extends Listener<Portals> {
	private final HashMap<UUID, Location> entities_portalling = new HashMap<>();

	public PortalTeleporter(Context<Portals> context) {
		super(context);
	}

	private boolean cancel_portal_event(final Entity entity) {
		if (entities_portalling.containsKey(entity.getUniqueId())) {
			return true;
		}

		if (get_module().is_portal_block(entity.getLocation().getBlock())) {
			return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_portal(final PlayerPortalEvent event) {
		if (cancel_portal_event(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_entity_portal_event(final EntityPortalEvent event) {
		if (cancel_portal_event(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_teleport_event(final PlayerTeleportEndGatewayEvent event) {
		final var block = event.getGateway().getBlock();
		if (get_module().is_portal_block(block)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		final var player = event.getPlayer();
		final var player_id = player.getUniqueId();
		final var block = event.getTo().getBlock();

		if (!entities_portalling.containsKey(player_id)) {
			// Check if we walked into a portal
			if (!get_module().portal_area_materials.contains(block.getType())) {
				return;
			}

			final var portal = get_module().portal_for(block);
			if (portal == null) {
				return;
			}

			final var target = get_module().connected_portal(portal);
			if (target == null) {
				return;
			}

			// Put null to signal initiated teleportation
			entities_portalling.put(player_id, null);

			var target_location = target.spawn().clone();
			final var player_location = player.getLocation();
			target_location.setPitch(player_location.getPitch());
			target_location.setYaw(player_location.getYaw());

			// Calculate new pitch, yaw and velocity
			target_location = portal.orientation().apply(target.orientation(), target_location);
			final var new_velocity = portal.orientation().apply(target.orientation(), player.getVelocity());

			// Set new movement location
			event.setTo(target_location);

			// Retain velocity
			player.setVelocity(new_velocity);
		} else {
			final var loc = entities_portalling.get(player_id);
			if (loc == null) {
				// Initial teleport. Remember current location, so we can check
				// that the entity moved away far enough to allow another teleport
				entities_portalling.put(player_id, event.getFrom().clone());
			} else if (!get_module().portal_area_materials.contains(block.getType())) {
				// At least 2 blocks away and outside of portal area → finish portalling.
				if (event.getFrom().distance(loc) > 2.0) {
					entities_portalling.remove(player_id);
				}
			}
		}
	}
}
