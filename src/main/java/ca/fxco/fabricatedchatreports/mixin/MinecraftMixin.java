package ca.fxco.fabricatedchatreports.mixin;

import ca.fxco.fabricatedchatreports.helpers.FabricatedAbuseReportSender;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.client.report.AbuseReportSender;
import net.minecraft.client.report.ChatLogImpl;
import net.minecraft.client.report.ReporterEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {

    private static AbuseReportSender createSender(ReporterEnvironment reportEnvironment, UserApiService userApiService) {
        return new FabricatedAbuseReportSender(reportEnvironment, userApiService);
    }

    private static AbuseReportContext create(ReporterEnvironment reportEnvironment, UserApiService userApiService) {
        ChatLogImpl rollingMemoryChatLog = new ChatLogImpl(1024);
        AbuseReportSender abuseReportSender = createSender(reportEnvironment, userApiService);
        return new AbuseReportContext(abuseReportSender, reportEnvironment, rollingMemoryChatLog);
    }


    @Redirect(
            method = "<init>(Lnet/minecraft/client/RunArgs;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/report/AbuseReportContext;create(" +
                            "Lnet/minecraft/client/report/ReporterEnvironment;" +
                            "Lcom/mojang/authlib/minecraft/UserApiService;)" +
                            "Lnet/minecraft/client/report/AbuseReportContext;"
            )
    )
    private AbuseReportContext updateEnviromentInit(ReporterEnvironment environment, UserApiService userApiService) {
        return create(environment, userApiService);
    }


    @Redirect(
            method = "ensureAbuseReportContext(Lnet/minecraft/client/report/ReporterEnvironment;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/report/AbuseReportContext;create(" +
                            "Lnet/minecraft/client/report/ReporterEnvironment;" +
                            "Lcom/mojang/authlib/minecraft/UserApiService;)" +
                            "Lnet/minecraft/client/report/AbuseReportContext;"
            )
    )
    private AbuseReportContext updateEnviroment(ReporterEnvironment environment, UserApiService userApiService) {
        return create(environment, userApiService);
    }
}
