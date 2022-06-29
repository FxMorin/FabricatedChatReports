package ca.fxco.fabricatedchatreports.screens;

import ca.fxco.fabricatedchatreports.FabricatedChatReports;
import ca.fxco.fabricatedchatreports.helpers.FabricatedAbuseReport;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ContextModifyingScreen extends Screen {
    private static final Component CONTEXT_INFO = Component.literal("Select messages to remove from the report").withStyle(ChatFormatting.GRAY);
    @Nullable
    private final Screen lastScreen;
    private MultiLineLabel contextInfoLabel;
    @Nullable
    private ContextModifyingScreen.ChatModifyingList chatModifyingList;
    final FabricatedAbuseReport abuseReport;
    private final Consumer<FabricatedAbuseReport> onSelected;

    protected EditBox messageEdit;

    public ContextModifyingScreen(
            @Nullable Screen screen, FabricatedAbuseReport abuseReport, Consumer<FabricatedAbuseReport> consumer
    ) {
        super(Component.literal("Right click to toggle messages. Left click to modify YOUR messages"));
        this.lastScreen = screen;
        this.abuseReport = abuseReport;
        this.onSelected = consumer;
    }

    private void modifyAbuseReport(FabricatedAbuseReport abuseReport) {
        List<ReportChatMessage> list = new ArrayList<>(abuseReport.evidence.messages);
        for (int i = list.size()-1; i >= 0; i--) {
            ReportChatMessage chatMessage = list.get(i);
            for (ChatModifyingList.Entry entry : chatModifyingList.children()) {
                if (entry.chatMessage() == chatMessage) {
                    if (entry.shouldInclude()) {
                        if (entry.hasChanged()) list.set(i, entry.chatMessage());
                    } else {
                        list.remove(chatMessage);
                    }
                    break;
                }
            }
        }
        abuseReport.evidence.messages = list;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (this.messageEdit.active) {
            boolean result = this.messageEdit.keyPressed(i, j, k);
            if (i == 257 || i == 256) {
                this.messageEdit.active = false;
                this.messageEdit.visible = false;
            }
            return result;
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    protected void init() {
        this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
        this.chatModifyingList = new ChatModifyingList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9);
        this.chatModifyingList.setRenderBackground(false);
        this.chatModifyingList.acceptMessages(this.abuseReport.evidence.messages);
        this.addWidget(this.chatModifyingList);
        this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 32, 150, 20, CommonComponents.GUI_BACK, button -> this.onClose()));
        this.addRenderableWidget(
                new Button(this.width / 2 - 155 + 160, this.height - 32, 150, 20, CommonComponents.GUI_DONE, button -> {
                    modifyAbuseReport(this.abuseReport);
                    this.onSelected.accept(this.abuseReport);
                    this.onClose();
                })
        );
        this.chatModifyingList.setScrollAmount(this.chatModifyingList.getMaxScroll());
        this.messageEdit = new EditBox(this.font, this.width / 2 - 150, 80, 300, 20, CommonComponents.EMPTY);
        this.messageEdit.setMaxLength(256);
        this.messageEdit.setValue("");
        this.addWidget(this.messageEdit);
        this.messageEdit.active = false;
        this.messageEdit.visible = false;
        this.chatModifyingList.acceptMessageBox(this.messageEdit);
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        String string = this.messageEdit.getValue();
        boolean active = this.messageEdit.active;
        this.init(minecraft, i, j);
        this.messageEdit.setValue(string);
        this.messageEdit.active = active;
        this.messageEdit.visible = active;
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        this.chatModifyingList.render(poseStack, i, j, f);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 16, 16777215);
        this.contextInfoLabel.renderCentered(poseStack, this.width / 2, this.chatModifyingList.getFooterTop());
        this.messageEdit.render(poseStack, i, j, f);
        super.render(poseStack, i, j, f);
    }

    @Override
    public void tick() {
        this.messageEdit.tick();
        super.tick();
    }

    @Override
    public void onClose() {
        this.onSelected.accept(this.abuseReport);
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
    }

    @Environment(EnvType.CLIENT)
    public class ChatModifyingList extends ObjectSelectionList<ChatModifyingList.Entry> implements Output {

        public EditBox messageEdit;

        public ChatModifyingList(Minecraft minecraft, int i) {
            super(minecraft, ContextModifyingScreen.this.width, ContextModifyingScreen.this.height, 40, ContextModifyingScreen.this.height - 40 - i, 16);
        }

        @Override
        public void acceptMessageBox(EditBox box) {
            this.messageEdit = box;
        }

        @Override
        public void acceptMessage(ReportChatMessage chatMessage) {
            this.addEntry(new MessageEntry(chatMessage));
        }

        @Override
        protected int getScrollbarPosition() {
            return (this.width + this.getRowWidth()) / 2;
        }

        @Override
        public int getRowWidth() {
            return Math.min(350, this.width - 50);
        }

        @Override
        protected void renderItem(PoseStack poseStack, int i, int j, float f, int k, int l, int m, int n, int o) {
            ChatModifyingList.Entry entry = this.getEntry(k);
            if (this.shouldHighlightEntry(entry)) {
                boolean bl = this.getSelected() == entry;
                int p = this.isFocused() && bl ? -1 : -8355712;
                this.renderSelection(poseStack, m, n, o, p, -16777216);
            }
            entry.render(poseStack, k, m, l, n, o, i, j, this.getHovered() == entry, f);
        }

        private boolean shouldHighlightEntry(ChatModifyingList.Entry entry) {
            if (entry.canSelect()) {
                boolean bl = this.getSelected() == entry;
                boolean bl2 = this.getSelected() == null;
                boolean bl3 = this.getHovered() == entry;
                return bl || bl2 && bl3 && entry.shouldInclude();
            } else {
                return false;
            }
        }

        @Override
        protected void moveSelection(AbstractSelectionList.SelectionDirection selectionDirection) {
            if (!this.moveSelectableSelection(selectionDirection) && selectionDirection == AbstractSelectionList.SelectionDirection.UP)
                this.moveSelectableSelection(selectionDirection);
        }

        private boolean moveSelectableSelection(AbstractSelectionList.SelectionDirection selectionDirection) {
            return this.moveSelection(selectionDirection, ChatModifyingList.Entry::canSelect);
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (this.messageEdit.active) return this.messageEdit.mouseClicked(d, e, i);
            return super.mouseClicked(d, e, i);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            ChatModifyingList.Entry entry = this.getSelected();
            if (entry != null && entry.keyPressed(i, j, k)) {
                return true;
            } else {
                this.setFocused(null);
                return super.keyPressed(i, j, k);
            }
        }

        @Override
        public boolean charTyped(char i, int j) {
            if (this.messageEdit.active) return this.messageEdit.charTyped(i, j);
            return super.charTyped(i, j);
        }

        public int getFooterTop() {
            return this.y1 + 9;
        }

        @Override
        protected boolean isFocused() {
            return ContextModifyingScreen.this.getFocused() == this;
        }

        @Environment(EnvType.CLIENT)
        public abstract class Entry extends ObjectSelectionList.Entry<ChatModifyingList.Entry> {
            @Override
            public Component getNarration() {
                return CommonComponents.EMPTY;
            }

            public boolean isSelected() {
                return false;
            }

            public boolean canSelect() {
                return false;
            }

            public boolean shouldInclude() {
                return this.canSelect();
            }

            public void modifyText(String text) {}

            public ReportChatMessage chatMessage() {
                return null;
            }

            public boolean hasChanged() {
                return true;
            }

            public String getText() {
                return "";
            }
        }

        @Environment(EnvType.CLIENT)
        public class MessageEntry extends ChatModifyingList.Entry {
            private static final ResourceLocation CHECKMARK_TEXTURE = new ResourceLocation("realms", "textures/gui/realms/checkmark.png");
            private static final int CHECKMARK_WIDTH = 9;
            private static final int CHECKMARK_HEIGHT = 8;
            private static final int INDENT_AMOUNT = 11;
            private FormattedText text;
            private final String username;
            private final ReportChatMessage chatMessage;
            private boolean includeMessage;
            private boolean hasChanged;
            private final boolean wasReported;
            private final boolean canModify;

            public MessageEntry(ReportChatMessage chatMessage) {
                this.chatMessage = chatMessage;
                Component component = Component.literal(chatMessage.message);
                FormattedText formattedText = ContextModifyingScreen.this.font.substrByWidth(component, this.getMaximumTextWidth() - ContextModifyingScreen.this.font.width(CommonComponents.ELLIPSIS));
                this.text = component != formattedText ? FormattedText.composite(formattedText, CommonComponents.ELLIPSIS) : component;
                this.includeMessage = true;
                this.hasChanged = false;
                this.wasReported = chatMessage.messageReported;
                this.canModify = !this.wasReported && chatMessage.profileId == Minecraft.getInstance().player.getUUID();
                if (Minecraft.getInstance().level != null) {
                    Player player = Minecraft.getInstance().level.getPlayerByUUID(chatMessage.profileId);
                    if (player != null) {
                        this.username = player.getScoreboardName();
                    } else {
                        this.username = chatMessage.profileId.toString().substring(0, 8);
                    }
                } else {
                    this.username = chatMessage.profileId.toString().substring(0, 8);
                }
            }

            @Override
            public void modifyText(String text) {
                if (this.canModify) {
                    this.hasChanged = !Objects.equals(this.text.getString(), text);
                    this.text = Component.literal(text);
                }
            }

            @Override
            public ReportChatMessage chatMessage() {
                return this.chatMessage;
            }

            @Override
            public boolean hasChanged() {
                return this.hasChanged;
            }

            @Override
            public String getText() {
                return this.text.getString();
            }

            @Override
            public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
                int p = k + this.getTextIndent();
                int q = j + 1 + (m - CHECKMARK_WIDTH) / 2;
                if (this.isSelected() && this.includeMessage || !this.canModify) {
                    this.renderCheckmark(poseStack, j, k, m);
                }
                if (this.wasReported) {
                    drawString(poseStack,ContextModifyingScreen.this.font, Component.literal(this.username).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), p, q, this.includeMessage ? -1 : -1593835521);
                } else if (this.canModify) {
                    drawString(poseStack,ContextModifyingScreen.this.font, this.username, p, q, this.includeMessage ? -1 : -1593835521);
                } else {
                    drawString(poseStack,ContextModifyingScreen.this.font, Component.literal(this.username).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)), p, q, this.includeMessage ? -1 : -1593835521);
                }
                drawString(poseStack, ContextModifyingScreen.this.font, this.text.getString(), p + 90, q, this.includeMessage ? -1 : -1593835521);
            }

            private void renderCheckmark(PoseStack poseStack, int i, int j, int k) {
                int m = i + (k - CHECKMARK_HEIGHT) / 2;
                RenderSystem.setShaderTexture(0, CHECKMARK_TEXTURE);
                RenderSystem.enableBlend();
                GuiComponent.blit(poseStack, j, m, 0.0F, 0.0F, CHECKMARK_WIDTH, CHECKMARK_HEIGHT, CHECKMARK_WIDTH, CHECKMARK_HEIGHT);
                RenderSystem.disableBlend();
            }

            private int getMaximumTextWidth() {
                return ChatModifyingList.this.getRowWidth() - this.getTextIndent();
            }

            private int getTextIndent() {
                return INDENT_AMOUNT;
            }

            private MessageSignature signMessage(MessageSigner messageSigner, Component component) {
                try {
                    Signer signer = Minecraft.getInstance().getProfileKeyPairManager().signer();
                    if (signer != null) {
                        return messageSigner.sign(signer, component);
                    }
                } catch (Exception var4) {
                    FabricatedChatReports.LOGGER.error("Failed to sign chat message: '{}'", component.getString(), var4);
                }

                return MessageSignature.unsigned();
            }

            private void reSignMessage() {
                MessageSigner messageSigner = new MessageSigner(this.chatMessage.profileId, this.chatMessage.timestamp, this.chatMessage.salt);
                MessageSignature fabricatedSignature = signMessage(messageSigner, Component.literal(this.text.getString()));
                this.chatMessage.signature = new String(fabricatedSignature.saltSignature().signature());
                this.chatMessage.salt = fabricatedSignature.saltSignature().salt();
            }

            @Override
            public boolean mouseClicked(double d, double e, int i) {
                if (!this.canModify) return false;
                if (i == 1) {
                    ChatModifyingList.this.setSelected(null);
                    return this.toggleInclude();
                } else {
                    if (!ChatModifyingList.this.messageEdit.active) {
                        ChatModifyingList.this.messageEdit.setResponder(string -> {
                            this.modifyText(string);
                            // Every time you press a key im going to re-sign the entire message cause I don't give a damn
                            // This is just to show a proof of concept so ive really been slacking on this project
                            if (this.hasChanged) this.reSignMessage();
                        });
                        ChatModifyingList.this.messageEdit.setValue(this.text.getString());
                        ChatModifyingList.this.messageEdit.active = true;
                        ChatModifyingList.this.messageEdit.visible = true;
                        ChatModifyingList.this.messageEdit.setFocus(true);
                        return true;
                    }
                    return false;
                }
            }

            @Override
            public boolean keyPressed(int i, int j, int k) {
                return false;
            }

            @Override
            public boolean isSelected() {
                return true;
            }

            @Override
            public boolean canSelect() {
                return true;
            }

            @Override
            public boolean shouldInclude() {
                return this.includeMessage;
            }

            private boolean toggleInclude() {
                this.includeMessage = !this.includeMessage;
                return true;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Output {
        default void acceptMessages(List<ReportChatMessage> chatMessages) {
            for (ReportChatMessage chat : chatMessages) this.acceptMessage(chat);
        }
        void acceptMessage(ReportChatMessage chatMessage);

        void acceptMessageBox(EditBox box);
    }
}