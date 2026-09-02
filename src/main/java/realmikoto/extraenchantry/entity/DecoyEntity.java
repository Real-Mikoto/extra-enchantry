package realmikoto.extraenchantry.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import realmikoto.extraenchantry.DecoyManager;

import java.util.UUID;

/**
 * 假象（Decoy）诱饵实体：
 * 复用原版盔甲架实体类型（客户端自动渲染，无需注册渲染器），服务器侧扩展行为。
 * 外观复刻：头戴带主人档案的玩家头颅（显示其皮肤），盔甲与主副手物品原样复制。
 * 行为：无重力缓慢随机游走（离主人过远时朝其靠拢）、限时存在、可被攻击摧毁、
 * 生成与消失时散发淡蓝色全息粒子；阿西莫夫级联彩蛋会攻击主人与其他诱饵。
 * 通过 shouldBeSaved=false 永不持久化，避免重启后变成普通盔甲架。
 */
public class DecoyEntity extends ArmorStand {

	/** 淡蓝色全息粒子（RGB 0x66CCFF） */
	private static final DustParticleOptions HOLOGRAM_PARTICLE = new DustParticleOptions(0x66CCFF, 1.0F);

	/** 游走速度（格/tick，约 0.7 m/s，明显慢于步行） */
	private static final double WANDER_SPEED = 0.035;

	private int lifeTicks = 240;
	private int baseDurationTicks = 240;
	private float baseHealth = 8.0F;
	private int nestingDepth = 0;
	private boolean asimov = false;
	private boolean nestedRolled = false;
	private int wanderCooldown = 0;
	private double wanderX = 0.0;
	private double wanderZ = 0.0;
	private UUID ownerUuid;

	public DecoyEntity(EntityType<? extends ArmorStand> type, Level level) {
		super(type, level);
	}

	public DecoyEntity(Level level, double x, double y, double z) {
		super(level, x, y, z);
	}

	/** 由 DecoyManager 在生成后调用完成配置并复刻主人外观 */
	public void configure(Player owner, float health, int durationTicks, int nestingDepth, boolean asimov) {
		this.ownerUuid = owner.getUUID();
		this.lifeTicks = durationTicks;
		this.baseDurationTicks = durationTicks;
		this.baseHealth = health;
		this.nestingDepth = nestingDepth;
		this.asimov = asimov;
		this.setHealth(health);
		this.setYRot(owner.getYRot());
		this.setNoGravity(true);
		this.setShowArms(true);
		this.setNoBasePlate(true);
		this.setInvulnerable(false);

		// 复刻外观：头=玩家头颅（皮肤）、盔甲与手持物品原样复制
		for (EquipmentSlot slot : new EquipmentSlot[]{
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
				EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
			if (slot == EquipmentSlot.HEAD) {
				ItemStack head = new ItemStack(Items.PLAYER_HEAD);
				head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(owner.getGameProfile()));
				this.setItemSlot(EquipmentSlot.HEAD, head);
			} else {
				this.setItemSlot(slot, owner.getItemBySlot(slot).copy());
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide() || this.isRemoved()) {
			return;
		}
		if (--this.lifeTicks <= 0) {
			DecoyManager.destroyDecoy(this, false);
			return;
		}
		this.extraenchantry$wanderTick();
		// 阿西莫夫级联彩蛋：每秒攻击附近的主人与其他诱饵
		if (this.asimov && this.lifeTicks % 20 == 0) {
			this.extraenchantry$asimovAttack();
		}
	}

	/** 缓慢随机游走；离主人超过 8 格时朝主人方向游回 */
	private void extraenchantry$wanderTick() {
		if (--this.wanderCooldown <= 0) {
			this.wanderCooldown = 60 + this.getRandom().nextInt(80);
			double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
			this.wanderX = Math.cos(angle);
			this.wanderZ = Math.sin(angle);
		}

		double dx = this.wanderX;
		double dz = this.wanderZ;
		Player owner = this.level().getPlayerByUUID(this.ownerUuid);
		if (owner != null && this.distanceToSqr(owner) > 64.0) {
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len > 0.001) {
				dx = (owner.getX() - this.getX()) / len;
				dz = (owner.getZ() - this.getZ()) / len;
			}
		}

		this.move(MoverType.SELF, new Vec3(dx * WANDER_SPEED, 0.0, dz * WANDER_SPEED));
		this.setYRot((float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F);
	}

	/** 阿西莫夫级联：诱饵反过来攻击主人与其他诱饵 */
	private void extraenchantry$asimovAttack() {
		ServerLevel level = (ServerLevel) this.level();
		DamageSource source = this.damageSources().mobAttack(this);
		Player owner = level.getPlayerByUUID(this.ownerUuid);
		if (owner != null && this.distanceToSqr(owner) < 6.25) {
			owner.hurtServer(level, source, 2.0F);
		}
		for (DecoyEntity other : DecoyManager.getDecoysOf(this.ownerUuid)) {
			if (other != this && !other.isRemoved() && other.distanceToSqr(this) < 6.25) {
				other.hurtServer(level, source, 2.0F);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.isRemoved()) {
			return false;
		}
		this.setHealth(Math.max(0.0F, this.getHealth() - amount));

		if (this.getHealth() <= 0.0F) {
			DecoyManager.destroyDecoy(this, true);
		} else if (!this.nestedRolled && source.getEntity() != null) {
			// 嵌套彩蛋：首次受击时 5% 概率生成自己的（更弱的）诱饵
			this.nestedRolled = true;
			DecoyManager.tryNestedSpawn(this);
		}
		return true;
	}

	/** 生成/消失时的淡蓝色全息粒子 */
	public static void spawnHologramParticles(ServerLevel level, Vec3 pos) {
		level.sendParticles(HOLOGRAM_PARTICLE, pos.x, pos.y + 1.0, pos.z, 50, 0.35, 0.7, 0.35, 0.5);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.0, pos.z, 15, 0.35, 0.7, 0.35, 0.1);
	}

	/** 禁止玩家右键取放装备等交互 */
	@Override
	public InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand, Vec3 hitPos) {
		return InteractionResult.PASS;
	}

	/** 诱饵是临时实体，永不持久化到存档（避免重启后残留为普通盔甲架） */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	/** 不可被推动 */
	@Override
	public boolean isPushable() {
		return false;
	}

	public UUID getOwnerUuid() {
		return this.ownerUuid;
	}

	public int getNestingDepth() {
		return this.nestingDepth;
	}

	public float getBaseHealth() {
		return this.baseHealth;
	}

	public int getBaseDurationTicks() {
		return this.baseDurationTicks;
	}
}
