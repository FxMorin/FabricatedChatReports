package ca.fxco.fabricatedchatreports.mixin;

import ca.fxco.fabricatedchatreports.helpers.ExtendedChatAbuseReport;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.client.report.ChatAbuseReport;
import net.minecraft.client.report.ReceivedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatAbuseReport.class)
public abstract class ChatAbuseReportMixin implements ExtendedChatAbuseReport {

    @Shadow
    protected abstract ReportChatMessage toReportChatMessage(int index, ReceivedMessage.ChatMessage message);

    @Override
    public String getIndexJson(AbuseReportContext reporter, int index) {
        ReceivedMessage receivedMessage = reporter.chatLog().get(index);
        return receivedMessage instanceof ReceivedMessage.ChatMessage chatMessage ?
                ObjectMapper.create().writeValueAsString(this.toReportChatMessage(index, chatMessage)) :
                null;
    }
}
