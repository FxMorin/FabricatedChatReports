package ca.fxco.fabricatedchatreports.mixin;

import ca.fxco.fabricatedchatreports.screens.ModifyReportScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.abusereport.ChatReportScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.abusereport.ChatAbuseReport;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatReportScreen.class)
public abstract class ChatReportScreenMixin extends Screen {

    @Shadow
    protected abstract int getBottomButtonsY();

    @Shadow
    @Nullable
    private ChatAbuseReport.ValidationError validationError;

    @Shadow
    private ChatAbuseReport report;

    @Shadow
    @Final
    private AbuseReportContext reporter;

    @Unique
    private ButtonWidget fabricateReportButton;

    protected ChatReportScreenMixin(Text component) {
        super(component);
    }


    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/abusereport/ChatReportScreen;onChange()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onInitializeAddButton(CallbackInfo ci) {
        int i = this.width / 2;
        this.fabricateReportButton = this.addDrawableChild(
                new ButtonWidget(
                        i + 140,
                        this.getBottomButtonsY(),
                        120,
                        20,
                        Text.literal("Fabricate Report"),
                        button -> this.callFabricationScreen()
                )
        );
    }


    @Inject(
            method = "onChange",
            at = @At("RETURN")
    )
    private void addNewButton(CallbackInfo ci) {
        this.fabricateReportButton.active = this.validationError == null;
    }


    private void callFabricationScreen() {
        this.report.finalizeReport(this.reporter).left().ifPresent(result -> {
            this.client.setScreen(new ModifyReportScreen(this, result.id(), result.report(), this.reporter));
        });
    }
}
