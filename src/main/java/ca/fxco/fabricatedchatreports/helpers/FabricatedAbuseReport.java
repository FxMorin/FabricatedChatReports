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

    public FabricatedAbuseReport(String type, String opinionComments, String reason, ReportEvidence evidence, ReportedEntity reportedEntity, Instant createdTime) {
        super(type, opinionComments, reason, evidence, reportedEntity, createdTime);
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

    public @Nullable String getClientVersion() {
        return this.clientVersion;
    }

    public @Nullable String getServerIp() {
        return this.serverIp;
    }

    public boolean shouldModify() {
        return this.shouldModify;
    }
}
