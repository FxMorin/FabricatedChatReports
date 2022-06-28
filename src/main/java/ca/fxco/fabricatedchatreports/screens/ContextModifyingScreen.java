package ca.fxco.fabricatedchatreports.screens;

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
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
    private final ReportingContext reportingContext;
    private Button confirmSelectedButton;
    private MultiLineLabel contextInfoLabel;
    @Nullable
    private ContextModifyingScreen.ChatModifyingList chatModifyingList;
    final FabricatedAbuseReport abuseReport;
    private final Consumer<FabricatedAbuseReport> onSelected;
    @Nullable
    private List<FormattedCharSequence> tooltip;

    public ContextModifyingScreen(
            @Nullable Screen screen, ReportingContext reportingContext, FabricatedAbuseReport abuseReport, Consumer<FabricatedAbuseReport> consumer
    ) {
        super(Component.literal("Select Chat Messages to Not Include"));
        this.lastScreen = screen;
        this.reportingContext = reportingContext;
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
                        if (entry.hasChanged()) chatMessage.message = entry.getText();
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
    protected void init() {
        this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
        this.chatModifyingList = new ChatModifyingList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9);
        this.chatModifyingList.setRenderBackground(false);
        chatModifyingList.acceptMessages(this.abuseReport.evidence.messages);
        this.addWidget(this.chatModifyingList);
        this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 32, 150, 20, CommonComponents.GUI_BACK, button -> this.onClose()));
        this.confirmSelectedButton = this.addRenderableWidget(
                new Button(this.width / 2 - 155 + 160, this.height - 32, 150, 20, CommonComponents.GUI_DONE, button -> {
                    modifyAbuseReport(this.abuseReport);
                    this.onSelected.accept(this.abuseReport);
                    this.onClose();
                })
        );
        this.chatModifyingList.setScrollAmount(this.chatModifyingList.getMaxScroll());
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        this.chatModifyingList.render(poseStack, i, j, f);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 16, 16777215);
        this.contextInfoLabel.renderCentered(poseStack, this.width / 2, this.chatModifyingList.getFooterTop());
        super.render(poseStack, i, j, f);
        if (this.tooltip != null) {
            this.renderTooltip(poseStack, this.tooltip, i, j);
            this.tooltip = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
    }

    void setTooltip(@Nullable List<FormattedCharSequence> list) {
        this.tooltip = list;
    }

    @Environment(EnvType.CLIENT)
    public class ChatModifyingList extends ObjectSelectionList<ChatModifyingList.Entry> implements Output {

        public ChatModifyingList(Minecraft minecraft, int i) {
            super(minecraft, ContextModifyingScreen.this.width, ContextModifyingScreen.this.height, 40, ContextModifyingScreen.this.height - 40 - i, 16);
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
        public boolean keyPressed(int i, int j, int k) {
            ChatModifyingList.Entry entry = this.getSelected();
            if (entry != null && entry.keyPressed(i, j, k)) {
                return true;
            } else {
                this.setFocused(null);
                return super.keyPressed(i, j, k);
            }
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
            @Nullable
            private final List<FormattedCharSequence> hoverText;
            private final ReportChatMessage chatMessage;
            private boolean includeMessage;
            private boolean hasChanged;
            private final boolean canModify;

            public MessageEntry(ReportChatMessage chatMessage) {
                this.chatMessage = chatMessage;
                Component component = Component.literal(chatMessage.message);
                FormattedText formattedText = ContextModifyingScreen.this.font.substrByWidth(component, this.getMaximumTextWidth() - ContextModifyingScreen.this.font.width(CommonComponents.ELLIPSIS));
                if (component != formattedText) {
                    this.text = FormattedText.composite(formattedText, CommonComponents.ELLIPSIS);
                    this.hoverText = ContextModifyingScreen.this.font.split(component, ChatModifyingList.this.getRowWidth());
                } else {
                    this.text = component;
                    this.hoverText = null;
                }
                this.includeMessage = true;
                this.hasChanged = false;
                this.canModify = !chatMessage.messageReported;
                if (Minecraft.getInstance().level != null) {
                    Player player = Minecraft.getInstance().level.getPlayerByUUID(chatMessage.profileId);
                    if (player != null) {
                        this.username = player.getName().getString();
                    } else {
                        this.username = chatMessage.profileId.toString();
                    }
                } else {
                    this.username = chatMessage.profileId.toString();
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
                if (this.canModify) {
                    drawString(poseStack,ContextModifyingScreen.this.font, this.username, p, q, this.includeMessage ? -1 : -1593835521);
                } else {
                    drawString(poseStack,ContextModifyingScreen.this.font, Component.literal(this.username).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), p, q, this.includeMessage ? -1 : -1593835521);
                }
                drawString(poseStack, ContextModifyingScreen.this.font, this.text.getString(), p + 70, q, this.includeMessage ? -1 : -1593835521);
                if (this.hoverText != null && bl) {
                    ContextModifyingScreen.this.setTooltip(this.hoverText);
                }
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

            @Override
            public boolean mouseClicked(double d, double e, int i) {
                if (i == 0) {
                    if (this.canModify) {
                        ChatModifyingList.this.setSelected(null);
                        return this.toggleInclude();
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            @Override
            public boolean keyPressed(int i, int j, int k) {
                return i != 257 && i != 32 && i != 335 ? false : this.toggleInclude(); //TODO Change
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
    }
}