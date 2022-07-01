package ca.fxco.fabricatedchatreports.screens;

import ca.fxco.fabricatedchatreports.FabricatedChatReports;
import ca.fxco.fabricatedchatreports.helpers.FabricatedAbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TaskScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TextifiedException;

import static ca.fxco.fabricatedchatreports.FabricatedChatReports.DEMO_MODE;

public class ModifyReportScreen extends Screen {

    private static final Text TITLE = Text.literal("Fabricated Report Screen");
    private static final Text REPORT_SENT_MESSAGE = Text.translatable("gui.chatReport.report_sent_msg");
    private static final Text REPORT_SENT_DEMO_MESSAGE = Text.literal("This message was not actually sent since this is a demo, due to the fact that mojang might actually be going through with these reports at the moment! Open the project and disable demo mode for the full mod!");
    private static final Text REPORT_SENDING_TITLE = Text.literal("Sending Fabricated Report").formatted(Formatting.BOLD);
    private static final Text REPORT_SENT_TITLE = Text.translatable("gui.abuseReport.sent.title").formatted(Formatting.BOLD);
    private static final Text REPORT_ERROR_TITLE = Text.translatable("gui.abuseReport.error.title").formatted(Formatting.BOLD);
    private static final Text REPORT_SEND_GENERIC_ERROR = Text.translatable("gui.abuseReport.send.generic_error");

    @Nullable
    private final Screen lastScreen;
    private ButtonWidget sendButton;
    private UUID reportId;
    private FabricatedAbuseReport abuseReport;
    private final AbuseReportContext reportingContext;
    @Nullable
    Text cannotBuildReason = null;

    private TextFieldWidget clientVersionEdit;
    private TextFieldWidget serverIpEdit;
    //private EditBox realmsIdEdit; //TODO: Add later
    //private EditBox realmsSlotEdit;

    public ModifyReportScreen(@Nullable Screen screen, UUID reportId, AbuseReport report, AbuseReportContext reportingContext) {
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

    private Text isValid() {
        return null; //Always returns null until I implement actual checks
    }

    private void onReportChanged() {
        this.abuseReport.setClientVersion(clientVersionEdit.getText());
        this.abuseReport.setServerIp(serverIpEdit.getText());
        this.cannotBuildReason = isValid();
        this.sendButton.active = this.cannotBuildReason == null;
    }

    @Override
    protected void init() {
        int i = this.width / 2;
        this.addDrawableChild(
                new ButtonWidget(
                        i - 140,
                        this.contentTop() + 30,
                        280,
                        20,
                        Text.literal("Modify chat context"),
                        button -> this.client.setScreen(new ContextModifyingScreen(this, this.abuseReport, modifiedAbuseReport -> {
                            this.abuseReport = modifiedAbuseReport;
                            this.onReportChanged();
                        }))
                )
        );
        this.clientVersionEdit = new TextFieldWidget(this.textRenderer, i - 50, this.contentTop() + 60, 200, 20, Text.literal("Client version"));
        this.clientVersionEdit.setMaxLength(128);
        this.clientVersionEdit.setText(this.reportingContext.environment().clientVersion());
        this.clientVersionEdit.setChangedListener(string -> this.onReportChanged());
        this.addDrawableChild(this.clientVersionEdit);
        this.serverIpEdit = new TextFieldWidget(this.textRenderer, i - 50, this.contentTop() + 90, 200, 20, Text.literal("Server address"));
        this.serverIpEdit.setMaxLength(128);
        this.serverIpEdit.setText(this.reportingContext.environment().toThirdPartyServerInfo() != null ? this.reportingContext.environment().toThirdPartyServerInfo().address : "");
        this.serverIpEdit.setChangedListener(string -> this.onReportChanged());
        this.addDrawableChild(this.serverIpEdit);
        this.addDrawableChild(
                new ButtonWidget(
                        i - 170,
                        this.contentTop() + 135,
                        160,
                        20,
                        ScreenTexts.BACK,
                        button -> close()
                )
        );
        this.sendButton = this.addDrawableChild(
                new ButtonWidget(
                        i + 10,
                        this.contentTop() + 135,
                        160,
                        20,
                        Text.literal("Send Fabricated Report"),
                        button -> this.sendFabricatedReport()
                )
        );
        this.onReportChanged();
    }

    @Override
    public void render(MatrixStack poseStack, int i, int j, float f) {
        int k = this.width / 2;
        RenderSystem.disableDepthTest();
        this.renderBackground(poseStack);
        drawCenteredText(poseStack, this.textRenderer, this.title, k, 10, 16777215);
        drawTextWithShadow(poseStack, this.textRenderer, Text.literal("Client Version"),k - 140, this.contentTop() + 65, 16777215);
        drawTextWithShadow(poseStack, this.textRenderer, Text.literal("Server Ip"),k - 140, this.contentTop() + 95, 16777215);
        super.render(poseStack, i, j, f);
        RenderSystem.enableDepthTest();
    }

    @Override
    public void close() {
        this.client.setScreen(this.lastScreen);
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
        this.client.setScreen(TaskScreen.createRunningScreen(REPORT_SENDING_TITLE, ScreenTexts.CANCEL, () -> {
            this.client.setScreen(this);
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
        }, this.client);
    }

    private void onReportSendSuccess() {
        this.client.setScreen(TaskScreen.createResultScreen(REPORT_SENT_TITLE, DEMO_MODE ? REPORT_SENT_MESSAGE : REPORT_SENT_DEMO_MESSAGE, ScreenTexts.DONE, () -> this.client.setScreen(null)));
    }

    private void onReportSendError(Throwable throwable) {
        FabricatedChatReports.LOGGER.error("Encountered error while sending abuse report", throwable);
        Throwable var4 = throwable.getCause();
        Text component;
        if (var4 instanceof TextifiedException throwingComponent) {
            component = throwingComponent.getMessageText();
        } else {
            component = REPORT_SEND_GENERIC_ERROR;
        }
        Text component2 = component.copy().formatted(Formatting.RED);
        this.client.setScreen(TaskScreen.createResultScreen(REPORT_ERROR_TITLE, component2, ScreenTexts.BACK, () -> this.client.setScreen(this)));
    }
}
