package ca.fxco.fabricatedchatreports.mixin;

import net.minecraft.client.gui.screen.report.ChatSelectionScreen;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.client.report.ChatAbuseReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatSelectionScreen.class)
public interface ChatSelectionScreenAccessor {
    @Accessor("report")
    ChatAbuseReport getReport();

    @Accessor("reporter")
    AbuseReportContext getReporter();
}
