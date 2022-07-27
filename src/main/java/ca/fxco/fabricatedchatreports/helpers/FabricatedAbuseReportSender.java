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
import net.minecraft.client.report.AbuseReportSender;
import net.minecraft.client.report.ReporterEnvironment;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static ca.fxco.fabricatedchatreports.FabricatedChatReports.DEMO_MODE;

@Environment(EnvType.CLIENT)
public record FabricatedAbuseReportSender(ReporterEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
    private static final Text SERVICE_UNAVAILABLE_TEXT = Text.translatable("gui.abuseReport.send.service_unavailable");
    private static final Text HTTP_ERROR_TEXT = Text.translatable("gui.abuseReport.send.http_error");
    private static final Text JSON_ERROR_TEXT = Text.translatable("gui.abuseReport.send.json_error");

    @Override
    public CompletableFuture<Unit> send(UUID uUID, AbuseReport abuseReport) {
        return CompletableFuture.supplyAsync(
                () -> {
                    AbuseReportRequest abuseReportRequest;
                    if (abuseReport instanceof FabricatedAbuseReport far && far.shouldModify()) {
                        abuseReportRequest = new AbuseReportRequest(
                                uUID,
                                new AbuseReport(far.opinionComments, far.reason, far.evidence, far.reportedEntity, far.createdTime),
                                far.getClientVersion() != null && !Objects.equals(far.getClientVersion(), "") ? new AbuseReportRequest.ClientInfo(far.getClientVersion()) : this.environment.toClientInfo(),
                                far.getServerIp() != null && !Objects.equals(far.getServerIp(), "") ? new AbuseReportRequest.ThirdPartyServerInfo(far.getServerIp()) : this.environment.toThirdPartyServerInfo(),
                                (this.environment.toRealmInfo() == null && (far.getRealmsId() == null || far.getRealmsSlotId() == null)) ? null : new AbuseReportRequest.RealmInfo(far.getRealmsId() == null ? this.environment.toRealmInfo().realmId : far.getRealmsId(), far.getRealmsSlotId() == null ? this.environment.toRealmInfo().slotId : far.getRealmsSlotId())
                        );
                    } else {
                        abuseReportRequest = new AbuseReportRequest(uUID, abuseReport, this.environment.toClientInfo(), this.environment.toThirdPartyServerInfo(), this.environment.toRealmInfo());
                    }
                    try {
                        if (DEMO_MODE) {
                            System.out.println(ObjectMapper.create().writeValueAsString(abuseReportRequest));
                        } else {
                            this.userApiService.reportAbuse(abuseReportRequest);
                        }
                        return Unit.INSTANCE;
                    } catch (MinecraftClientHttpException var6) {
                        Text component = this.getHttpErrorDescription(var6);
                        throw new CompletionException(new AbuseReportSender.AbuseReportException(component, var6));
                    } catch (MinecraftClientException var7) {
                        Text componentx = this.getErrorDescription(var7);
                        throw new CompletionException(new AbuseReportSender.AbuseReportException(componentx, var7));
                    }
                },
                Util.getIoWorkerExecutor()
        );
    }

    @Override
    public boolean canSendReports() {
        return DEMO_MODE || this.userApiService.canSendReports();
    }

    private Text getHttpErrorDescription(MinecraftClientHttpException minecraftClientHttpException) {
        return Text.translatable("gui.abuseReport.send.error_message", minecraftClientHttpException.getMessage());
    }

    private Text getErrorDescription(MinecraftClientException minecraftClientException) {
        return switch(minecraftClientException.getType()) {
            case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
            case HTTP_ERROR -> HTTP_ERROR_TEXT;
            case JSON_ERROR -> JSON_ERROR_TEXT;
            default -> throw new IncompatibleClassChangeError();
        };
    }

    @Override
    public AbuseReportLimits getLimits() {
        return this.userApiService.getAbuseReportLimits();
    }
}
