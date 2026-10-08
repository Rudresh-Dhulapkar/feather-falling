package com.rudresh.featherfalling;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FeatherFalling implements ModInitializer {

	public static final String MOD_ID = "feather-falling";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Map<UUID, List<Chicken>> chickens = new HashMap<>();
	private static final Map<UUID, Integer> featherTimers = new HashMap<>();
	private static final Map<UUID, Integer> chickenSpawnTimers = new HashMap<>();

	private static final int MAX_CHICKENS = 20;
	private static final int CHICKEN_SPAWN_INTERVAL = 200; // 20 means every tic, so 200 means every 10 seconds

	@Override
	public void onInitialize() {

		ServerTickEvents.END_SERVER_TICK.register(server -> {

			for (var player : server.getPlayerList().getPlayers()) {

				var boots = player.getItemBySlot(EquipmentSlot.FEET);
				var enchantments = boots.getEnchantments();

				var enchantmentRegistry = player.level()
						.registryAccess()
						.lookupOrThrow(Registries.ENCHANTMENT);

				var featherFalling = enchantmentRegistry.getOrThrow(
						Enchantments.FEATHER_FALLING
				);

				var level = enchantments.getLevel(featherFalling);

				if (level <= 0) {
					var playerChickens = chickens.remove(player.getUUID());

					if (playerChickens != null) {
						for (var chicken : playerChickens) {
							chicken.setNoGravity(false);
							chicken.setInvulnerable(false);
						}
					}

					featherTimers.remove(player.getUUID());
					chickenSpawnTimers.remove(player.getUUID());

					continue;
				}

				var playerChickens = chickens.computeIfAbsent(
						player.getUUID(),
						uuid -> new ArrayList<>()
				);

				// Spawn the first chicken immediately.
				if (level > 0 && playerChickens.isEmpty()) {

					var chicken = EntityTypes.CHICKEN.create(
							player.level(),
							EntitySpawnReason.TRIGGERED
					);

					if (chicken != null) {
						chicken.setNoGravity(true);
						chicken.setInvulnerable(true);
						chicken.setPos(
								player.getX(),
								player.getY() + 2.5,
								player.getZ()
						);
						chicken.setYRot(player.getYRot());

						player.level().addFreshEntity(chicken);
						playerChickens.add(chicken);
					}
				}

				// Timer for spawning additional chickens.
				int spawnTicks = chickenSpawnTimers.getOrDefault(
						player.getUUID(),
						0
				) + 1;

				if (level > 0
						&& playerChickens.size() < MAX_CHICKENS
						&& spawnTicks >= CHICKEN_SPAWN_INTERVAL) {

					spawnTicks = 0;

					var chicken = EntityTypes.CHICKEN.create(
							player.level(),
							EntitySpawnReason.TRIGGERED
					);

					if (chicken != null) {
						chicken.setNoGravity(true);
						chicken.setInvulnerable(true);
						chicken.setPos(
								player.getX(),
								player.getY() + 2.5,
								player.getZ()
						);
						chicken.setYRot(player.getYRot());

						player.level().addFreshEntity(chicken);
						playerChickens.add(chicken);
					}
				}

				chickenSpawnTimers.put(player.getUUID(), spawnTicks);

				// Move every chicken with the player.
				double angleStep = Math.toRadians(45);
				double radiusStep = 0.25;
				double heightStep = 0.12;

				for (int i = 0; i < playerChickens.size(); i++) {
					var chicken = playerChickens.get(i);

					double angle = i * angleStep;
					double radius = i * radiusStep;

					double x = player.getX() + Math.cos(angle) * radius;
					double y = player.getY() + 2.5 + i * heightStep;
					double z = player.getZ() + Math.sin(angle) * radius;

					chicken.setPos(x, y, z);
					chicken.setYRot(player.getYRot());
				}

				// Feather timer.
				if (!playerChickens.isEmpty()) {

					int ticks = featherTimers.getOrDefault(
							player.getUUID(),
							0
					) + 1;

					if (ticks >= 20) {
						ticks = 0;

						for (var chicken : playerChickens) {

							var feather = new ItemEntity(
									player.level(),
									chicken.getX(),
									chicken.getY() - 0.3,
									chicken.getZ(),
									new ItemStack(Items.FEATHER)
							);

							feather.setDeltaMovement(
									player.getLookAngle().scale(0.25)
							);

							player.level().addFreshEntity(feather);
						}
					}

					featherTimers.put(player.getUUID(), ticks);
				}
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}