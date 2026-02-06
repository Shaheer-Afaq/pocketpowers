package pocketutils;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GemItem extends Item {

    public static Map<UUID, List<String>> areUsingAbility = new HashMap<>();
    private final Holder<MobEffect>  effect;
    private final int amplifier;
    private final String name;
    public int cooldown = 0;
    public GemItem(Properties settings, Holder<MobEffect> effect, int amplifier, String name, int cooldown_) {
        super(settings);
        this.effect = effect;
        this.amplifier = amplifier;
        this.name = name;
        this.cooldown = cooldown_;
    }
    {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (source.is(DamageTypes.FALL) && (entity instanceof Player player) && isGemActive(player, "gravity")!=null && player.level() instanceof ServerLevel world) {
                world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS,1.0F,1.0F);
                world.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);
                world.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);

                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                );
                for (LivingEntity e : nearbyList) {
                    float smashDamage = (amount * 0.7f < 100)? amount * 0.7f : 100;
                    if (e != player){
                        e.hurtServer(world, new DamageSource(world.registryAccess().get(DamageTypes.MACE_SMASH).get()), smashDamage);
                        circularKnockback(e, player, smashDamage*0.04, smashDamage*0.02, smashDamage*0.04);
                    }
                }
                return false;
            }

            if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MACE_SMASH)) && source.getEntity() instanceof Player attacker) {
                final Random RANDOM = new Random();

                if (entity.level() instanceof ServerLevel world){
                    if (RANDOM.nextInt(100) < 20 && isGemActive(attacker, "magic") != null && amount >= 10) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.setPos(entity.getX(), entity.getY(), entity.getZ());
                            world.addFreshEntity(lightning);
                        }
                    }
                    else if(RANDOM.nextInt(100) < 40 && isGemActive(attacker, "health")!=null) {
                        Objects.requireNonNull(source.getEntity().asLivingEntity()).heal((float) (amount*0.2));
                        world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,1.0F,1.0F);
                    }
                }
            }

            if ((source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.THROWN)) || source.is(DamageTypes.MAGIC) && entity instanceof Player player && isGemActive(player, "magic")!=null) {
                return false;
            }

            return true;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            ItemStack stack = player.getMainHandItem();
            ItemStack stackOff = player.getOffhandItem();
            if (player.isShiftKeyDown()){
                 if (stackOff.getItem() instanceof GemItem itemOff && !player.getCooldowns().isOnCooldown(stackOff)) {
                     player.getCooldowns().addCooldown(stackOff, cooldown);
                     System.out.println(cooldown);
                     List<String> abilities = areUsingAbility.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
                     abilities.add(itemOff.name);
                     System.out.println("ranOff");
                     return InteractionResult.FAIL;
                 }

            }else if (stack.getItem() instanceof GemItem item && !player.getCooldowns().isOnCooldown(stack)){
                player.getCooldowns().addCooldown(stack, cooldown);
                System.out.println(cooldown);
                List<String> abilities = areUsingAbility.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
                abilities.add(item.name);
                System.out.println("ran");
                return InteractionResult.FAIL;
            }
            return  InteractionResult.PASS;
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (world.isClientSide()) return;
        if (!(entity instanceof Player player)) return;


        if (isGemActive(player, name)!=null) {
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, 10, amplifier, false, true));
            }
        }
        List<String> abilities = areUsingAbility.get(player.getUUID());
        if (abilities != null && abilities.contains(name)) {
            if (name.equals("fire")){
//                player.getCooldowns().addCooldown(stack, cooldown);
                List<ParticleOptions> particleList = Arrays.asList(
                        ParticleTypes.FLAME,
                        ParticleTypes.SOUL_FIRE_FLAME,
                        new DustParticleOptions(0xff6339, 3.0f)
//                        ParticleTypes.SMOKE,
//                        ParticleTypes.LAVA,
//                        ParticleTypes.CAMPFIRE_COSY_SMOKE
                );
                spawnParticleRing(world, player, 3.0, 5, 100, 1, particleList);
                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                );
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
                for (LivingEntity e : nearbyList) {
                    if (e != player) {
                        circularKnockback(e, player, 2, 0.0, 2);
                        e.setRemainingFireTicks(200);
                    }
                }
            }
            if (name.equals("magic")){
//                player.getCooldowns().addCooldown(stack, cooldown);
//                spawnFangs(world, player, 10, 1);
                spawnSonicBoom(world, player, 4, 1);
            }
            if (name.equals("health")){
//                player.getCooldowns().addCooldown(stack, cooldown);
                List<ParticleOptions> particleList = List.of(
                        new DustParticleOptions(0xF81139, 3.0f)
                );
                spawnParticleRing(world, player, 3.0, 1, 100, 1, particleList);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0F, 1.0F);

                player.heal(10);
                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 0.5, 3.0)
                );
                for (LivingEntity e : nearbyList) {
                    if (e != player){
                        e.heal(6);
                        world.sendParticles(ParticleTypes.HEART, e.getX(), e.getBoundingBox().maxY + 0.2, e.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }
            if (name.equals("gravity")){
                world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS,1.0F,1.0F);
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.x, 3.0, vel.z);
                player.hurtMarked = true;
            }
            if (name.equals("mining")){
                miningBeam(world, player, 10, 1);
            }
            areUsingAbility.get(player.getUUID()).remove(name);
        }
    }

    public static ItemStack isGemActive(Player player, String effectId) {
        if (isGem(player.getMainHandItem(), effectId)) return player.getMainHandItem();
        if (isGem(player.getOffhandItem(), effectId)) return player.getOffhandItem();
        return null;
    }
    public static boolean isGem(ItemStack stack, String effectId) {
        return stack.getItem() instanceof GemItem gem && effectId.equals(gem.name);
    }
    public static void circularKnockback(LivingEntity e, Player player, double ax, double ay, double az){
        double dx = e.getX() - player.getX();
        double dz = e.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        dx /= dist;
        dz /= dist;
        e.push(dx * ax, ay, dz * az);
    }
    public static void spawnParticleRing(ServerLevel world, Player player, double maxRadius, int steps, int points, int tickDelay, List<ParticleOptions> particles) {
        final Random rand = new Random();

        double centerX = player.getX();
        double centerY = player.getY() + 0.5;
        double centerZ = player.getZ();

        new TaskTicker(t -> {
//            double alpha =  (double) t / steps;
            double radius = maxRadius * t / steps;
            int pointsThisTick = Math.max(4, (int)(radius * points * 0.6));

            for (int i = 0 ; i < pointsThisTick; i++) {
                double angle = 2 * Math.PI * ((double) i / pointsThisTick);
                double x = centerX + radius * Math.cos(angle) + ((rand.nextDouble() - 0.5) * 0.8);
                double z = centerZ + radius * Math.sin(angle) + ((rand.nextDouble() - 0.5) * 0.8);
                double y = centerY + (rand.nextDouble() - 0.5) * 0.2;

                for (ParticleOptions particle : particles) {
                    world.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0.0);
                }
            }
        }, tickDelay, steps);
    }
    public static void spawnFangs(ServerLevel world, Player player, int rings, int tickDelay) {
        if (!(player.level() instanceof ServerLevel)) return;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);

        double direction = -Math.atan2(player.getHeadLookAngle().x, player.getHeadLookAngle().z) + Math.PI / 2;
        Vec3 position = new Vec3(player.getX(), player.getY(), player.getZ());
        float scope = 0.25f;
        float density = 0.6f;

        new TaskTicker(i -> {
            for (int j = 0; j < i; j++) {
                double t = (i == 1) ? 0.5 : (double) j / (i - 1);
                double angle = direction + (t - 0.5) * scope;

                EvokerFangs fang = EntityType.EVOKER_FANGS.create(world, EntitySpawnReason.TRIGGERED);
                if (fang == null) return;
                fang.setPos(position.x + i * density * Math.cos(angle), position.y, position.z + i * density * Math.sin(angle));
                world.addFreshEntity(fang);
            }
        }, tickDelay, rings);
    }
    public static void spawnSonicBoom(ServerLevel world, Player player, int range, int tickDelay){
        Vec3 pos =  player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        float accuracy = 0.5f;
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5F, 1.0F);

        new TaskTicker(i -> {
            Vec3 point = pos.add(dir.scale(i* 3 * accuracy)); //3 affects the range it covers in the same time
            world.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0.0);

            List<LivingEntity> entities = world.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(point.x - 1, point.y - 1, point.z - 1,
                            point.x + 1, point.y + 1, point.z + 1)
            );
            DamageSource source = new DamageSource(world.registryAccess().get(DamageTypes.SONIC_BOOM).get());

            for (LivingEntity e : entities) {
                if (e != player){
                    e.hurtServer(world, source, 50f);
                    e.push(dir.scale(2));
                }
            }
        }, tickDelay, (int) (range/accuracy));
    }
    public static void miningBeam(ServerLevel world, Player player, int range, int tickDelay) {
        Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ()).add(player.getEyePosition()).scale(0.5); //gets the position between eyes and feet
        Vec3 dir = player.getLookAngle().normalize();

        new TaskTicker(i -> {
            Vec3 point = pos.add(dir.scale(i));
            AABB beamBox = new AABB(point.x - 0.5, point.y - 0.5, point.z - 0.5,
                                    point.x + 0.5, point.y + 0.5, point.z + 0.5);

            world.sendParticles(ParticleTypes.GUST, point.x, point.y, point.z, 1, 0, 0, 0, 0.0);
            BlockPos.betweenClosedStream(
                    new BlockPos((int) beamBox.minX, (int) beamBox.minY, (int) beamBox.minZ),
                    new BlockPos((int) beamBox.maxX, (int) beamBox.maxY, (int) beamBox.maxZ)
            ).forEach(blockPos -> {
                BlockState state = world.getBlockState(blockPos);
                if(state.isAir() || state.getDestroySpeed(world, blockPos) < 0 || state.getBlock().getExplosionResistance() > 1000 || state.hasBlockEntity()) return;
                world.destroyBlock(blockPos, true, player);
            });
        }, tickDelay, range);

    }
}