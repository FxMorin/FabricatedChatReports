package ca.fxco.fabricatedchatreports.screens;

import ca.fxco.fabricatedchatreports.FabricatedChatReports;
import ca.fxco.fabricatedchatreports.helpers.FabricatedAbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class ModifyReportScreen extends Screen {

    private static final Component TITLE = Component.literal("Fabricated Report Screen");
    private static final Component REPORT_SENT_MESSAGE = Component.translatable("gui.chatReport.report_sent_msg");
    private static final Component REPORT_SENDING_TITLE = Component.literal("Sending Fabricated Report").withStyle(ChatFormatting.BOLD);
    private static final Component REPORT_SENT_TITLE = Component.translatable("gui.abuseReport.sent.title").withStyle(ChatFormatting.BOLD);
    private static final Component REPORT_ERROR_TITLE = Component.translatable("gui.abuseReport.error.title").withStyle(ChatFormatting.BOLD);
    private static final Component REPORT_SEND_GENERIC_ERROR = Component.translatable("gui.abuseReport.send.generic_error");

    @Nullable
    private final Screen lastScreen;
    private Button sendButton;
    private UUID reportId;
    private FabricatedAbuseReport abuseReport;
    private final ReportingContext reportingContext;
    @Nullable
    Component cannotBuildReason = null;

    private EditBox clientVersionEdit;
    private EditBox serverIpEdit;
    //private EditBox realmsIdEdit; //TODO: Add later
    //private EditBox realmsSlotEdit;

    public ModifyReportScreen(@Nullable Screen screen, UUID reportId, AbuseReport report, ReportingContext reportingContext) {
        super(TITLE);
        this.lastScreen = screen;
        this.reportId = reportId;
        this.reportingContext = reportingContext;
        if (report instanceof FabricatedAbuseReport far) {
            this.abuseReport = far;
        } else {
            this.abuseReport = new FabricatedAbuseReport(
                    report.type,
                    report.opinionComments,
                    report.reason,
                    report.evidence,
                    report.reportedEntity,
                    report.createdTime
            );
        }
        this.abuseReport.setShouldModify(true);
    }

    private Component isValid() {
        return null; //Always returns null until I implement actual checks
    }

    private void onReportChanged() {
        this.abuseReport.setClientVersion(clientVersionEdit.getValue());
        this.abuseReport.setServerIp(serverIpEdit.getValue());
        this.cannotBuildReason = isValid();
        this.sendButton.active = this.cannotBuildReason == null;
    }

    @Override
    protected void init() {
        int i = this.width / 2;
        this.addRenderableWidget(
                new Button(
                        i - 140,
                        this.contentTop() + 30,
                        280,
                        20,
                        Component.literal("Modify chat context"),
                        button -> this.minecraft.setScreen(new ContextModifyingScreen(this, this.abuseReport, modifiedAbuseReport -> {
                            this.abuseReport = modifiedAbuseReport;
                            this.onReportChanged();
                        }))
                )
        );
        this.clientVersionEdit = new EditBox(this.font, i - 50, this.contentTop() + 60, 200, 20, Component.literal("Client version"));
        this.clientVersionEdit.setMaxLength(128);
        this.clientVersionEdit.setValue(this.reportingContext.environment().clientVersion());
        this.clientVersionEdit.setResponder(string -> this.onReportChanged());
        this.addRenderableWidget(this.clientVersionEdit);
        this.serverIpEdit = new EditBox(this.font, i - 50, this.contentTop() + 90, 200, 20, Component.literal("Server address"));
        this.serverIpEdit.setMaxLength(128);
        this.serverIpEdit.setValue(this.reportingContext.environment().thirdPartyServerInfo() != null ? this.reportingContext.environment().thirdPartyServerInfo().address : "");
        this.serverIpEdit.setResponder(string -> this.onReportChanged());
        this.addRenderableWidget(this.serverIpEdit);
        this.addRenderableWidget(
                new Button(
                        i - 170,
                        this.contentTop() + 135,
                        160,
                        20,
                        CommonComponents.GUI_BACK,
                        button -> onClose()
                )
        );
        this.sendButton = this.addRenderableWidget(
                new Button(
                        i + 10,
                        this.contentTop() + 135,
                        160,
                        20,
                        Component.literal("Send Fabricated Report"),
                        button -> this.sendFabricatedReport()
                )
        );
        this.onReportChanged();
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        int k = this.width / 2;
        RenderSystem.disableDepthTest();
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, k, 10, 16777215);
        drawString(poseStack, this.font, Component.literal("Client Version"),k - 140, this.contentTop() + 65, 16777215);
        drawString(poseStack, this.font, Component.literal("Server Ip"),k - 140, this.contentTop() + 95, 16777215);
        super.render(poseStack, i, j, f);
        RenderSystem.enableDepthTest();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void tick() {
        this.clientVersionEdit.tick();
        this.serverIpEdit.tick();
        super.tick();
    }

    private int contentTop() {
        return Math.max((this.height - 300) / 2, 0);
    }

    private void sendFabricatedReport() {
        CompletableFuture<?> sendReport = this.reportingContext.sender().send(this.reportId, abuseReport);
        this.minecraft.setScreen(GenericWaitingScreen.createWaiting(REPORT_SENDING_TITLE, CommonComponents.GUI_CANCEL, () -> {
            this.minecraft.setScreen(this);
            sendReport.cancel(true);
        }));
        sendReport.handleAsync((object, throwable) -> {
            if (throwable == null) {
                this.onReportSendSuccess();
            } else {
                if (throwable instanceof CancellationException) return null;
                this.onReportSendError(throwable);
            }
            return null;
        }, this.minecraft);
    }

    private void onReportSendSuccess() {
        this.minecraft.setScreen(GenericWaitingScreen.createCompleted(REPORT_SENT_TITLE, REPORT_SENT_MESSAGE, CommonComponents.GUI_DONE, () -> this.minecraft.setScreen(null)));
    }

    private void onReportSendError(Throwable throwable) {
        FabricatedChatReports.LOGGER.error("Encountered error while sending abuse report", throwable);
        Throwable var4 = throwable.getCause();
        Component component;
        if (var4 instanceof ThrowingComponent throwingComponent) {
            component = throwingComponent.getComponent();
        } else {
            component = REPORT_SEND_GENERIC_ERROR;
        }
        Component component2 = component.copy().withStyle(ChatFormatting.RED);
        this.minecraft.setScreen(GenericWaitingScreen.createCompleted(REPORT_ERROR_TITLE, component2, CommonComponents.GUI_BACK, () -> this.minecraft.setScreen(this)));
    }
}
