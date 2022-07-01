package ca.fxco.fabricatedchatreports.mixin;

import net.minecraft.client.gui.screen.report.ChatSelectionScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatSelectionScreen.class)
public class ChatSelectionScreenMixin {

    private static final Text addition = Text.literal(" - Right-click to copy a messages data");


    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)" +
                            "Lnet/minecraft/text/MutableText;"
            )
    )
    public MutableText render(String key, Object[] args) {
        return Text.translatable(key, args).append(addition);
    }
}
