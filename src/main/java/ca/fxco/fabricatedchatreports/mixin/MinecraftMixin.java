package ca.fxco.fabricatedchatreports.mixin;

import ca.fxco.fabricatedchatreports.helpers.FabricatedAbuseReportSender;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.RollingMemoryChatLog;
import net.minecraft.client.multiplayer.chat.report.AbuseReportSender;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    private static AbuseReportSender createSender(ReportEnvironment reportEnvironment, UserApiService userApiService) {
        return new FabricatedAbuseReportSender(reportEnvironment, userApiService);
    }

    private static ReportingContext create(ReportEnvironment reportEnvironment, UserApiService userApiService) {
        RollingMemoryChatLog rollingMemoryChatLog = new RollingMemoryChatLog(1024);
        AbuseReportSender abuseReportSender = createSender(reportEnvironment, userApiService);
        return new ReportingContext(abuseReportSender, reportEnvironment, rollingMemoryChatLog);
    }


    @Redirect(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/chat/report/ReportingContext;create(" +
                            "Lnet/minecraft/client/multiplayer/chat/report/ReportEnvironment;" +
                            "Lcom/mojang/authlib/minecraft/UserApiService;)" +
                            "Lnet/minecraft/client/multiplayer/chat/report/ReportingContext;"
            )
    )
    private ReportingContext updateEnviromentInit(ReportEnvironment reportEnvironment, UserApiService userApiService) {
        return create(reportEnvironment, userApiService);
    }


    @Redirect(
            method = "updateReportEnvironment(Lnet/minecraft/client/multiplayer/chat/report/ReportEnvironment;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/chat/report/ReportingContext;create(" +
                            "Lnet/minecraft/client/multiplayer/chat/report/ReportEnvironment;" +
                            "Lcom/mojang/authlib/minecraft/UserApiService;)" +
                            "Lnet/minecraft/client/multiplayer/chat/report/ReportingContext;"
            )
    )
    private ReportingContext updateEnviroment(ReportEnvironment reportEnvironment, UserApiService userApiService) {
        return create(reportEnvironment, userApiService);
    }
}
