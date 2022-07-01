package ca.fxco.fabricatedchatreports;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class FabricatedChatReports implements ClientModInitializer {

    public static Logger LOGGER = LoggerFactory.getLogger("fabricated_chat_reports");

    public static boolean DEMO_MODE = true;

    @Override
    public void onInitializeClient() {}
}
