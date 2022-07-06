package ca.fxco.fabricatedchatreports.helpers;

import net.minecraft.client.report.AbuseReportContext;

public interface ExtendedChatAbuseReport {
    String getIndexJson(AbuseReportContext reporter, int index);
}
