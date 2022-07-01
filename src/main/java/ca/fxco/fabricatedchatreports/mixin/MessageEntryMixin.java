package ca.fxco.fabricatedchatreports.mixin;

import ca.fxco.fabricatedchatreports.helpers.ExtendedChatAbuseReport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.report.ChatSelectionScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatSelectionScreen.SelectionListWidget.MessageEntry.class)
public class MessageEntryMixin {

    @Shadow
    @Final
    private int index;


    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 1) {
            ChatSelectionScreenAccessor screenAccessor = ((ChatSelectionScreenAccessor)MinecraftClient.getInstance().currentScreen);
            MinecraftClient.getInstance().keyboard.setClipboard(((ExtendedChatAbuseReport)screenAccessor.getReport()).getIndexJson(screenAccessor.getReporter(), this.index));
            cir.setReturnValue(true);
        }
    }
}
