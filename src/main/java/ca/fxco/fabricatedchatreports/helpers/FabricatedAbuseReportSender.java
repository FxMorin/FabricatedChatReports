package ca.fxco.fabricatedchatreports.helpers;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.chat.report.AbuseReportSender;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Environment(EnvType.CLIENT)
public record FabricatedAbuseReportSender(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
    private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
    private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
    private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

    @Override
    public CompletableFuture<Unit> send(UUID uUID, AbuseReport abuseReport) {
        return CompletableFuture.supplyAsync(
                () -> {
                    AbuseReportRequest abuseReportRequest;
                    if (abuseReport instanceof FabricatedAbuseReport far && far.shouldModify()) {
                        abuseReportRequest = new AbuseReportRequest(
                                uUID,
                                new AbuseReport(far.type, far.opinionComments, far.reason, far.evidence, far.reportedEntity, far.createdTime),
                                far.getClientVersion() != null && !Objects.equals(far.getClientVersion(), "") ? new AbuseReportRequest.ClientInfo(far.getClientVersion()) : this.environment.clientInfo(),
                                far.getServerIp() != null && !Objects.equals(far.getServerIp(), "") ? new AbuseReportRequest.ThirdPartyServerInfo(far.getServerIp()) : this.environment.thirdPartyServerInfo(),
                                this.environment.realmInfo() //TODO: later
                        );
                    } else {
                        abuseReportRequest = new AbuseReportRequest(uUID, abuseReport, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo());
                    }
                    try {
                        System.out.println(ObjectMapper.create().writeValueAsString(abuseReportRequest));
                        //this.userApiService.reportAbuse(abuseReportRequest);
                        return Unit.INSTANCE;
                    } catch (MinecraftClientHttpException var6) {
                        Component component = this.getHttpErrorDescription(var6);
                        throw new CompletionException(new AbuseReportSender.SendException(component, var6));
                    } catch (MinecraftClientException var7) {
                        Component componentx = this.getErrorDescription(var7);
                        throw new CompletionException(new AbuseReportSender.SendException(componentx, var7));
                    }
                },
                Util.ioPool()
        );
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private Component getHttpErrorDescription(MinecraftClientHttpException minecraftClientHttpException) {
        return Component.translatable("gui.abuseReport.send.error_message", minecraftClientHttpException.getMessage());
    }

    private Component getErrorDescription(MinecraftClientException minecraftClientException) {
        return switch(minecraftClientException.getType()) {
            case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
            case HTTP_ERROR -> HTTP_ERROR_TEXT;
            case JSON_ERROR -> JSON_ERROR_TEXT;
            default -> throw new IncompatibleClassChangeError();
        };
    }

    @Override
    public AbuseReportLimits reportLimits() {
        return this.userApiService.getAbuseReportLimits();
    }
}
