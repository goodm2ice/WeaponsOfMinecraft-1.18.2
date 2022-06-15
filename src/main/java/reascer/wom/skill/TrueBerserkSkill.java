package reascer.wom.skill;

import java.util.List;
import java.util.UUID;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SpecialAttackSkill;
import yesman.epicfight.skill.SkillDataManager.SkillDataKey;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.effect.EpicFightMobEffects;

public class TrueBerserkSkill extends SpecialAttackSkill {
	private static final SkillDataKey<Integer> TIMER = SkillDataKey.createDataKey(SkillDataManager.ValueType.INTEGER);
	private static final SkillDataKey<Boolean> ACTIVE = SkillDataKey.createDataKey(SkillDataManager.ValueType.BOOLEAN);

	protected final StaticAnimation activateAnimation;
	
	public TrueBerserkSkill(Builder builder) {
		super(builder);

		this.activateAnimation = builder.activateAnimation;
	}
	
	public static class Builder extends Skill.Builder<AgonyPlungeSkill> {
		
		protected StaticAnimation activateAnimation;
		
		public Builder(ResourceLocation resourceLocation) {
			super(resourceLocation);
		}
		
		public Builder setCategory(SkillCategories category) {
			this.category = category;
			return this;
		}
		
		public Builder setConsumption(float consumption) {
			this.consumption = consumption;
			return this;
		}
		
		public Builder setMaxDuration(int maxDuration) {
			this.maxDuration = maxDuration;
			return this;
		}
		
		public Builder setMaxStack(int maxStack) {
			this.maxStack = maxStack;
			return this;
		}
		
		public Builder setRequiredXp(int requiredXp) {
			this.requiredXp = requiredXp;
			return this;
		}
		
		public Builder setActivateType(ActivateType activateType) {
			this.activateType = activateType;
			return this;
		}
		
		public Builder setResource(Resource resource) {
			this.resource = resource;
			return this;
		}
		
		public Builder setAnimations(StaticAnimation activateAnimation) {
			this.activateAnimation = activateAnimation;
			return this;
		}
	}
	
	public static Builder createBuilder(ResourceLocation resourceLocation) {
		return (new Builder(resourceLocation)).setCategory(SkillCategories.WEAPON_SPECIAL_ATTACK).setResource(Resource.SPECIAL_GAUAGE);
	}
	
	@Override
	public void onInitiate(SkillContainer container) {
		container.getDataManager().registerData(TIMER);
		container.getDataManager().registerData(ACTIVE);
		
		container.getDataManager().setData(TIMER, 12);
		container.getDataManager().setData(ACTIVE,false);
		
		if (!container.getExecuter().isLogicalClient()) {
			this.setMaxDurationSynchronize((ServerPlayerPatch)container.getExecuter(), this.maxDuration + EnchantmentHelper.getEnchantmentLevel(Enchantments.SWEEPING_EDGE, container.getExecuter().getOriginal()));
		}
		
	}
	
	@Override
	public void onRemoved(SkillContainer container) {
		if(!container.getExecuter().isLogicalClient()) {
			container.getSkill().cancelOnServer((ServerPlayerPatch)container.getExecuter(), null);
		}
		container.deactivate();
	}
	
	@Override
	public void executeOnServer(ServerPlayerPatch executer, FriendlyByteBuf args) {
		if (executer.getSkill(this.category).isActivated()) { 
			super.cancelOnServer(executer, args);
			executer.getOriginal().removeEffect(EpicFightMobEffects.STUN_IMMUNITY.get());
			executer.getSkill(this.category).getDataManager().setData(TIMER, 12);
			executer.getSkill(this.category).getDataManager().setDataSync(ACTIVE, false,executer.getOriginal());
			this.setDurationSynchronize(executer, 0);
			this.setStackSynchronize(executer, executer.getSkill(this.category).getStack() - 1);
			executer.getSkill(this.category).deactivate();
			executer.modifyLivingMotionByCurrentItem();
		} else {
			executer.playAnimationSynchronized(this.activateAnimation, 0);
			executer.getOriginal().level.playSound(null, executer.getOriginal().xo, executer.getOriginal().yo, executer.getOriginal().zo,
	    			SoundEvents.DRAGON_FIREBALL_EXPLODE, executer.getOriginal().getSoundSource(), 1.0F, 1.0F);
			((ServerLevel) executer.getOriginal().level).sendParticles( ParticleTypes.LARGE_SMOKE, 
					executer.getOriginal().getX() - 0.15D, 
					executer.getOriginal().getY() + 1.2D, 
					executer.getOriginal().getZ() - 0.15D, 
					150, 0.3D, 0.6D, 0.3D, 0.1D);
			executer.getOriginal().addEffect(new MobEffectInstance(EpicFightMobEffects.STUN_IMMUNITY.get(), 120000));
			executer.getSkill(this.category).getDataManager().setData(TIMER, 12);
			executer.getSkill(this.category).getDataManager().setDataSync(ACTIVE, true,executer.getOriginal());
			this.setDurationSynchronize(executer, this.maxDuration + EnchantmentHelper.getEnchantmentLevel(Enchantments.SWEEPING_EDGE, executer.getOriginal()));
			executer.getSkill(this.category).activate();
			executer.modifyLivingMotionByCurrentItem();
		}
	}
	
