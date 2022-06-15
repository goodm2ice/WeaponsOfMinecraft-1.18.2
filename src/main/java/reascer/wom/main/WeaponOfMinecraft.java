package reascer.wom.main;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import reascer.wom.gameasset.EFAnimations;
import reascer.wom.gameasset.EFSkills;
import reascer.wom.world.item.EFECItems;
import yesman.epicfight.main.EpicFightMod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// The value here should match an entry in the META-INF/mods.toml file
@Mod("wom")
public class WeaponOfMinecraft
{
	public static final String MODID = "wom";
	public static final String CONFIG_FILE_PATH = WeaponOfMinecraft.MODID + ".toml";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
    public WeaponOfMinecraft()
    {
    	IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
    	bus.addListener(EFAnimations::registerAnimations);
    	bus.addListener(EFSkills::registerSkills);
    	
    	EFECItems.ITEMS.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
    }
}