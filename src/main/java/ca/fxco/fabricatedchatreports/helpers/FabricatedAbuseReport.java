package ca.fxco.fabricatedchatreports.helpers;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class FabricatedAbuseReport extends AbuseReport {

    private boolean shouldModify;

    @Nullable
    private String clientVersion;
    @Nullable
    private String serverIp;
    @Nullable
    private String realmsId;
    @Nullable
    private Integer realmsSlotId;

    public FabricatedAbuseReport(String opinionComments, String reason, ReportEvidence evidence, ReportedEntity reportedEntity, Instant createdTime) {
        super(opinionComments, reason, evidence, reportedEntity, createdTime);
        this.shouldModify = false;
    }

    public void setShouldModify(boolean shouldModify) {
        this.shouldModify = shouldModify;
    }

    public void setClientVersion(@Nullable String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public void setServerIp(@Nullable String serverIp) {
        this.serverIp = serverIp;
    }

    public void setRealmsId(@Nullable String realmsId) {
        this.realmsId = realmsId;
    }

    public void setRealmsSlotId(@Nullable Integer realmsSlotId) {
        this.realmsSlotId = realmsSlotId;
    }

    public @Nullable String getClientVersion() {
        return this.clientVersion;
    }

    public @Nullable String getServerIp() {
        return this.serverIp;
    }

    public @Nullable String getRealmsId() {
        return this.realmsId;
    }

    public @Nullable Integer getRealmsSlotId() {
        return this.realmsSlotId;
    }

    public boolean shouldModify() {
        return this.shouldModify;
    }
}
