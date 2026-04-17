package com.cipollomods.dontgotoofar;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DontGoTooFar.MOD_ID)
public class DontGoTooFar {

    public static final String MOD_ID = "dontgotoofar";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DontGoTooFar() {
        ZoneConfig.register();
        LOGGER.info("Don't Go Too Far cargado.");
    }
}