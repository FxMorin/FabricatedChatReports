package ca.fxco.fabricatedchatreports.mixin;

import ca.fxco.fabricatedchatreports.screens.ModifyReportScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.report.ChatReportBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
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
    protected abstract int completeButtonTop();

    @Shadow
    @Nullable
    private ChatReportBuilder.@Nullable CannotBuildReason cannotBuildReason;

    @Shadow
    private ChatReportBuilder report;

    @Shadow
    @Final
    private ReportingContext reportingContext;

    @Unique
    private Button fabricateReportButton;

    protected ChatReportScreenMixin(Component component) {
        super(component);
    }


    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/reporting/ChatReportScreen;onReportChanged()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onInitializeAddButton(CallbackInfo ci) {
        int i = this.width / 2;
        this.fabricateReportButton = this.addRenderableWidget(
                new Button(
                        i + 140,
                        this.completeButtonTop(),
                        120,
                        20,
                        Component.literal("Fabricate Report"),
                        button -> this.callFabricationScreen()
                )
        );
    }


    @Inject(
            method = "onReportChanged",
            at = @At("RETURN")
    )
    private void addNewButton(CallbackInfo ci) {
        this.fabricateReportButton.active = this.cannotBuildReason == null;
    }


    private void callFabricationScreen() {
        this.report.build(this.reportingContext).left().ifPresent(result -> {
            this.minecraft.setScreen(new ModifyReportScreen(this, result.id(), result.report(), this.reportingContext));
        });
    }
}