	@Override
	public void cancelOnServer(ServerPlayerPatch executer, FriendlyByteBuf args) {
		super.cancelOnServer(executer, args);
		executer.getOriginal().removeEffect(EpicFightMobEffects.STUN_IMMUNITY.get());
		executer.getSkill(this.category).getDataManager().setData(TIMER, 12);
		executer.getSkill(this.category).getDataManager().setDataSync(ACTIVE, false,executer.getOriginal());
		this.setStackSynchronize(executer, executer.getSkill(this.category).getStack() - 1);
		executer.modifyLivingMotionByCurrentItem();
	}
	
	@Override
	public boolean canExecute(PlayerPatch<?> executer) {
		if (executer.isLogicalClient()) {
			return super.canExecute(executer);
		} else {
			return executer.getHoldingItemCapability(InteractionHand.MAIN_HAND).getSpecialAttack(executer) == this && executer.getOriginal().getVehicle() == null;
		}
	}
	
	@Override
	public SpecialAttackSkill registerPropertiesToAnimation() {
		return this;
	}
	
	@Override
	public List<Component> getTooltipOnItem(ItemStack itemStack, CapabilityItem cap, PlayerPatch<?> playerCap) {
		List<Component> list = super.getTooltipOnItem(itemStack, cap, playerCap);
		this.generateTooltipforPhase(list, itemStack, cap, playerCap, this.properties.get(1), "Auto attack :");
		this.generateTooltipforPhase(list, itemStack, cap, playerCap, this.properties.get(2), "Dash attack :");
		
		return list;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onScreen(LocalPlayerPatch playerpatch, float resolutionX, float resolutionY) {
		if (playerpatch.getSkill(this.category).isActivated()) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, new ResourceLocation(EpicFightMod.MODID, "textures/gui/overlay/true_berserk.png"));
			GlStateManager._enableBlend();
			GlStateManager._disableDepthTest();
			GlStateManager._blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			Tesselator tessellator = Tesselator.getInstance();
		    BufferBuilder bufferbuilder = tessellator.getBuilder();
		    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		    bufferbuilder.vertex(0, 0, 1).uv(0, 0).endVertex();
		    bufferbuilder.vertex(0, resolutionY, 1).uv(0, 1).endVertex();
		    bufferbuilder.vertex(resolutionX, resolutionY, 1).uv(1, 1).endVertex();
		    bufferbuilder.vertex(resolutionX, 0, 1).uv(1, 0).endVertex();
		    tessellator.end();
		}
	}
	
	@Override
	public void updateContainer(SkillContainer container) {
		if (container.isActivated()) {
			if (container.getDataManager().getDataValue(ACTIVE)) {
				if (container.getDataManager().getDataValue(TIMER) > 0) {
					container.getDataManager().setData(TIMER, container.getDataManager().getDataValue(TIMER)-1);
				} else {
					container.getDataManager().setData(TIMER,12);
					if (container.getRemainDuration() > 1) {
						if(!container.getExecuter().isLogicalClient()) {
							this.setDurationSynchronize((ServerPlayerPatch) container.getExecuter(), container.getRemainDuration()-1);
						}
					} else {
						if (container.getExecuter().getOriginal().getHealth() > 1f) {
							container.getExecuter().getOriginal().setHealth(container.getExecuter().getOriginal().getHealth() - 1f);
							if(!container.getExecuter().isLogicalClient()) {
								container.getExecuter().getOriginal().level.playSound(null, container.getExecuter().getOriginal().xo, container.getExecuter().getOriginal().yo, container.getExecuter().getOriginal().zo,
						    			SoundEvents.PLAYER_HURT, container.getExecuter().getOriginal().getSoundSource(), 1.0F, 1.0F);
								((ServerLevel) container.getExecuter().getOriginal().level).sendParticles( ParticleTypes.SMOKE, 
										container.getExecuter().getOriginal().getX() - 0.2D, 
										container.getExecuter().getOriginal().getY() + 1.3D, 
										container.getExecuter().getOriginal().getZ() - 0.2D, 
										40, 0.6D, 0.8D, 0.6D, 0.05);
								this.setDurationSynchronize((ServerPlayerPatch) container.getExecuter(),this.maxDuration + EnchantmentHelper.getEnchantmentLevel(Enchantments.SWEEPING_EDGE, container.getExecuter().getOriginal()));
							}
						} else {
							if(!container.getExecuter().isLogicalClient()) {
								container.getSkill().cancelOnServer((ServerPlayerPatch)container.getExecuter(), null);
								this.setDurationSynchronize((ServerPlayerPatch) container.getExecuter(), 0);
							}
							container.deactivate();
						}
					}
				}
			} else {
				if(!container.getExecuter().isLogicalClient()) {
					container.getSkill().cancelOnServer((ServerPlayerPatch)container.getExecuter(), null);
				}
			}
		}
	}
}