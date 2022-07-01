package ca.fxco.fabricatedchatreports.screens;

import ca.fxco.fabricatedchatreports.FabricatedChatReports;
import ca.fxco.fabricatedchatreports.helpers.FabricatedAbuseReport;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.encryption.Signer;
import net.minecraft.network.message.ChatMessageSigner;
import net.minecraft.network.message.MessageSignature;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ContextModifyingScreen extends Screen {
    private static final Text CONTEXT_INFO = Text.literal("This is all the evidence that will be sent to Mojang").formatted(Formatting.GRAY);
    private static final Text TOOLTIP_INJECT = Text.literal("Right-click messages in selection menu to copy them, then press this button to inject them into the conversation");
    @Nullable
    private final Screen lastScreen;
    private MultilineText contextInfoLabel;
    @Nullable
    private ContextModifyingScreen.ChatModifyingList chatModifyingList;
    final FabricatedAbuseReport abuseReport;
    private final Consumer<FabricatedAbuseReport> onSelected;

    protected TextFieldWidget messageEdit;

    public ContextModifyingScreen(
            @Nullable Screen screen, FabricatedAbuseReport abuseReport, Consumer<FabricatedAbuseReport> consumer
    ) {
        super(Text.literal("Right click to toggle messages. Left click to modify YOUR messages"));
        this.lastScreen = screen;
        this.abuseReport = abuseReport;
        this.onSelected = consumer;
    }

    private void modifyAbuseReport() {
        List<ReportChatMessage> list = new ArrayList<>(this.abuseReport.evidence.messages);
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
        this.abuseReport.evidence.messages = list;
    }

    private void addReportChatMessage(ReportChatMessage reportChatMessage) {
        List<ReportChatMessage> list = new ArrayList<>(this.abuseReport.evidence.messages);
        for (int i = list.size()-1; i >= 0; i--) {
            if (list.get(i).timestamp.isAfter(reportChatMessage.timestamp)) {
                list.add(i, reportChatMessage);
                break;
            }
        }
        this.abuseReport.evidence.messages = list;
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
        ButtonWidget.TooltipSupplier tooltipSupplier = new ButtonWidget.TooltipSupplier() {
            @Override
            public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int i, int j) {
                ContextModifyingScreen.this.renderOrderedTooltip(
                        matrixStack, ContextModifyingScreen.this.client.textRenderer.wrapLines(TOOLTIP_INJECT, Math.max(ContextModifyingScreen.this.width / 2 - 43, 170)), i, j
                );
            }
            @Override
            public void supply(Consumer<Text> consumer) {
                consumer.accept(TOOLTIP_INJECT);
            }
        };
        this.addDrawableChild(
                new ButtonWidget(10, 13, 120, 20, Text.literal("Inject message data"), button -> {
                    try {
                        ReportChatMessage reportChatMessage = ObjectMapper.create().readValue(MinecraftClient.getInstance().keyboard.getClipboard(), ReportChatMessage.class);
                        addReportChatMessage(reportChatMessage);
                        this.chatModifyingList.acceptMessage(reportChatMessage); // Visual
                        this.clearAndInit();
                    } catch (MinecraftClientException e) {
                        //TODO: tell the user
                    }
                }, tooltipSupplier)
        );
        this.contextInfoLabel = MultilineText.create(this.textRenderer, CONTEXT_INFO, this.width - 16);
        this.chatModifyingList = new ChatModifyingList(this.client, (this.contextInfoLabel.count() + 1) * 9);
        this.chatModifyingList.setRenderBackground(false);
        this.chatModifyingList.acceptMessages(this.abuseReport.evidence.messages);
        this.addSelectableChild(this.chatModifyingList);
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 155, this.height - 32, 150, 20, ScreenTexts.BACK, button -> this.close()));
        this.addDrawableChild(
                new ButtonWidget(this.width / 2 - 155 + 160, this.height - 32, 150, 20, ScreenTexts.DONE, button -> {
                    modifyAbuseReport();
                    this.onSelected.accept(this.abuseReport);
                    this.close();
                })
        );
        this.chatModifyingList.setScrollAmount(this.chatModifyingList.getMaxScroll());
        this.messageEdit = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 80, 300, 20, ScreenTexts.EMPTY);
        this.messageEdit.setMaxLength(256);
        this.messageEdit.setText("");
        this.addSelectableChild(this.messageEdit);
        this.messageEdit.active = false;
        this.messageEdit.visible = false;
        this.chatModifyingList.acceptMessageBox(this.messageEdit);
    }

    @Override
    public void resize(MinecraftClient minecraft, int i, int j) {
        String string = this.messageEdit.getText();
        boolean active = this.messageEdit.active;
        this.init(minecraft, i, j);
        this.messageEdit.setText(string);
        this.messageEdit.active = active;
        this.messageEdit.visible = active;
    }

    @Override
    public void render(MatrixStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        this.chatModifyingList.render(poseStack, i, j, f);
        drawCenteredText(poseStack, this.textRenderer, this.title, this.width / 2, 16, 16777215);
        this.contextInfoLabel.drawCenterWithShadow(poseStack, this.width / 2, this.chatModifyingList.getFooterTop());
        this.messageEdit.render(poseStack, i, j, f);
        super.render(poseStack, i, j, f);
    }

    @Override
    public void tick() {
        this.messageEdit.tick();
        super.tick();
    }

    @Override
    public void close() {
        this.onSelected.accept(this.abuseReport);
        this.client.setScreen(this.lastScreen);
    }

    @Override
    public Text getNarratedTitle() {
        return ScreenTexts.joinSentences(super.getNarratedTitle(), CONTEXT_INFO);
    }

    @Environment(EnvType.CLIENT)
    public class ChatModifyingList extends AlwaysSelectedEntryListWidget<ChatModifyingList.Entry> implements Output {

        public TextFieldWidget messageEdit;

        public ChatModifyingList(MinecraftClient minecraft, int i) {
            super(minecraft, ContextModifyingScreen.this.width, ContextModifyingScreen.this.height, 40, ContextModifyingScreen.this.height - 40 - i, 16);
        }

        @Override
        public void acceptMessageBox(TextFieldWidget box) {
            this.messageEdit = box;
        }

        @Override
        public void acceptMessage(ReportChatMessage chatMessage) {
            this.addEntry(new MessageEntry(chatMessage));
        }

        @Override
        protected int getScrollbarPositionX() {
            return (this.width + this.getRowWidth()) / 2;
        }

        @Override
        public int getRowWidth() {
            return Math.min(350, this.width - 50);
        }

        @Override
        protected void renderEntry(MatrixStack poseStack, int i, int j, float f, int k, int l, int m, int n, int o) {
            ChatModifyingList.Entry entry = this.getEntry(k);
            if (this.shouldHighlightEntry(entry)) {
                boolean bl = this.getSelectedOrNull() == entry;
                int p = this.isFocused() && bl ? -1 : -8355712;
                this.drawSelectionHighlight(poseStack, m, n, o, p, -16777216);
            }
            entry.render(poseStack, k, m, l, n, o, i, j, this.getHoveredEntry() == entry, f);
        }

        private boolean shouldHighlightEntry(ChatModifyingList.Entry entry) {
            if (entry.canSelect()) {
                boolean bl = this.getSelectedOrNull() == entry;
                boolean bl2 = this.getSelectedOrNull() == null;
                boolean bl3 = this.getHoveredEntry() == entry;
                return bl || bl2 && bl3 && entry.shouldInclude();
            } else {
                return false;
            }
        }

        @Override
        protected void moveSelection(EntryListWidget.MoveDirection selectionDirection) {
            if (!this.moveSelectableSelection(selectionDirection) && selectionDirection == EntryListWidget.MoveDirection.UP)
                this.moveSelectableSelection(selectionDirection);
        }

        private boolean moveSelectableSelection(EntryListWidget.MoveDirection selectionDirection) {
            return this.moveSelectionIf(selectionDirection, ChatModifyingList.Entry::canSelect);
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (this.messageEdit.active) return this.messageEdit.mouseClicked(d, e, i);
            return super.mouseClicked(d, e, i);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            ChatModifyingList.Entry entry = this.getSelectedOrNull();
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
            return this.bottom + 9;
        }

        @Override
        protected boolean isFocused() {
            return ContextModifyingScreen.this.getFocused() == this;
        }

        @Environment(EnvType.CLIENT)
        public abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<ChatModifyingList.Entry> {
            @Override
            public Text getNarration() {
                return ScreenTexts.EMPTY;
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
            private static final Identifier CHECKMARK_TEXTURE = new Identifier("realms", "textures/gui/realms/checkmark.png");
            private static final int CHECKMARK_WIDTH = 9;
            private static final int CHECKMARK_HEIGHT = 8;
            private static final int INDENT_AMOUNT = 11;
            private StringVisitable text;
            private final String username;
            private final ReportChatMessage chatMessage;
            private boolean includeMessage;
            private boolean hasChanged;
            private final boolean wasReported;
            private final boolean canModify;

            public MessageEntry(ReportChatMessage chatMessage) {
                this.chatMessage = chatMessage;
                Text component = Text.literal(chatMessage.message);
                StringVisitable formattedText = ContextModifyingScreen.this.textRenderer.trimToWidth(component, this.getMaximumTextWidth() - ContextModifyingScreen.this.textRenderer.getWidth(ScreenTexts.ELLIPSIS));
                this.text = component != formattedText ? StringVisitable.concat(formattedText, ScreenTexts.ELLIPSIS) : component;
                this.includeMessage = true;
                this.hasChanged = false;
                this.wasReported = chatMessage.messageReported;
                this.canModify = !this.wasReported && chatMessage.profileId == MinecraftClient.getInstance().player.getUuid();
                if (MinecraftClient.getInstance().world != null) {
                    PlayerEntity player = MinecraftClient.getInstance().world.getPlayerByUuid(chatMessage.profileId);
                    if (player != null) {
                        this.username = player.getEntityName();
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
                    this.text = Text.literal(text);
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
            public void render(MatrixStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
                int p = k + this.getTextIndent();
                int q = j + 1 + (m - CHECKMARK_WIDTH) / 2;
                if (this.isSelected() && this.includeMessage || !this.canModify) {
                    this.renderCheckmark(poseStack, j, k, m);
                }
                if (this.wasReported) {
                    drawTextWithShadow(poseStack,ContextModifyingScreen.this.textRenderer, Text.literal(this.username).fillStyle(Style.EMPTY.withColor(Formatting.RED)), p, q, this.includeMessage ? -1 : -1593835521);
                } else if (this.canModify) {
                    drawStringWithShadow(poseStack,ContextModifyingScreen.this.textRenderer, this.username, p, q, this.includeMessage ? -1 : -1593835521);
                } else {
                    drawTextWithShadow(poseStack,ContextModifyingScreen.this.textRenderer, Text.literal(this.username).fillStyle(Style.EMPTY.withColor(Formatting.GRAY)), p, q, this.includeMessage ? -1 : -1593835521);
                }
                drawStringWithShadow(poseStack, ContextModifyingScreen.this.textRenderer, this.text.getString(), p + 90, q, this.includeMessage ? -1 : -1593835521);
            }

            private void renderCheckmark(MatrixStack poseStack, int i, int j, int k) {
                int m = i + (k - CHECKMARK_HEIGHT) / 2;
                RenderSystem.setShaderTexture(0, CHECKMARK_TEXTURE);
                RenderSystem.enableBlend();
                DrawableHelper.drawTexture(poseStack, j, m, 0.0F, 0.0F, CHECKMARK_WIDTH, CHECKMARK_HEIGHT, CHECKMARK_WIDTH, CHECKMARK_HEIGHT);
                RenderSystem.disableBlend();
            }

            private int getMaximumTextWidth() {
                return ChatModifyingList.this.getRowWidth() - this.getTextIndent();
            }

            private int getTextIndent() {
                return INDENT_AMOUNT;
            }

            private MessageSignature signMessage(ChatMessageSigner messageSigner, Text component) {
                try {
                    Signer signer = MinecraftClient.getInstance().getProfileKeys().getSigner();
                    if (signer != null) {
                        return messageSigner.sign(signer, component);
                    }
                } catch (Exception var4) {
                    FabricatedChatReports.LOGGER.error("Failed to sign chat message: '{}'", component.getString(), var4);
                }

                return MessageSignature.none();
            }

            private void reSignMessage() {
                ChatMessageSigner messageSigner = new ChatMessageSigner(this.chatMessage.profileId, this.chatMessage.timestamp, this.chatMessage.salt);
                MessageSignature fabricatedSignature = signMessage(messageSigner, Text.literal(this.text.getString()));
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
                        ChatModifyingList.this.messageEdit.setChangedListener(string -> {
                            this.modifyText(string);
                            // Every time you press a key im going to re-sign the entire message cause I don't give a damn
                            // This is just to show a proof of concept so ive really been slacking on this project
                            if (this.hasChanged) this.reSignMessage();
                        });
                        ChatModifyingList.this.messageEdit.setText(this.text.getString());
                        ChatModifyingList.this.messageEdit.active = true;
                        ChatModifyingList.this.messageEdit.visible = true;
                        ChatModifyingList.this.messageEdit.setTextFieldFocused(true);
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

        void acceptMessageBox(TextFieldWidget box);
    }
}