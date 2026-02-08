package pocketutils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GemItem extends Item {

    public static Map<UUID, List<String>> areUsingAbility = new HashMap<>();
    private final Holder<MobEffect>  effect;
    private final int amplifier;
    public final String name;
    public int cooldown = 0;
    public GemItem(Properties settings, Holder<MobEffect> effect, int amplifier, String name, int cooldown) {
        super(settings);
        this.effect = effect;
        this.amplifier = amplifier;
        this.name = name;
        this.cooldown = cooldown;
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
                List<ParticleOptions> particleList = Arrays.asList(
                        ParticleTypes.FLAME,
                        ParticleTypes.SOUL_FIRE_FLAME,
                        new DustParticleOptions(0xff6339, 3.0f),
                        ParticleTypes.LAVA
                );
                spawnParticleRing(world, player, 3.0, 5, 100, 1, particleList);
                List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                );
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_DEATH, SoundSource.PLAYERS);
                for (LivingEntity e : nearbyList) {
                    if (e != player) {
                        circularKnockback(e, player, 2, 0.0, 2);
                        e.setRemainingFireTicks(200);
                    }
                }
            }
            if (name.equals("magic")){
                spawnSonicBoom(world, player, 4, 1);
            }
            if (name.equals("health")){
                List<ParticleOptions> particleList = List.of(
                        new DustParticleOptions(0xF81139, 3.0f)
                );
                spawnParticleRing(world, player, 3.0, 1, 100, 1, particleList);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS);

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
                world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS);
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
        if (e instanceof Player p) p.hurtMarked = true;
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
    public static void spawnSonicBoom(ServerLevel world, Player player, int range, int tickDelay){
        Vec3 pos =  player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        float accuracy = 1f;
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS);

        new TaskTicker(i -> {
            Vec3 point = pos.add(dir.scale(i * 2 * accuracy)); //3 affects the range it covers in the same time
            world.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0.0);

            List<LivingEntity> entities = world.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(point.x - 1, point.y - 1, point.z - 1,
                            point.x + 1, point.y + 1, point.z + 1)
            );

            for (LivingEntity e : entities) {
                if (e != player){
                    e.hurtServer(world, world.damageSources().sonicBoom(player), 10f);
                    e.push(dir.scale(2));
                }
            }
        }, tickDelay, (int) (range/accuracy));
    }
    public static void miningBeam(ServerLevel world, Player player, int range, int tickDelay) {
        Vec3 start = player.getBoundingBox().getCenter();
        Vec3 dir = player.getLookAngle().normalize();

        new TaskTicker(i -> {
            Vec3 point = start.add(dir.scale(i));
            double r = 0.5;
            AABB beamBox = new AABB(
                    point.x - r, point.y - r, point.z - r,
                    point.x + r, point.y + r, point.z + r
            );
            world.sendParticles(ParticleTypes.GUST, point.x, point.y, point.z, 1, 0, 0, 0, 0.0);

            BlockPos.betweenClosedStream(
                    BlockPos.containing(beamBox.minX, beamBox.minY, beamBox.minZ),
                    BlockPos.containing(beamBox.maxX, beamBox.maxY, beamBox.maxZ)
            ).forEach(pos -> {
                BlockState state = world.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(world, pos) < 0 || state.hasBlockEntity() || state.getBlock().getExplosionResistance() > 1000) return;
                world.destroyBlock(pos, true, player);
            });
        }, tickDelay, range);
    }
//    public static void spawnFangs(ServerLevel world, Player player, int rings, int tickDelay) {
//        if (!(player.level() instanceof ServerLevel)) return;
//
//        world.playSound(null, player.getX(), player.getY(), player.getZ(),
//                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
//
//        double direction = -Math.atan2(player.getHeadLookAngle().x, player.getHeadLookAngle().z) + Math.PI / 2;
//        Vec3 position = new Vec3(player.getX(), player.getY(), player.getZ());
//        float scope = 0.25f;
//        float density = 0.6f;
//
//        new TaskTicker(i -> {
//            for (int j = 0; j < i; j++) {
//                double t = (i == 1) ? 0.5 : (double) j / (i - 1);
//                double angle = direction + (t - 0.5) * scope;
//
//                EvokerFangs fang = EntityType.EVOKER_FANGS.create(world, EntitySpawnReason.TRIGGERED);
//                if (fang == null) return;
//                fang.setPos(position.x + i * density * Math.cos(angle), position.y, position.z + i * density * Math.sin(angle));
//                world.addFreshEntity(fang);
//            }
//        }, tickDelay, rings);
//    }
}
